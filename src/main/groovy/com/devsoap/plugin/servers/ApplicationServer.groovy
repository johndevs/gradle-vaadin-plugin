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
package com.devsoap.plugin.servers

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.Util
import com.devsoap.plugin.configuration.ApplicationServerConfiguration
import com.devsoap.plugin.configuration.CompileThemeConfiguration
import com.devsoap.plugin.tasks.BuildClassPathJar
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CompressCssTask
import groovy.io.FileType
import groovy.transform.PackageScope
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.logging.Level

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
                                    Map browserParameters = [:],
                                    ApplicationServerConfiguration configuration) {
        switch(configuration.server) {
            case PayaraApplicationServer.NAME:
                return new PayaraApplicationServer(project, browserParameters, configuration)
            case JettyApplicationServer.NAME:
                return new JettyApplicationServer(project, browserParameters, configuration)
            default:
                throw new IllegalArgumentException("Server name not recognized. Must be either payara or jetty.")
        }
    }

    Process process

    boolean reloadInProgress = false

    final Project project

    Map browserParameters = [:]

    ApplicationServerConfiguration configuration

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
    protected ApplicationServer(Project project, Map browserParameters, ApplicationServerConfiguration configuration) {
        this.project = project
        this.browserParameters = browserParameters
        this.configuration = configuration
    }

    abstract String getServerRunner()

    abstract String getServerName()

    abstract String getSuccessfullyStartedLogToken()

    abstract void defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies)

    /**
     * Get the class path of the server
     *
     * @return
     *      the files as a FileCollection
     */
    FileCollection getClassPath() {
        FileCollection cp
        if ( project.vaadin.useClassPathJar ) {
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            cp = project.files(pathJarTask.archivePath)
        } else {
            cp = (Util.getWarClasspath(project) + project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER] )
                    .filter { it.file && it.canonicalFile.name.endsWith(JAR_FILE_POSTFIX)}
        }
        cp
    }

    /**
     * Creates the classpath file containing the project classpath
     *
     * @param buildDir
     *      the build directory
     * @return
     *      the classpath file
     */
    File makeClassPathFile(File buildDir) {
        def buildClasspath = new File(buildDir, 'classpath.txt')
        buildClasspath.text = Util.getWarClasspath(project)
                .filter { it.file && it.canonicalFile.name.endsWith(JAR_FILE_POSTFIX)}
                .join(";")
        buildClasspath
    }

    /**
     * Override to to configure the server process before execution. This is the best place for example
     * to add system properties.
     *
     * @param parameters
     *      the command line parameters
     */
    void configureProcess(List<String> parameters) {
        // Debug
        if ( configuration.debug ) {
            parameters.add('-Xdebug')
            parameters.add("-Xrunjdwp:transport=dt_socket,address=${configuration.debugPort},server=y,suspend=n")
        }

        // JVM options
        if ( configuration.debug ) {
            parameters.add('-ea')
        }

        parameters.add('-cp')
        parameters.add(classPath.asPath)

        if ( configuration.jvmArgs ) {
            parameters.addAll(configuration.jvmArgs)
        }

        // Program args
        parameters.add(serverRunner)

        parameters.add(configuration.serverPort.toString())

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        parameters.add(webAppDir.canonicalPath + File.separator)

        SourceSetContainer sourceSets = project.sourceSets
        SourceSet mainSourceSet = sourceSets.main

        List<File> classesDirs = new ArrayList<>(mainSourceSet.output.classesDirs.toList())
        File resourcesDir = mainSourceSet.output.resourcesDir
        if ( configuration.classesDir ) {
            classesDirs.add(0, project.file(configuration.classesDir));
            resourcesDir = project.file(configuration.classesDir)
        }

        parameters.add(classesDirs.collect { it.canonicalPath + File.separator}.join(','))
        parameters.add(resourcesDir.canonicalPath + File.separator)

        if ( project.logger.debugEnabled ) {
            parameters.add(Level.FINEST.name)
        } else {
            parameters.add(Level.INFO.name)
        }

        parameters.add(project.name)

        def buildDir = new File(project.buildDir, serverName)
        buildDir.mkdirs()
        parameters.add(buildDir.absolutePath)
    }

    boolean start(boolean firstStart=false, boolean stopAfterStart=false) {
        if ( process ) {
            project.logger.error('Server is already running.')
            return false
        }

        project.logger.info("Starting $serverName...")

        def appServerProcess = [Util.getJavaBinary(project)]

        configureProcess(appServerProcess)

        def buildDir = new File(project.buildDir, serverName)
        makeClassPathFile(buildDir)

        if ( executeServer(appServerProcess, firstStart) ) {
            monitorLog(firstStart, stopAfterStart)
        }
    }

    @PackageScope
    boolean executeServer(List appServerProcess, boolean firstStart=false) {
        project.logger.debug("Running server with the command: "+appServerProcess)
        process = appServerProcess.execute([], project.buildDir)

        if ( !process.alive ) {
            // Something is avery, warn user and return
            throw new GradleException("Server failed to start. Exited with exit code ${process.exitValue()}")
        }

        // Watch for changes in classes
        if ( firstStart && configuration.serverRestart ) {
            def self = this
            GradleVaadinPlugin.THREAD_POOL.submit {
                watchClassDirectoryForChanges(self)
            }
        }

        // Watch for changes in theme
        if ( firstStart && configuration.themeAutoRecompile ) {
            def self = this
            GradleVaadinPlugin.THREAD_POOL.submit {
                watchThemeDirectoryForChanges(self)
            }
        }
        true
    }

    @PackageScope
    void monitorLog(boolean firstStart=false, boolean stopAfterStart=false) {
        Util.logProcess(project, process, "${serverName}.log") { line ->
            if ( line.contains(successfullyStartedLogToken) ) {
                if ( firstStart ) {
                    def resultStr = "Application running on http://localhost:${configuration.serverPort} "
                    if ( configuration.debug ) {
                        resultStr += "(debugger on ${configuration.debugPort})"
                    }
                    project.logger.lifecycle(resultStr)
                    project.logger.lifecycle('Press [Ctrl+C] to terminate server...')

                    if ( stopAfterStart ) {
                        terminate()
                        return false
                    } else if ( configuration.openInBrowser ) {
                        openBrowser()
                    }
                } else {
                    project.logger.lifecycle("Server reload complete.")
                }
            }

            if ( line.contains('ERROR') ) {
                // Terminate if server logs an error
                terminate()
                return false
            }
            true
        }
    }

    @PackageScope
    void openBrowser() {
        // Build browser GET parameters
        String paramString = ''
        if ( configuration.debug ) {
            paramString += '?debug'
            paramString += AMPERSAND + browserParameters.collect {key,value ->
                "$key=$value"
            }.join(AMPERSAND)
        } else if ( !browserParameters.isEmpty() ) {
            paramString += '?' + browserParameters.collect {key,value ->
                "$key=$value"
            }.join(AMPERSAND)
        }
        paramString = paramString.replaceAll('\\?$|&$', '')

        // Open browser
        Util.openBrowser((Project)project, "http://localhost:${(Integer)configuration.serverPort}/${paramString}")
    }

    @PackageScope
    void startAndBlock(boolean stopAfterStart=false) {
        def firstStart = true

        while(true) {
            // Keep main loop running so task does not end. Task
            // shutdownhook will terminate server

            if ( process != null ) {
                // Process has not been terminated
                project.logger.warn("Server process was not terminated cleanly before re-loading")
                break
            }

            // Start server
            start(firstStart, stopAfterStart)
            firstStart = false

            // Wait until server process calls destroy()
            def exitCode = process.waitFor()
            if ( !reloadInProgress && exitCode != 0 ) {
                terminate()
                if(!stopAfterStart){
                    if(project.vaadin.logToConsole){
                        throw new GradleException("Server process terminated with exit code $exitCode. " +
                                "See console output for further details (use --info for more details).")
                    } else {
                        throw new GradleException("Server process terminated with exit code $exitCode. " +
                                "See build/logs/${serverName}.log for further details.")
                    }
                }
            }

            if ( !configuration.serverRestart || stopAfterStart ) {
                // Auto-refresh turned off
                break
            }

            reloadInProgress = false
        }
    }

    void terminate() {
        process?.destroy()
        process = null
        project.logger.info("Application server terminated.")
    }

    void reload() {
        reloadInProgress = true
        terminate()
    }

    @PackageScope
    static void watchClassDirectoryForChanges(final ApplicationServer server) {
        Project project = server.project

        def serverConfiguration = Util.findOrCreateExtension(project, ApplicationServerConfiguration)
        List<File> classesDirs = []
        if ( serverConfiguration.classesDir && project.file(serverConfiguration.classesDir).exists() ) {
            classesDirs.add(project.file(serverConfiguration.classesDir))
        }

        classesDirs.addAll(project.sourceSets.main.output.classesDirs.toList())

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()

        classesDirs.each {
            ScheduledFuture currentTask
            if(it.exists()) {
                Util.watchDirectoryForChanges(project, (File) classesDir, { WatchKey key, WatchEvent event ->
                    if (server.process && server.configuration.serverRestart ) {
                        if ( currentTask ) {
                            currentTask.cancel(true)
                        }
                        currentTask = executor.schedule({
                            // Force restart of server
                            project.logger.lifecycle(RELOADING_SERVER_MESSAGE)
                            server.reload()
                        }, 1 , TimeUnit.SECONDS)
                    }
                    true
                })
            }
        }
    }

    @PackageScope
    static void watchThemeDirectoryForChanges(final ApplicationServer server) {
        Project project = server.project
        CompileThemeConfiguration compileConf = Util.findOrCreateExtension(project, CompileThemeConfiguration)

        File themesDir = Util.getThemesDirectory(project)
        if ( themesDir.exists() ) {
            def executor = Executors.newSingleThreadScheduledExecutor()
            ScheduledFuture currentTask

            Util.watchDirectoryForChanges(project, themesDir, { WatchKey key, WatchEvent event ->
                if (server.process && event.context().toString().toLowerCase().endsWith(".scss") ) {
                    if ( currentTask ) {
                        currentTask.cancel(true)
                    }
                    currentTask = executor.schedule({

                        // Recompile theme
                        CompileThemeTask.compile(project, true)

                        // Recompress theme
                        if(compileConf.compress){
                            CompressCssTask.compress(project, true)
                        }

                        // Restart
                        if ( server.configuration.serverRestart && server.process ) {
                            // Force restart of server
                            project.logger.lifecycle(RELOADING_SERVER_MESSAGE)
                            server.reload()
                        }
                    }, 1 , TimeUnit.SECONDS)
                }
                true
            })
        }
    }
}

