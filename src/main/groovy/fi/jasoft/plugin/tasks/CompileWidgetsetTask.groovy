/*
* Copyright 2014 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.Util
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipInputStream

class CompileWidgetsetTask extends DefaultTask {

    static final NAME = 'vaadinCompileWidgetset'

    static final WIDGETSET_CDN_URL = 'http://cdn.virit.in'

    /**
     * HTTP POST request sent to CDN for requesting a widgetset.
     */
    private def queryWidgetsetRequest = { version, style ->
        [
            path: '/api/compiler/compile',
            query: [
                    'compile.async' : true
            ],
            body: [
                    vaadinVersion: version,
                    eager: [],
                    addons: [],
                    compileStyle: style
            ],
            requestContentType : ContentType.JSON
        ]
    }

    /**
     * HTTP POST request sent to CDN for downloading a widgetset.
     */
    private def downloadWidgetsetRequest = { version, style ->
        [
            path: '/api/compiler/download',
            body: [
                    vaadinVersion: version,
                    eager: [],
                    addons: [],
                    compileStyle: style
            ],
            requestContentType : ContentType.JSON
        ]
    }

    /**
     * Called by the downloadWidgetsetRequest once the response with the zipped contents arrive
     */
    private def writeWidgetsetToFileSystem = { request, zipStream ->
        def widgetsetName = project.vaadin.widgetset.replaceAll("[^a-zA-Z0-9]+","")

        if(widgetsetName != project.vaadin.widgetset){
            logger.warn("Widgetset name cannot contain special characters when using CDN. Illegal characters removed, please update your @Widgetset annotation or web.xml accordingly.")
        }

        def widgetsetDirectory = new File(Util.getWidgetsetDirectory(project).absolutePath, widgetsetName)
        widgetsetDirectory.mkdirs()

        def generatedWidgetSetName = request.headers.wsId as String
        def ze
        while ((ze = zipStream.nextEntry) != null) {
            def fileName = ze.name as String

            // Replace the generated widgetset filename with the real one
            final File outfile = new File(widgetsetDirectory.absolutePath,
                    fileName.replace(generatedWidgetSetName, widgetsetName));

            // Create directories and file
            outfile.parentFile.mkdirs();
            outfile.createNewFile();

            // Create file byte by byte
            FileOutputStream fout = new FileOutputStream(outfile);
            for (int c = zipStream.read(); c != -1; c = zipStream.read()) {
                fout.write(c);
            }
            zipStream.closeEntry();
            fout.close();

            // Replace all mentions of generated widgetset name with real one inside the files as well
            def contents = outfile.text
            outfile.text = contents.replaceAll(generatedWidgetSetName, widgetsetName)
        }
        zipStream.close();
    }

    public CompileWidgetsetTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME, BuildClassPathJar.NAME)
        description = "Compiles Vaadin Addons and components into Javascript."
    }

    private File

    @TaskAction
    public void run() {

        // Ensure widgetset directory exists
        Util.getWidgetsetDirectory(project).mkdirs()

        if (project.vaadin.widgetset == null) {
            // Use default widgetset
            return
        } else if(project.vaadin.widgetsetCDN) {
            // Use widgetset CDN to retrieve widgetset

            while(true){
                def info = queryRemoteWidgetset()
                def status = info.status as String
                switch(status){
                    case 'NOT_FOUND':
                    case 'UNKNOWN':
                    case 'ERROR':
                        logger.error(info)
                        didWork = false
                        return
                    case 'QUEUED':
                    case 'COMPILING':
                    case 'COMPILED':
                        logger.info("Widgetset is compiling with status $status. Waiting 10 seconds and quering again.")
                        sleep(10000)
                        break;
                    case 'AVAILABLE':
                        logger.info('Widgetset is available, downloading...')
                        downloadWidgetset()
                        return
                }
            }
            return
        }

        // Compile widgetset locally

        FileCollection classpath
        if(project.vaadin.plugin.useClassPathJar){
            // Add dependencies using the classpath jar
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            classpath = project.files(pathJarTask.archivePath)

            classpath += Util.getClientCompilerClassPath(project).filter { File file ->
                if(file.name.endsWith('.jar')){
                    // Add GWT compiler + deps
                    if(file.name.startsWith('vaadin-client') ||
                            file.name.startsWith('vaadin-shared') ||
                            file.name.startsWith('validation-api')){
                        return true
                    }

                    // Addons with client side widgetset
                    JarFile jar = new JarFile(file.absolutePath)
                    Attributes attributes = jar.manifest.mainAttributes
                    return attributes.getValue('Vaadin-Widgetsets')
                }
                true
            }
        } else {
            classpath = Util.getClientCompilerClassPath(project)
        }

        def widgetsetCompileProcess = ['java']

        if (project.vaadin.gwt.jvmArgs) {
            widgetsetCompileProcess += project.vaadin.gwt.jvmArgs
        }

        widgetsetCompileProcess += ['-cp',  classpath.getAsPath()]

        widgetsetCompileProcess += 'com.google.gwt.dev.Compiler'

        widgetsetCompileProcess += ['-style', project.vaadin.gwt.style]
        widgetsetCompileProcess += ['-optimize', project.vaadin.gwt.optimize]
        widgetsetCompileProcess += ['-war', Util.getWidgetsetDirectory(project).canonicalPath]
        widgetsetCompileProcess += ['-logLevel', project.vaadin.gwt.logLevel]
        widgetsetCompileProcess += ['-localWorkers', project.vaadin.gwt.localWorkers]

        if (project.vaadin.gwt.draftCompile) {
            widgetsetCompileProcess += '-draftCompile'
        }

        if (project.vaadin.gwt.strict) {
            widgetsetCompileProcess += '-strict'
        }

        if (project.vaadin.gwt.extraArgs) {
            widgetsetCompileProcess += project.vaadin.gwt.extraArgs as List
        }

        widgetsetCompileProcess += project.vaadin.widgetset

        def Process process = widgetsetCompileProcess.execute()

        Util.logProcess(project, process, 'widgetset-compile.log')

        process.waitFor()
    }

    /**
     * Queries the CDN fro a widgetset
     *
     * @return
     *      Returns the status json
     */
    private queryRemoteWidgetset(){
        logger.info("Querying widgetset for Vaadin "+Util.getResolvedVaadinVersion(project))
        def client = new RESTClient(WIDGETSET_CDN_URL)
        def response = client.post(queryWidgetsetRequest(
                Util.getResolvedVaadinVersion(project),
                project.vaadin.gwt.style
        ))
        response.data
    }

    /**
     * Downloads the widgetset from the CDN
     *
     * @return
     *      Returns a stream with the widgetset files
     */
    private ZipInputStream downloadWidgetset() {
        def client = new RESTClient(WIDGETSET_CDN_URL)
        client.headers['User-Agent'] = 'VWSCDN-1.3.0 ( / / ; / ; )'
        client.headers['Accept'] = 'application/x-zip'
        client.parser.'application/x-zip' = { response ->
            new ZipInputStream(response.entity.content)
        }

        client.post(downloadWidgetsetRequest(
            Util.getResolvedVaadinVersion(project),
            project.vaadin.gwt.style
        ), writeWidgetsetToFileSystem)
    }
}
