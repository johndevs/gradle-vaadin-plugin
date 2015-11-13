/*
* Copyright 2015 John Ahlroos
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
import fi.jasoft.plugin.configuration.VaadinPluginExtension
import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

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
                    addons: Util.findAddonsInProject(project),
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
                    addons: Util.findAddonsInProject(project),
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

        project.afterEvaluate {

            /* Monitor changes in dependencies since upgrading a
            * dependency should also trigger a recompile of the widgetset
            */
            inputs.files(project.configurations.compile)

            // Monitor changes in client side classes and resources
            project.sourceSets.main.java.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
            }

            //Monitor changes in resources
            project.sourceSets.main.resources.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
            }

            // Add classpath jar
            if(project.vaadin.plugin.useClassPathJar) {
                BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
                inputs.file(pathJarTask.archivePath)
            }

            // Widgetset output directory
            outputs.dir(Util.getWidgetsetDirectory(project))

            // Unit cache output directory
            outputs.dir(Util.getWidgetsetCacheDirectory(project))
        }
    }

    @TaskAction
    public void run() {
        def vaadin = project.vaadin as VaadinPluginExtension
        if(vaadin.widgetset){
            if(vaadin.widgetsetCDN){
                compileRemotely()
            } else {
                compileLocally()
            }
        }
    }

    private void compileRemotely() {
        def vaadin = project.vaadin as VaadinPluginExtension
        if(vaadin.widgetset ==~ /[A-Za-z0-9]+/){

            // Ensure widgetset directory exists
            Util.getWidgetsetDirectory(project).mkdirs()

            def timeout = 60000 * 3 // 3 minutes

            while(true){
                def widgetsetInfo = queryRemoteWidgetset()
                logger.info(widgetsetInfo.toString())
                def status = widgetsetInfo.status as String
                switch(status){
                    case 'NOT_FOUND':
                    case 'UNKNOWN':
                    case 'ERROR':
                        throw new GradleException("Failed to compile widgetset with CDN with the status $status")
                    case 'QUEUED':
                    case 'COMPILING':
                    case 'COMPILED':
                        logger.info("Widgetset is compiling with status $status. Waiting 10 seconds and querying again.")
                        if(timeout > 0){
                            sleep(10000)
                            timeout -= 10000
                        } else {
                            throw new GradleException('Waiting for widgetset to compile timed out. Please try again at a later time.')
                        }
                        break
                    case 'AVAILABLE':
                        logger.info('Widgetset is available, downloading...')
                        downloadWidgetset()
                        return
                }
            }
        } else {
            throw new GradleException('Widgetset name can only contain alphanumeric characters (A-Z,a-z,0-9) when using CDN.')
        }
    }

    private void compileLocally() {
        def vaadin = project.vaadin as VaadinPluginExtension
        def gwt = vaadin.gwt

        // Ensure widgetset directory exists
        Util.getWidgetsetDirectory(project).mkdirs()

        FileCollection classpath =Util.getCompileClassPathOrJar(project)
        if(vaadin.plugin.useClassPathJar){
            // Add client dependencies missing from the classpath jar
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

                    if(!jar.manifest) {
                        return false
                    }

                    Attributes attributes = jar.manifest.mainAttributes
                    return attributes.getValue('Vaadin-Widgetsets')
                }
                true
            }

            // Ensure gwt sdk libs are in the correct order
            if(project.vaadin.gwt.gwtSdkFirstInClasspath){
                classpath = Util.moveGwtSdkFirstInClasspath(project, classpath)
            }
        }

        def widgetsetCompileProcess = ['java']

        if (gwt.jvmArgs) {
            widgetsetCompileProcess += gwt.jvmArgs as List
        }

        widgetsetCompileProcess += ['-cp',  classpath.asPath]

        widgetsetCompileProcess += 'com.google.gwt.dev.Compiler'

        widgetsetCompileProcess += ['-style', gwt.style]
        widgetsetCompileProcess += ['-optimize', gwt.optimize]
        widgetsetCompileProcess += ['-war', Util.getWidgetsetDirectory(project).canonicalPath]
        widgetsetCompileProcess += ['-logLevel', gwt.logLevel]
        widgetsetCompileProcess += ['-localWorkers', gwt.localWorkers]

        if (gwt.draftCompile) {
            widgetsetCompileProcess += '-draftCompile'
        }

        if (gwt.strict) {
            widgetsetCompileProcess += '-strict'
        }

        if (gwt.extraArgs) {
            widgetsetCompileProcess += gwt.extraArgs as List
        }

        widgetsetCompileProcess += vaadin.widgetset

        def Process process = widgetsetCompileProcess.execute()
        def failed = false
        Util.logProcess(project, process, 'widgetset-compile.log', { String output ->
            // Monitor log for errors
            if(output.trim().startsWith('[ERROR]')){
                failed = true
            }
        })

        // Block
        process.waitFor()

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
        new File(Util.getWidgetsetDirectory(project), 'WEB-INF').deleteDir()

        if(failed) {
            // Terminate build
            throw new GradleException('Widgetset failed to compile. See error log above.')
        }
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

        def request = queryWidgetsetRequest(Util.getResolvedVaadinVersion(project), project.vaadin.gwt.style)
        logger.info(request.toString())

        def response = client.post(request)
        logger.info(response.toString())

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
