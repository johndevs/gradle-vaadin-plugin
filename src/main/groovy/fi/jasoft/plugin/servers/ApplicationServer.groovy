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
import fi.jasoft.plugin.Util
import fi.jasoft.plugin.configuration.ApplicationServerConfiguration
import fi.jasoft.plugin.tasks.BuildClassPathJar
import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.RunTask
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

abstract class ApplicationServer {

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
    static ApplicationServer create(Project project,
                                    List browserParameters = [],
                                    ApplicationServerConfiguration configuration=project.vaadinRun.configuration){
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

    def ApplicationServer(Project project, List browserParameters, ApplicationServerConfiguration configuration) {
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
            cp = project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER]
                    .plus(Util.getWarClasspath(project))
                    .filter { it.file && it.canonicalFile.name.endsWith('.jar')}
        }
        cp
    }

    def buildClassPathFile(File buildDir) {
        def buildClasspath = new File(buildDir, 'classpath.txt')
        buildClasspath.text = Util.getWarClasspath(project)
                .filter { it.file && it.canonicalFile.name.endsWith('.jar')}
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

        // Jrebel
        if (project.vaadin.jrebel.enabled && configuration.debug) {
            if (project.vaadin.jrebel.location != null && new File(project.vaadin.jrebel.location).exists()) {
                appServerProcess.add('-noverify')
                appServerProcess.add("-javaagent:${project.vaadin.jrebel.location}")
            } else {
                project.logger.warn('jrebel.jar not found, running without jrebel')
            }
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

        appServerProcess.add(webAppDir.canonicalPath + '/')

        File classesDir = project.sourceSets.main.output.classesDir
        appServerProcess.add(classesDir.canonicalPath + '/')

        if(project.logger.debugEnabled){
            appServerProcess.add('DEBUG')
        } else {
            appServerProcess.add('INFO')
        }

        appServerProcess.add(project.name);

        def buildDir = new File(project.buildDir, serverName)
        buildDir.mkdirs()
        appServerProcess.add(buildDir.absolutePath)

        buildClassPathFile(buildDir)

        // Execute server
        project.logger.debug("Running server with the command: "+appServerProcess)
        process = appServerProcess.execute()

        // Watch for changes in classes
        if(configuration.serverRestart) {
            def self = this
            Thread.start 'Class Directory Watcher', {
                ApplicationServer.watchClassDirectoryForChanges(self)
            }
        }

        // Watch for changes in theme
        if(firstStart && configuration.themeAutoRecompile){
            Thread.start 'Theme Directory Watcher', {
                watchThemeDirectoryForChanges()
            }
        }

        // Build browser GET parameters
        def paramString = ''
        if (configuration.debug) {
            paramString += '?debug'
            paramString += '&' + browserParameters.join('&')
        } else if(!browserParameters.isEmpty()){
            paramString += '?' + browserParameters.join('&')
        }
        paramString = paramString.replaceAll('\\?$|&$', '')

        // Capture log output
        Util.logProcess(project, process, "${serverName}.log", { line ->
            if(line.contains(successfullyStartedLogToken)) {
                if(firstStart) {
                    def resultStr = "Application running on http://localhost:${configuration.serverPort} "
                    if (project.vaadin.jrebel.enabled) {
                        resultStr += "(debugger on ${configuration.debugPort}, JRebel active)"
                    } else if (configuration.debug) {
                        resultStr += "(debugger on ${configuration.debugPort})"
                    }
                    project.logger.lifecycle(resultStr)
                    project.logger.lifecycle('Press [Ctrl+C] to terminate server...')

                    if(stopAfterStart){
                        terminate()
                    } else if(configuration.openInBrowser){
                        Util.openBrowser((Project)project, "http://localhost:${(Integer)configuration.serverPort}/${paramString}")
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
                project.logger.warn("Server process terminated with exit code $exitCode. See ${serverName}.log for further details.")
                terminate()
                break
            }

            if(!project.vaadin.plugin.serverRestart || stopAfterStart){
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

    static watchClassDirectoryForChanges(final ApplicationServer server) {
        def project = server.project
        def classesDir
        if (project.vaadin.plugin.eclipseOutputDir == null) {
            classesDir = project.sourceSets.main.output.classesDir
        } else {
            classesDir = project.file(project.vaadin.plugin.eclipseOutputDir)
        }

        Util.watchDirectoryForChanges(project, (File) classesDir, { WatchKey key, WatchEvent event ->
            Path basePath = (Path) key.watchable();
            WatchEvent<Path> watchEventPath = (WatchEvent<Path>) event
            Path path =  basePath.resolve(watchEventPath.context())
            File file = path.toFile()

            // Ignore client classes, as restarting server will not do you any good
            if(project.vaadinCompile.configuration.widgetset){
                def widgetsetPath = (project.vaadinCompile.configuration.widgetset as String).tokenize('.')[0..-2].join('/')+'/client/'
                if(file.absolutePath.contains(widgetsetPath)){
                    //TODO when file based widgetset recompiling is implmeneted we could recompile the widgetset here instead
                    project.logger.info("Ignored client side class change in ${file.absolutePath}")
                    return false
                }
            }

            if(project.vaadin.plugin.serverRestart && server.process){
                // Force restart of server
                project.logger.lifecycle("Reloading server...")
                server.reload()
            }
            false
        })
    }

    def watchThemeDirectoryForChanges() {
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
                        CompileThemeTask.compile(project, true)
                    }, 1 , TimeUnit.SECONDS)
                }

                !themesDir.exists() // Terminate if theme directory no longer exists
            })
        }
    }
}

