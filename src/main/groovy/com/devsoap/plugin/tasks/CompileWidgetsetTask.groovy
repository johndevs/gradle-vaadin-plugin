/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util

import com.devsoap.plugin.extensions.WidgetsetCDNExtension
import groovy.transform.PackageScope
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.RESTClient
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.TimeUnit
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Compiles the Vaadin Widgetsets
 *
 * @author John Ahlroos
 */
@CacheableTask
class CompileWidgetsetTask extends DefaultTask {

    static final NAME = 'vaadinCompile'

    static final WIDGETSET_CDN_URL = 'https://wsc.vaadin.com/'
    static final String PUBLIC_FOLDER_PATTERN = '**/*/public/**/*.*'
    static final String GWT_MODULE_XML_PATTERN = '**/*/*.gwt.xml'

    final PropertyState<String> style = project.property(String)
    final PropertyState<Integer> optimize = project.property(Integer)
    final PropertyState<Boolean> logEnabled = project.property(Boolean)
    final PropertyState<String> logLevel = project.property(String)
    final PropertyState<Integer> localWorkers = project.property(Integer)
    final PropertyState<Boolean> draftCompile = project.property(Boolean)
    final PropertyState<Boolean> strict = project.property(Boolean)
    final PropertyState<String> userAgent = project.property(String)
    final PropertyState<List<String>> jvmArgs = project.property(List)
    final PropertyState<List<String>> extraArgs = project.property(List)
    final PropertyState<List<String>> sourcePaths = project.property(List)
    final PropertyState<Boolean> collapsePermutations = project.property(Boolean)
    final PropertyState<List<String>> extraInherits = project.property(List)
    final PropertyState<Boolean> gwtSdkFirstInClasspath = project.property(Boolean)
    final PropertyState<String> outputDirectory = project.property(String)
    final PropertyState<Boolean> widgetsetCDN = project.property(Boolean)
    final PropertyState<Boolean> profiler = project.property(Boolean)
    final PropertyState<Boolean> manageWidgetset = project.property(Boolean)
    final PropertyState<String> widgetset = project.property(String)
    final PropertyState<String> widgetsetGenerator = project.property(String)

    final WidgetsetCDNExtension widgetsetCDNConfig =  extensions.create(WidgetsetCDNExtension.NAME,
            WidgetsetCDNExtension, project)

    @PackageScope
    def queryWidgetsetRequest = { version, style ->
        Set addons = Util.findAddonsInProject(project)
        project.logger.info("Querying widgetset with addons $addons")

        [
            path: '/api/compiler/compile',
            query: [
                    'compile.async' : true,
            ],
            body: [
                    vaadinVersion:version,
                    eager: [],
                    addons:addons,
                    compileStyle:style
            ],
            requestContentType:ContentType.JSON
        ]
    }

    /**
     * HTTP POST request sent to CDN for downloading a widgetset.
     */
    @PackageScope
    def downloadWidgetsetRequest = { version, style ->
        [
            path: '/api/compiler/download',
            body: [
                    vaadinVersion:version,
                    eager: [],
                    addons:Util.findAddonsInProject(project),
                    compileStyle:style
            ],
            requestContentType:ContentType.JSON
        ]
    }

    /**
     * Called by the downloadWidgetsetRequest once the response with the zipped contents arrive
     */
    @PackageScope
    def writeWidgetsetToFileSystem = { HttpResponseDecorator response, ZipInputStream zipStream ->

        // Check for redirect
        if ( response.status == 307 ) {
            String newUrl = response.headers['Location'].value
            project.logger.info("Widgetset download was redirected to $newUrl")
            downloadWidgetsetZip(newUrl)
            return
        }

        def generatedWidgetSetName = response.headers['x-amz-meta-wsid']?.value
        if ( !generatedWidgetSetName ) {
            generatedWidgetSetName = response.headers['wsId'].value
        }

        def widgetsetDirectory = new File(Util.getWidgetsetDirectory(project), generatedWidgetSetName)
        if ( widgetsetDirectory.exists() ) {
            widgetsetDirectory.deleteDir()
        }
        widgetsetDirectory.mkdirs()

        Objects.requireNonNull(zipStream, "Response stream cannot be null")

        project.logger.info("Extracting widgetset $generatedWidgetSetName into $widgetsetDirectory")

        ZipEntry ze
        while ((ze = zipStream.nextEntry) != null) {
            def fileName = ze.name as String
            File outfile = new File(widgetsetDirectory, fileName)

            if ( ze.directory ) {
                outfile.mkdirs()
                continue
            }

            outfile.parentFile.mkdirs()
            outfile.createNewFile()

            // Create file byte by byte
            FileOutputStream fout = new FileOutputStream(outfile)
            for ( int c = zipStream.read(); c != -1; c = zipStream.read() ) {
                fout.write(c)
            }
            zipStream.closeEntry()
            fout.close()
        }
        zipStream.close()

        project.logger.info("Generating AppWidgetset")

        def substitutions = [:]
        substitutions['widgetsetName'] = generatedWidgetSetName
        File sourceDir = Util.getMainSourceSet(project).srcDirs.first()

        String widgetsetName = 'AppWidgetset'
        switch (Util.getProjectType(project)) {
            case ProjectType.JAVA:
                TemplateUtil.writeTemplate("${widgetsetName}.java", sourceDir,
                        "${widgetsetName}.java",  substitutions)
                break
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate("${widgetsetName}.groovy", sourceDir,
                        "${widgetsetName}.groovy",  substitutions)
                break
            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate("${widgetsetName}.kt", sourceDir,
                        "${widgetsetName}.kt",  substitutions)
                break

        }
    }

    CompileWidgetsetTask() {
        description = "Compiles Vaadin Addons and components into Javascript."

        style.set('OBF')
        optimize.set(0)
        logEnabled.set(true)
        logLevel.set('INFO')
        localWorkers.set(Runtime.getRuntime().availableProcessors())
        draftCompile.set(true)
        strict.set(true)
        userAgent.set(null)
        jvmArgs.set(null)
        extraArgs.set(null)
        sourcePaths.set(['client', 'shared'])
        collapsePermutations.set(true)
        extraInherits.set(null)
        gwtSdkFirstInClasspath.set(true)
        outputDirectory.set(null)
        widgetsetCDN.set(false)
        profiler.set(false)
        manageWidgetset.set(true)
        widgetset.set(null)
        widgetsetGenerator.set(null)

        project.afterEvaluate {

            // Set task dependencies

            if ( getWidgetsetCDN() ) {
                dependsOn 'processResources'
            } else {
                dependsOn('classes', UpdateWidgetsetTask.NAME, BuildClassPathJar.NAME)
            }

            /* Monitor changes in dependencies since upgrading a
            * dependency should also trigger a recompile of the widgetset
            */
            inputs.files(project.configurations.compile)
            inputs.files(project.configurations[GradleVaadinPlugin.CONFIGURATION_CLIENT])

            // Monitor changes in client side classes and resources
            project.sourceSets.main.java.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include(PUBLIC_FOLDER_PATTERN))
                inputs.files(project.fileTree(it.absolutePath).include(GWT_MODULE_XML_PATTERN))
            }

            //Monitor changes in resources
            project.sourceSets.main.resources.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include(PUBLIC_FOLDER_PATTERN))
                inputs.files(project.fileTree(it.absolutePath).include(GWT_MODULE_XML_PATTERN))
            }

            // Add classpath jar
            if ( project.vaadin.useClassPathJar ) {
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
    def run() {
        if ( getWidgetsetCDN() ) {
            compileRemotely()
            return
        }

        String widgetset = Util.getWidgetset(project)
        if ( widgetset ) {
            compileLocally(widgetset)
        }
    }

    /**
     * Compiles the widgetset on the remote CDN
     */
    @PackageScope compileRemotely() {

        // Ensure widgetset directory exists
        Util.getWidgetsetDirectory(project).mkdirs()

        def timeout = TimeUnit.MINUTES.toMillis(5)

        while(true) {
            def widgetsetInfo = queryRemoteWidgetset()
            def status = widgetsetInfo.status as String
            switch(status) {
                case 'NOT_FOUND':
                case 'UNKNOWN':
                case 'ERROR':
                    throw new GradleException("Failed to compile widgetset with CDN with the status $status")
                case 'QUEUED':
                case 'COMPILING':
                case 'COMPILED':
                    logger.info("Widgetset is compiling with status $status. " +
                            "Waiting 10 seconds and querying again.")
                    int timeoutInterval = TimeUnit.SECONDS.toMillis(10)
                    if ( timeout > 0 ) {
                        sleep(timeoutInterval)
                        timeout -= timeoutInterval
                    } else {
                        throw new GradleException('Waiting for widgetset to compile timed out. ' +
                                'Please try again at a later time.')
                    }
                    break
                case 'AVAILABLE':
                    logger.info('Widgetset is available, downloading...')
                    downloadWidgetset()
                    return
            }
        }
    }

    /**
     * Compiles the widgetset locally
     */
    @PackageScope compileLocally(String widgetset = Util.getWidgetset(project)) {

        // Re-create directory
        Util.getWidgetsetDirectory(project).mkdirs()

        FileCollection classpath = Util.getCompileClassPathOrJar(project)

        // Add client dependencies missing from the classpath jar
        classpath += Util.getClientCompilerClassPath(project).filter { File file ->
            if ( file.name.endsWith('.jar') ) {
                // Add GWT compiler + deps
                if ( file.name.startsWith('vaadin-client' ) ||
                        file.name.startsWith('vaadin-shared') ||
                        file.name.startsWith('vaadin-compatibility-client') ||
                        file.name.startsWith('vaadin-compatibility-shared') ||
                        file.name.startsWith('validation-api')) {
                    return true
                }

                // Addons with client side widgetset
                JarFile jar = new JarFile(file.absolutePath)

                if ( !jar.manifest ) {
                    return false
                }

                Attributes attributes = jar.manifest.mainAttributes
                return attributes.getValue('Vaadin-Widgetsets')
            }
            true
        }

        // Ensure gwt sdk libs are in the correct order
        if ( getGwtSdkFirstInClasspath() ) {
            classpath = Util.moveGwtSdkFirstInClasspath(project, classpath)
        }

        def widgetsetCompileProcess = [Util.getJavaBinary(project)]

        if ( getJvmArgs() ) {
            widgetsetCompileProcess += getJvmArgs() as List
        }

        widgetsetCompileProcess += ['-cp',  classpath.asPath]

        widgetsetCompileProcess += ["-Dgwt.persistentunitcachedir=${project.buildDir.canonicalPath}"]

        widgetsetCompileProcess += 'com.google.gwt.dev.Compiler'

        widgetsetCompileProcess += ['-style', getStyle()]
        widgetsetCompileProcess += ['-optimize', getOptimize()]
        widgetsetCompileProcess += ['-war', Util.getWidgetsetDirectory(project).canonicalPath]
        widgetsetCompileProcess += ['-logLevel', getLogLevel()]
        widgetsetCompileProcess += ['-localWorkers', getLocalWorkers()]
        widgetsetCompileProcess += ['-workDir', project.buildDir.canonicalPath + File.separator + 'tmp']

        if ( getDraftCompile() ) {
            widgetsetCompileProcess += '-draftCompile'
        }

        if ( getStrict() ) {
            widgetsetCompileProcess += '-strict'
        }

        if ( getExtraArgs() ) {
            widgetsetCompileProcess += getExtraArgs() as List
        }

        widgetsetCompileProcess += widgetset

        def Process process = widgetsetCompileProcess.execute([], project.buildDir)
        def failed = false
        Util.logProcess(project, process, 'widgetset-compile.log') { String output ->
            // Monitor log for errors
            if ( output.trim().startsWith('[ERROR]') ) {
                failed = true
                return false
            }
            true
        }

        // Block
        def result = process.waitFor()

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
        new File(Util.getWidgetsetDirectory(project), 'WEB-INF').deleteDir()

        if ( failed || result != 0 ) {
            // Terminate build
            throw new GradleException('Widgetset failed to compile. See error log.')
        }
    }

    /**
     * Queries the CDN fro a widgetset
     *
     * @return
     *      Returns the status json
     */
    @PackageScope queryRemoteWidgetset() {
        logger.info("Querying widgetset for Vaadin "+Util.getResolvedVaadinVersion(project))
        def client = new RESTClient(WIDGETSET_CDN_URL)
        configureClient(client)

        def request = queryWidgetsetRequest(
                Util.getResolvedVaadinVersion(project),
                getStyle()
        )
        def response = client.post(request)
        response.data
    }

    /**
     * Downloads the widgetset from the CDN
     *
     * @return
     *      Returns a stream with the widgetset files
     */
    @PackageScope ZipInputStream downloadWidgetset() {
        makeClient(WIDGETSET_CDN_URL).post(downloadWidgetsetRequest(
            Util.getResolvedVaadinVersion(project),
            getStyle()
        ), writeWidgetsetToFileSystem)
    }

    /**
     * Uses a GET request to download the zip
     * @param url
     *      the full URL of the zip archive
     * @return
     */
    @PackageScope ZipInputStream downloadWidgetsetZip(String url) {
        makeClient(url).get([:], writeWidgetsetToFileSystem)
    }

    /**
     * Creates a new Rest client
     * @param url
     *      the base url of the client
     * @return
     *      the client
     */
    @PackageScope RESTClient makeClient(String url) {
        RESTClient client = new RESTClient(url)
        client.headers['User-Agent'] = getUA()
        client.headers['Accept'] = 'application/x-zip'
        client.parser.'application/x-zip' = { response ->
            new ZipInputStream(response.entity.content)
        }
        client.parser.'application/zip' = { response ->
            new ZipInputStream(response.entity.content)
        }
        configureClient(client)
        client
    }

    @PackageScope configureClient(RESTClient client) {

        // Proxy support
        if(widgetsetCDNConfig.proxyEnabled) {
            client.ignoreSSLIssues()
            client.setProxy(
                    widgetsetCDNConfig.proxyHost,
                    widgetsetCDNConfig.proxyPort,
                    widgetsetCDNConfig.proxyScheme
            )
            if(widgetsetCDNConfig.proxyAuth) {
                client.setAuthConfig(widgetsetCDNConfig.proxyAuth)
            }
        }
    }

    @PackageScope static String getUA() {
        StringBuilder ua = new StringBuilder('VWSCDN-1.0.gradle (')
        ua.append(
                System.properties
                .subMap(['os.name', 'os.arch', 'os.version', 'java.runtime.name', 'java.version'])
                .values()
                .join('/')
        )
        ua.append(';')
        ua.append(DigestUtils.md5Hex(System.properties['user.name']))
        ua.toString()
    }

    /**
     * Compilation style
     */
    String getStyle() {
        style.get()
    }

    /**
     * Compilation style
     */
    void setStyle(String style) {
        this.style.set(style)
    }

    /**
     * Should the compilation result be optimized
     */
    Integer getOptimize() {
        optimize.get()
    }

    /**
     * Should the compilation result be optimized
     */
    void setOptimize(Integer optimized) {
        optimize.set(optimized)
    }

    /**
     * Should logging be enabled
     */
    Boolean getLogEnabled() {
        logEnabled.get()
    }

    /**
     * Should logging be enabled
     */
    void setLogEnabled(Boolean enabled) {
        logEnabled.set(enabled)
    }

    /**
     * The log level. Possible levels NONE,DEBUG,TRACE,INFO
     */
    String getLogLevel() {
        logLevel.get()
    }

    /**
     * The log level. Possible levels NONE,DEBUG,TRACE,INFO
     */
    void setLogLevel(String logLevel) {
        this.logLevel.set(logLevel)
    }

    /**
     * Amount of local workers used when compiling. By default the amount of processors.
     */
    Integer getLocalWorkers() {
        localWorkers.get()
    }

    /**
     * Amount of local workers used when compiling. By default the amount of processors.
     */
    void setLocalWorkers(Integer workers) {
        localWorkers.set(workers)
    }

    /**
     * Should draft compile be used
     */
    Boolean getDraftCompile() {
        draftCompile.get()
    }

    /**
     * Should draft compile be used
     */
    void setDraftCompile(Boolean draft) {
        draftCompile.set(draft)
    }

    /**
     * Should strict compiling be used
     */
    Boolean getStrict() {
        strict.get()
    }

    /**
     * Should strict compiling be used
     */
    void setStrict(Boolean strict) {
        this.strict.set(strict)
    }

    /**
     * What user agents (browsers should be used. By defining null all user agents are used.
     */
    String getUserAgent() {
        userAgent.getOrNull()
    }

    /**
     * What user agents (browsers should be used. By defining null all user agents are used.
     */
    void setUserAgent(String ua) {
        userAgent.set(ua)
    }

    /**
     * Extra jvm arguments passed the JVM running the compiler
     */
    String[] getJvmArgs() {
        jvmArgs.present ? jvmArgs.get().toArray(new String[jvmArgs.get().size()]) : null
    }

    /**
     * Extra jvm arguments passed the JVM running the compiler
     */
    void setJvmArgs(String... args) {
        jvmArgs.set(Arrays.asList(args))
    }

    /**
     * Extra arguments passed to the compiler
     */
    String[] getExtraArgs() {
        extraArgs.present ? extraArgs.get().toArray(new String[extraArgs.get().size()]) : null
    }

    /**
     * Extra arguments passed to the compiler
     */
    void setExtraArgs(String... args) {
        extraArgs.set(Arrays.asList(args))
    }

    /**
     * Source paths where the compiler will look for source files
     */
    String[] getSourcePaths() {
        sourcePaths.present ? sourcePaths.get().toArray(new String[sourcePaths.get().size()]): null
    }

    /**
     * Source paths where the compiler will look for source files
     */
    void setSourcePaths(String... paths) {
        sourcePaths.set(Arrays.asList(paths))
    }

    /**
     * Should the compiler permutations be collapsed to save time
     */
    Boolean getCollapsePermutations() {
        collapsePermutations.get()
    }

    /**
     * Should the compiler permutations be collapsed to save time
     */
    void setCollapsePermutations(Boolean collapse) {
        collapsePermutations.set(collapse)
    }

    /**
     * Extra module inherits
     */
    String[] getExtraInherits() {
        extraInherits.present ? extraInherits.get().toArray(new String[extraInherits.get().size()]) : null
    }

    /**
     * Extra module inherits
     */
    void setExtraInherits(String... inherits) {
        extraInherits.set(Arrays.asList(inherits))
    }

    /**
     * Should GWT be placed first in the classpath when compiling the widgetset.
     */
    Boolean getGwtSdkFirstInClasspath() {
        gwtSdkFirstInClasspath.get()
    }

    /**
     * Should GWT be placed first in the classpath when compiling the widgetset.
     */
    void setGwtSdkFirstInClasspath(Boolean first) {
        gwtSdkFirstInClasspath.set(first)
    }

    /**
     * (Optional) root directory, for generated files; default is the web-app dir from the WAR plugin
     */
    String getOutputDirectory() {
        outputDirectory.getOrNull()
    }

    /**
     * (Optional) root directory, for generated files; default is the web-app dir from the WAR plugin
     */
    void setOutputDirectory(String outputDir) {
        outputDirectory.set(outputDir)
    }

    /**
     * Use the widgetset CDN located at cdn.virit.in
     */
    Boolean getWidgetsetCDN() {
        widgetsetCDN.get()
    }

    /**
     * Use the widgetset CDN located at cdn.virit.in
     */
    void setWidgetsetCDN(Boolean cdn) {
        widgetsetCDN.set(cdn)
    }

    /**
     * Should the Vaadin client side profiler be used
     */
    Boolean getProfiler() {
        profiler.get()
    }

    /**
     * Should the Vaadin client side profiler be used
     */
    void setProfiler(Boolean enabled) {
        profiler.set(enabled)
    }

    /**
     * Should the plugin manage the widgetset (gwt.xml file)
     */
    Boolean getManageWidgetset() {
        manageWidgetset.get()
    }

    /**
     * Should the plugin manage the widgetset (gwt.xml file)
     */
    void setManageWidgetset(Boolean manage) {
        manageWidgetset.set(manage)
    }

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    String getWidgetset() {
        widgetset.getOrNull()
    }

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    void setWidgetset(String widgetset) {
        this.widgetset.set(widgetset)
    }

    /**
     * The widgetset generator to use
     */
    String getWidgetsetGenerator() {
        widgetsetGenerator.getOrNull()
    }

    /**
     * The widgetset generator to use
     */
    void setWidgetsetGenerator(String generator) {
        widgetsetGenerator.set(generator)
    }
}
