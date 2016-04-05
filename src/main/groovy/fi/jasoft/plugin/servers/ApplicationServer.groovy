/*
* Copyright 2016 John Ahlroos
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
package fi.jasoft.plugin.servers

import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import fi.jasoft.plugin.configuration.ApplicationServerConfiguration
import fi.jasoft.plugin.tasks.BuildClassPathJar
import fi.jasoft.plugin.tasks.CompileThemeTask
import groovy.transform.PackageScope
import org.apache.tools.ant.taskdefs.Pack
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Base class for application servers
 *
 * @author John Ahlroos
 */
abstract class ApplicationServer {

    private static final String JAR_FILE_POSTFIX = '.jar'
    private static final String AMPERSAND = '&'
    private static final String RELOADING_SERVER_MESSAGE = 'Reloading server...'

    /**
     * Creates a new application server
     *
     * @param project
     *      the project to create the server for
     * @param browserParameters
     *      possible browser GET parameters passed to the browser that opens the page after the server has loaded
     * @return
     *      returns the application server
     */
    static ApplicationServer get(Project project,
                                    List browserParameters = [],
                                    ApplicationServerConfiguration configuration=project.vaadinRun){
        switch(configuration.server){
            case PayaraApplicationServer.NAME:
                return new PayaraApplicationServer(project, browserParameters, configuration)
            case JettyApplicationServer.NAME:
                return new JettyApplicationServer(project, browserParameters, configuration)
            default:
                throw new IllegalArgumentException("Server name not recognized. Must be either payara or jetty.")
        }
    }

    def Process process;

    def boolean reloadInProgress = false

    def final Project project;

    def List browserParameters = []

    def ApplicationServerConfiguration configuration

    /**
     * Create a application server
     *
     * @param project
     *      the project to use
     * @param browserParameters
     *      the parameters passes to the browser
     * @param configuration
     *      the serverconfiguration
     */
    protected ApplicationServer(Project project, List browserParameters, ApplicationServerConfiguration configuration) {
        this.project = project
        this.browserParameters = browserParameters
        this.configuration = configuration
    }

    abstract String getServerRunner()

    abstract String getServerName()

    abstract String getSuccessfullyStartedLogToken()

    abstract defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies)

    def FileCollection getClassPath(){
        FileCollection cp
        if(project.vaadin.plugin.useClassPathJar){
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            cp = project.files(pathJarTask.archivePath)
        } else {
            cp = (project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER] + Util.getWarClasspath(project))
                    .filter { it.file && it.canonicalFile.name.endsWith(JAR_FILE_POSTFIX)}
        }
        cp
    }

    def makeClassPathFile(File buildDir) {
        def buildClasspath = new File(buildDir, 'classpath.txt')
        buildClasspath.text = Util.getWarClasspath(project)
                .filter { it.file && it.canonicalFile.name.endsWith(JAR_FILE_POSTFIX)}
                .join(";")
    }


    def boolean start(boolean firstStart=false, boolean stopAfterStart=false) {
        if (process) {
            project.logger.error('Server is already running.')
            return false
        }

        project.logger.info("Starting $serverName...")

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        def appServerProcess = [Util.getJavaBinary(project)]

        // Debug
        if (configuration.debug) {
            appServerProcess.add('-Xdebug')
            appServerProcess.add("-Xrunjdwp:transport=dt_socket,address=${configuration.debugPort},server=y,suspend=n")
        }

        // JVM options
        if (configuration.debug) {
            appServerProcess.add('-ea')
        }

        appServerProcess.add('-cp')
        appServerProcess.add(classPath.asPath)

        if (configuration.jvmArgs) {
            appServerProcess.addAll(configuration.jvmArgs)
        }

        // Program args
        appServerProcess.add(serverRunner)

        appServerProcess.add(configuration.serverPort.toString())

        appServerProcess.add(webAppDir.canonicalPath + File.separator)

        File classesDir = project.sourceSets.main.output.classesDir
        if(project.vaadinRun.classesDir){
            // Eclipse might output somewhere else
            classesDir = project.file(project.vaadinRun.classesDir)
        }

        appServerProcess.add(classesDir.canonicalPath + File.separator)

        if(project.logger.debugEnabled){
            appServerProcess.add('DEBUG')
        } else {
            appServerProcess.add('INFO')
        }

        appServerProcess.add(project.name);

        def buildDir = new File(project.buildDir, serverName)
        buildDir.mkdirs()
        appServerProcess.add(buildDir.absolutePath)

        makeClassPathFile(buildDir)

        executeServer(appServerProcess, firstStart)

        monitorLog(firstStart, stopAfterStart)
    }

    @PackageScope
    def executeServer(List appServerProcess, boolean firstStart=false) {
        project.logger.debug("Running server with the command: "+appServerProcess)
        process = appServerProcess.execute()

        // Watch for changes in classes
        if(configuration.serverRestart) {
            def self = this
            Thread.start 'Class Directory Watcher', {
                watchClassDirectoryForChanges(self)
            }
        }

        // Watch for changes in theme
        if(firstStart && configuration.themeAutoRecompile){
            def self = this
            Thread.start 'Theme Directory Watcher', {
                watchThemeDirectoryForChanges(self)
            }
        }
    }

    @PackageScope
    def monitorLog(boolean firstStart=false, boolean stopAfterStart=false){
        Util.logProcess(project, process, "${serverName}.log", { line ->
            if(line.contains(successfullyStartedLogToken)) {
                if(firstStart) {
                    def resultStr = "Application running on http://localhost:${configuration.serverPort} "
                    if (configuration.debug) {
                        resultStr += "(debugger on ${configuration.debugPort})"
                    }
                    project.logger.lifecycle(resultStr)
                    project.logger.lifecycle('Press [Ctrl+C] to terminate server...')

                    if(stopAfterStart){
                        terminate()
                    } else if(configuration.openInBrowser){
                        openBrowser()
                    }
                } else {
                    project.logger.lifecycle("Server reload complete.")
                }
            }

            if(line.contains('ERROR')){
                // Terminate if server logs an error
                terminate()
            }
        })
    }

    @PackageScope
    def openBrowser() {
        // Build browser GET parameters
        def paramString = ''
        if (configuration.debug) {
            paramString += '?debug'
            paramString += AMPERSAND + browserParameters.join(AMPERSAND)
        } else if(!browserParameters.isEmpty()){
            paramString += '?' + browserParameters.join(AMPERSAND)
        }
        paramString = paramString.replaceAll('\\?$|&$', '')

        // Open browser
        Util.openBrowser((Project)project, "http://localhost:${(Integer)configuration.serverPort}/${paramString}")
    }

    @PackageScope
    def startAndBlock(boolean stopAfterStart=false) {
        def firstStart = true

        while(true){
            // Keep main loop running so task does not end. Task
            // shutdownhook will terminate server

            if(process != null){
                // Process has not been terminated
                project.logger.warn("Server process was not terminated cleanly before re-loading")
                break
            }

            // Start server
            start(firstStart, stopAfterStart)
            firstStart = false

            // Wait until server process calls destroy()
            def exitCode = process.waitFor()
            if(!reloadInProgress && exitCode != 0){
                project.logger.warn("Server process terminated with exit code $exitCode. " +
                        "See ${serverName}.log for further details.")
                terminate()
                break
            }

            if(!configuration.serverRestart || stopAfterStart){
                // Auto-refresh turned off
                break
            }

            reloadInProgress = false
        }
    }

    def terminate() {
        process?.destroy()
        process = null
        project.logger.info("Application server terminated.")
    }

    def reload() {
        reloadInProgress = true
        terminate()
    }

    @PackageScope
    static watchClassDirectoryForChanges(final ApplicationServer server) {
        def project = server.project

        def classesDir
        if(project.vaadinRun.classesDir && project.file(project.vaadinRun.classesDir).exists()){
            classesDir = project.file(project.vaadinRun.classesDir)
        } else {
            classesDir = project.sourceSets.main.output.classesDir
        }

        if(classesDir && classesDir.exists()){
            Util.watchDirectoryForChanges(project, (File) classesDir, { WatchKey key, WatchEvent event ->
                Path basePath = (Path) key.watchable();
                WatchEvent<Path> watchEventPath = (WatchEvent<Path>) event
                Path path =  basePath.resolve(watchEventPath.context())
                File file = path.toFile()

                // Ignore client classes, as restarting server will not do you any good
                if(Util.getWidgetset(project)){
                    def widgetsetPath = Util.getWidgetset(project).tokenize('.')[0..-2]
                            .join(File.separator) + File.separator +'client' + File.separator
                    if(file.absolutePath.contains(widgetsetPath)){
                        // TODO when file based widgetset recompiling is
                        // implemented we could recompile the widgetset here instead
                        project.logger.info("Ignored client side class change in ${file.absolutePath}")
                        return false
                    }
                }

                if(server.configuration.serverRestart && server.process){
                    // Force restart of server
                    project.logger.lifecycle(RELOADING_SERVER_MESSAGE)
                    server.reload()
                }
                false
            })
        }
    }

    @PackageScope
    static watchThemeDirectoryForChanges(final ApplicationServer server) {
        def project = server.project
        File themesDir = Util.getThemesDirectory(project)
        if(themesDir.exists()) {
            def executor = Executors.newSingleThreadScheduledExecutor()
            ScheduledFuture currentTask

            Util.watchDirectoryForChanges(project, themesDir, { WatchKey key, WatchEvent event ->
                if(event.context().toString().toLowerCase().endsWith(".scss")){
                    if(currentTask){
                        currentTask.cancel(true)
                    }
                    currentTask = executor.schedule({

                        // Recompile theme
                        CompileThemeTask.compile(project, true)

                        // Restart
                        if(server.configuration.serverRestart && server.process){
                            // Force restart of server
                            project.logger.lifecycle(RELOADING_SERVER_MESSAGE)
                            server.reload()
                        }
                    }, 1 , TimeUnit.SECONDS)
                }
                false
            })
        }
    }
}

