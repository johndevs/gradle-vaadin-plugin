/*
 * Copyright 2017 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import com.devsoap.plugin.tasks.BuildClassPathJar
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CompressCssTask
import com.devsoap.plugin.tasks.RunTask
import groovy.transform.PackageScope
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.FileCollection
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
 * @since 1.0
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
                                 Map browserParameters = [:]) {
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        switch(runTask.server) {
            case PayaraApplicationServer.NAME:
                return new PayaraApplicationServer(project, browserParameters)
            case JettyApplicationServer.NAME:
                return new JettyApplicationServer(project, browserParameters)
            default:
                throw new IllegalArgumentException("Server name not recognized. Must be either payara or jetty.")
        }
    }

    Process process

    boolean reloadInProgress = false

    final Project project

    Map browserParameters = [:]

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
    protected ApplicationServer(Project project, Map browserParameters) {
        this.project = project
        this.browserParameters = browserParameters
    }

    /**
     * Get the runner name. This is a fully qualified name of the class that will run the server.
     */
    abstract String getServerRunner()

    /**
     * Get the server name. This is the descriptive name of the server
     */
    abstract String getServerName()

    /**
     * Get the string token that when appearing in the log marks the server as started and ready to serve the Vaadin
     * application.
     */
    abstract String getSuccessfullyStartedLogToken()

    /**
     * Configures the needed dependencies for the service
     *
     * @param projectDependencies
     *      the project dependency handler where the dependencies will be injected
     *
     * @param dependencies
     *      The dependency set where the dependencies will be injected
     */
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

        RunTask runTask = project.tasks.getByName(RunTask.NAME)

        // Debug
        if ( runTask.debug ) {
            parameters.add('-Xdebug')
            parameters.add("-Xrunjdwp:transport=dt_socket,address=${runTask.debugPort},server=y,suspend=n")
        }

        // JVM options
        if ( runTask.debug ) {
            parameters.add('-ea')
        }

        parameters.add("-Djava.io.tmpdir=${runTask.temporaryDir.canonicalPath}")

        parameters.add('-cp')
        parameters.add(classPath.asPath)

        if ( runTask.jvmArgs ) {
            parameters.addAll(runTask.jvmArgs)
        }

        // Program args
        parameters.add(serverRunner)

        parameters.add(runTask.serverPort.toString())

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        parameters.add(webAppDir.canonicalPath + File.separator)

        SourceSetContainer sourceSets = project.sourceSets
        SourceSet mainSourceSet = sourceSets.main

        List<File> classesDirs = new ArrayList<>(mainSourceSet.output.classesDirs.toList())
        File resourcesDir = mainSourceSet.output.resourcesDir
        if ( runTask.classesDir ) {
            classesDirs.add(0, project.file(runTask.classesDir))
            resourcesDir = project.file(runTask.classesDir)
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

    /**
     * Starts the server
     *
     * @param firstStart
     *      is this an initial start. <code>false</code> if the server is restarted.
     * @param stopAfterStart
     *      Should the server stop immediately after start. This is mostly used for tests.
     * @return
     *      <code>true</code> if the server started successfully.
     */
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

    /**
     * Executes the server process and starts watching directories for changes
     *
     * @param appServerProcess
     *      the server process to execute
     * @param firstStart
     *      is this the first execution of the server
     * @return
     *      <code>true</code> if the server started successfully.
     */
    protected boolean executeServer(List appServerProcess, boolean firstStart=false) {
        project.logger.debug("Running server with the command: "+appServerProcess)
        process = appServerProcess.execute([], project.buildDir)

        if ( !process.alive ) {
            // Something is avery, warn user and return
            throw new GradleException("Server failed to start. Exited with exit code ${process.exitValue()}")
        }

        // Watch for changes in classes
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        if ( firstStart && runTask.serverRestart ) {
            def self = this
            GradleVaadinPlugin.THREAD_POOL.submit {
                watchClassDirectoryForChanges(self)
            }
        }

        // Watch for changes in theme
        if ( firstStart && runTask.themeAutoRecompile ) {
            def self = this
            GradleVaadinPlugin.THREAD_POOL.submit {
                watchThemeDirectoryForChanges(self)
            }
        }
        true
    }

    /**
     * Monitor the log for tokens
     *
     * @param firstStart
     *      is this the first execution of the server
     * @param stopAfterStart
     *      <code>true</code> if the server should stop right after it has started.
     */
    protected void monitorLog(boolean firstStart=false, boolean stopAfterStart=false) {
        Util.logProcess(project, process, "${serverName}.log") { line ->
            if ( line.contains(successfullyStartedLogToken) ) {
                if ( firstStart ) {
                    RunTask runTask = project.tasks.getByName(RunTask.NAME)
                    def resultStr = "Application running on http://localhost:${runTask.serverPort} "
                    if ( runTask.debug ) {
                        resultStr += "(debugger on ${runTask.debugPort})"
                    }
                    project.logger.lifecycle(resultStr)
                    project.logger.lifecycle('Press [Ctrl+C] to terminate server...')

                    if ( stopAfterStart ) {
                        terminate()
                        return false
                    } else if ( runTask.openInBrowser ) {
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

    /**
     * Open the users browser and point it the running server
     */
    protected void openBrowser() {
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        // Build browser GET parameters
        String paramString = ''
        if ( runTask.debug ) {
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
        Util.openBrowser((Project)project, "http://localhost:${(Integer)runTask.serverPort}/${paramString}")
    }

    /**
     * Start the server and block the process until the process is killed
     *
     * @param stopAfterStart
     *      <code>true</code> if the server should stop right after it has started.
     */
    protected void startAndBlock(boolean stopAfterStart=false) {
        def firstStart = true

        while(true) {
            // Keep main loop running so runTask does not end. Task
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

            RunTask runTask = project.tasks.getByName(RunTask.NAME)
            if ( !runTask.serverRestart || stopAfterStart ) {
                // Auto-refresh turned off
                break
            }

            reloadInProgress = false
        }
    }

    /**
     * Terminate the server if it is running.
     */
    void terminate() {
        process?.destroy()
        process = null
        project.logger.info("Application server terminated.")
    }

    /**
     * Reload the server if it is running.
     */
    void reload() {
        reloadInProgress = true
        terminate()
    }

    /**
     * Watch the class directory for changes and restart the server if changes occur
     *
     * @param server
     *      the server instance to restart
     */
    protected static void watchClassDirectoryForChanges(final ApplicationServer server) {
        Project project = server.project

        final RunTask RUNTASK = project.tasks.getByName(RunTask.NAME)
        List<File> classesDirs = []
        if ( RUNTASK.classesDir && project.file(RUNTASK.classesDir).exists() ) {
            classesDirs.add(project.file(RUNTASK.classesDir))
        }

        classesDirs.addAll(project.sourceSets.main.output.classesDirs.toList())

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()

        classesDirs.each { dir ->
            ScheduledFuture currentTask
            if(dir.exists()) {
                Util.watchDirectoryForChanges(project, dir, { WatchKey key, WatchEvent event ->
                    if (server.process && RUNTASK.serverRestart ) {
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

    /**
     * Watch the theme directory for changes and restart the server if changes occur
     *
     * @param server
     *      the server instance to restart
     */
    protected static void watchThemeDirectoryForChanges(final ApplicationServer server) {
        Project project = server.project
        CompileThemeTask compileThemeTask = project.tasks.getByName(CompileThemeTask.NAME)
        final RunTask RUNTASK = project.tasks.getByName(RunTask.NAME)

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
                        if(compileThemeTask.compress){
                            CompressCssTask.compress(project, true)
                        }

                        // Restart
                        if ( RUNTASK.serverRestart && server.process ) {
                            // Force restart of serverInstance
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

