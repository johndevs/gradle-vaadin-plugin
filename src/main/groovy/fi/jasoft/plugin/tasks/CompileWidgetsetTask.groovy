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

    private def queryWidgetsetRequest = { version, style ->
        [
            path: '/api/compiler/compile',
            query: [
                    'compile.async' : false
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

    public CompileWidgetsetTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME, BuildClassPathJar.NAME)
        description = "Compiles Vaadin Addons and components into Javascript."
    }

    @TaskAction
    public void run() {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File targetDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        targetDir.mkdirs()

        if (project.vaadin.widgetset == null) {
            if(project.vaadin.widgetsetCDN) {
                /*
               Query for a widgetset
                */
                while(true){
                    def info = queryRemoteWidgetset()
                    println info

                    def status = info.status as String
                    def name = info.widgetSetName as String
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
                            downloadWidgetset(new File(targetDir, name))
                            return
                    }
                }
            }

            // Use default widgetset
            return
        }

        // Ensure unit cache dir is present so the compiler does not complain
        new File(webAppDir.canonicalPath + '/VAADIN/gwt-unitCache').mkdirs()

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
        widgetsetCompileProcess += ['-war', targetDir.canonicalPath]
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

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
        new File(targetDir.canonicalPath + "/WEB-INF").deleteDir()
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
    private ZipInputStream downloadWidgetset(File targetDir) {
        logger.info("Saving widgetset to "+targetDir.absolutePath)

        def client = new RESTClient(WIDGETSET_CDN_URL)
        client.headers['User-Agent'] = 'VWSCDN-1.3.1-SNAPSHOT ( / / ; / ; )'
        client.headers['Accept'] = 'application/x-zip'
        client.parser.'application/x-zip' = { response ->
            new ZipInputStream(response.entity.content)
        }

        def response = client.post(downloadWidgetsetRequest(
            Util.getResolvedVaadinVersion(project),
            project.vaadin.gwt.style
        ))

        def writeToFilestem = { ZipInputStream zipInputStream ->
            def ze
            while ((ze = zipInputStream.nextEntry) != null) {
                final File outfile = new File(targetDir.absolutePath, ze.getName());
                outfile.getParentFile().mkdirs();
                outfile.createNewFile();

                FileOutputStream fout = new FileOutputStream(outfile);
                for (int c = zipInputStream.read(); c != -1; c = zipInputStream.
                        read()) {
                    fout.write(c);
                }
                zipInputStream.closeEntry();
                fout.close();
            }
            zipInputStream.close();
        }
        writeToFilestem(new ZipInputStream(response.data))
    }
}
