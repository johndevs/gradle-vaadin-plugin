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
package fi.jasoft.plugin

import fi.jasoft.plugin.tasks.CompileThemeTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention

import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ApplicationServer {

    def Process process;

    def final Project project;

    def List browserParameters

    def ApplicationServer(project, browserParameters = []) {
        this.project = project
        this.browserParameters = browserParameters
    }

    def boolean start(firstStart=false) {

        if (process != null) {
            project.logger.error('Server is already running.')
            return false
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = Util.getJettyClassPath(project)

        def appServerProcess = ['java']

        // Debug
        if (project.vaadin.debug) {
            appServerProcess.add('-Xdebug')
            appServerProcess.add("-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n")
        }

        // Jrebel
        if (project.vaadin.jrebel.enabled && project.vaadin.debug) {
            if (project.vaadin.jrebel.location != null && new File(project.vaadin.jrebel.location).exists()) {
                appServerProcess.add('-noverify')
                appServerProcess.add("-javaagent:${project.vaadin.jrebel.location}")
            } else {
                project.logger.warn('jrebel.jar not found, running without jrebel')
            }
        }

        // JVM options
        if (project.vaadin.debug) {
            appServerProcess.add('-ea')
        }

        appServerProcess.add('-cp')
        appServerProcess.add(cp.getAsPath())

        if (project.vaadin.jvmArgs != null) {
            appServerProcess.addAll(project.vaadin.jvmArgs)
        }

        // Program args
        appServerProcess.add('fi.jasoft.plugin.ApplicationServerRunner')

        appServerProcess.add(project.vaadin.serverPort)

        appServerProcess.add(webAppDir.canonicalPath + '/')

        File classesDir = project.file("build/classes")
        appServerProcess.add(classesDir.canonicalPath + '/')

        if(project.logger.debugEnabled){
            appServerProcess.add('DEBUG')
        } else {
            appServerProcess.add('INFO')
        }

        // Execute server
        process = appServerProcess.execute()

        // Watch for changes in classes
        if(project.vaadin.plugin.jettyAutoRefresh) {
            def self = this
            Thread.start 'Class Directory Watcher', {
                watchClassDirectoryForChanges(self)
            }
        }

        // Watch for changes in theme
        if(firstStart && project.vaadin.plugin.themeAutoRecompile){
            Thread.start 'Theme Directory Watcher', {
                watchThemeDirectoryForChanges()
            }
        }

        // Build browser GET parameters
        def paramString = ''
        if (project.vaadin.debug) {
            paramString += '?debug'
            paramString += '&' + browserParameters.join('&')
        } else if(!browserParameters.isEmpty()){
            paramString += '?' + browserParameters.join('&')
        }
        paramString = paramString.replaceAll('\\?$|&$', '')

        // Capture log output
        Util.logProcess(project, process, 'jetty.log', { line ->
            if(line.contains('org.eclipse.jetty.server.Server - Started')) {
                if(firstStart) {
                    def resultStr = "Application running on http://localhost:${project.vaadin.serverPort} "
                    if (project.vaadin.jrebel.enabled) {
                        resultStr += "(debugger on ${project.vaadin.debugPort}, JRebel active)"
                    } else if (project.vaadin.debug) {
                        resultStr += "(debugger on ${project.vaadin.debugPort})"
                    }
                    project.logger.lifecycle(resultStr)
                    project.logger.lifecycle('Press [Ctrl+C] to terminate server...')

                    Util.openBrowser((Project)project, "http://localhost:${(Integer)project.vaadin.serverPort}/${paramString}")
                } else {
                    project.logger.lifecycle("Server reload complete.")
                }
            }
        })
    }

    def startAndBlock() {
        def firstStart = true

        while(true){
            // Keep main loop running so task does not end. Task
            // shutdownhook will terminate server

            if(process != null){
                // Process has not been terminated
                project.logger.warn("Server process was not terminated cleanly before re-loading")
            }

            // Start server
            start(firstStart)
            firstStart = false

            // Wait until server process calls destroy()
            process.waitFor()

            if(!project.vaadin.plugin.jettyAutoRefresh){
                // Auto-refresh turned off
                break
            }
        }
    }

    def terminate() {
        if(process){
            process.destroy()
            process = null
            project.logger.info("Application server terminated.")
        }
    }

    def watchClassDirectoryForChanges(final ApplicationServer server) {
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
            if(project.vaadin.widgetset){
                def widgetsetPath = (project.vaadin.widgetset as String).tokenize('.')[0..-2].join('/')+'/client/'
                if(file.absolutePath.contains(widgetsetPath)){
                    //TODO when file based widgetset recompiling is implmeneted we could recompile the widgetset here instead
                    project.logger.info("Ignored client side class change in ${file.absolutePath}")
                    return false
                }
            }

            if(project.vaadin.plugin.jettyAutoRefresh){
                // Force restart of server
                server.terminate()
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
