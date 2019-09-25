/*
 * Copyright 2018 John Ahlroos
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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.MessageLogger
import com.devsoap.plugin.servers.ApplicationServer
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.options.Option
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Runs the application on a application server
 *
 * @author John Ahlroos
 * @since 1.0
 */
class RunTask extends DefaultTask {

    static final String NAME = 'vaadinRun'

    static final String SERVER_RESTART_DEPRECATED_MESSAGE = 'The classes will automatically be hotswapped by' +
            ' Spring Loaded.'

    /**
     * The server instance of the running server. Internal only, made public so cleanup thread can clean it up.
     */
    @Internal
    ApplicationServer serverInstance

    /**
     * Should the server be stopped after it has been started
     */
    @Option(option = 'stopAfterStart', description = 'Should the server stop after starting')
    boolean stopAfterStarting = false

    /**
     * Should the browser be shown
     */
    // FIXME Is this duplicate to the property openInBrowser?
    @Option(option = 'nobrowser', description = 'Do not open browser after server has started')
    boolean nobrowser = false

    private final Property<String> server = project.objects.property(String)
    private final Property<Boolean> debug = project.objects.property(Boolean)
    private final Property<Integer> debugPort = project.objects.property(Integer)
    private final ListProperty<String> jvmArgs = project.objects.listProperty(String)
    private final Property<Integer> serverPort = project.objects.property(Integer)
    private final Property<Boolean> themeAutoRecompile = project.objects.property(Boolean)
    private final Property<Boolean> openInBrowser = project.objects.property(Boolean)
    private final Property<String> classesDir = project.objects.property(String)

    /**
     * Intern cleanup thread for when the JVM terminates. Needs to be public so it can be accessed from another thread.
     */
    @Internal
    final Thread cleanupThread = new Thread({
        if ( serverInstance ) {
            serverInstance.terminate()
            serverInstance = null
        }

        try {
            Runtime.getRuntime().removeShutdownHook(cleanupThread)
        } catch (IllegalStateException e) {
            // Shutdown of the JVM in progress already, we don't need to remove the hook it will be removed by the JVM
            project.logger.debug('Shutdownhook could not be removed. This can be ignored.', e)
        }
    })

    RunTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        dependsOn(CompileThemeTask.NAME)
        description = 'Runs the Vaadin application'
        Runtime.getRuntime().addShutdownHook(cleanupThread)

        server.set('payara')
        debug.set(true)
        debugPort.set(8000)
        jvmArgs.empty()
        serverPort.set(8080)
        themeAutoRecompile.set(true)
        openInBrowser.set(true)
        classesDir.set(null)
    }

    /**
     * Run the application server
     */
    @TaskAction
    void run() {
        if ( nobrowser ) {
            setOpenInBrowser(false)
        }
        serverInstance = ApplicationServer.get(project, [:])
        serverInstance.startAndBlock(stopAfterStarting)
    }

    /**
     * Get application server in use.
     * <p>
     * Available options are
     * <ul>
     *     <li>payara - Webserver with EJB/CDI support</li>
     *     <li>jetty - Plain J2EE web server</li>
     * </ul>
     * Default server is payara.
     */
    String getServer() {
        server.get()
    }

    /**
     * Set application server to use.
     * <p>
     * Available options are
     * <ul>
     *     <li>payara - Webserver with EJB/CDI support</li>
     *     <li>jetty - Plain J2EE web server</li>
     * </ul>
     * Default server is payara.
     */
    void setServer(String server) {
        this.server.set(server)
    }

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    Boolean getDebug() {
        debug.get()
    }

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    void setDebug(Boolean debug) {
        this.debug.set(debug)
    }

    /**
     * The port the debugger listens to
     */
    Integer getDebugPort() {
        debugPort.get()
    }

    /**
     * The port the debugger listens to
     */
    void setDebugPort(Integer debugPort) {
        this.debugPort.set(debugPort)
    }

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    String[] getJvmArgs() {
        jvmArgs.present ? jvmArgs.get().toArray(new String[jvmArgs.get().size()]) : null
    }

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    void setJvmArgs(String[] args) {
        jvmArgs.set(Arrays.asList(args))
    }

    /**
     * Should the server restart after every change.
     * 
     * @deprecated No longer in use since JVM hotswapping was taken into use in 1.2.4
     */
    @Deprecated
    Boolean getServerRestart() {
        MessageLogger.nagUserOfDiscontinuedProperty(new Throwable(SERVER_RESTART_DEPRECATED_MESSAGE))
        false
    }

    /**
     * Should the server restart after every change.
     *
     * @deprecated No longer in use since JVM htoswapping was itekn into use in 1.2.4
     */
    @Deprecated
    void setServerRestart(Boolean restart) {
        MessageLogger.nagUserOfDiscontinuedProperty(new Throwable(SERVER_RESTART_DEPRECATED_MESSAGE))
        restart
    }

    /**
     * The port the vaadin application should run on
     */
    Integer getServerPort() {
        serverPort.get()
    }

    /**
     * The port the vaadin application should run on
     */
    void setServerPort(Integer port) {
        serverPort.set(port)
    }

    /**
     * Should theme be recompiled when SCSS file is changes.
     */
    Boolean getThemeAutoRecompile() {
        themeAutoRecompile.get()
    }

    /**
     * Should theme be recompiled when SCSS file is changes.
     */
    void setThemeAutoRecompile(Boolean recompile) {
        themeAutoRecompile.set(recompile)
    }

    /**
     * Should the application be opened in a browser when it has been launched
     */
    Boolean getOpenInBrowser() {
        openInBrowser.get()
    }

    /**
     * Should the application be opened in a browser when it has been launched
     */
    void setOpenInBrowser(Boolean open) {
        openInBrowser.set(open)
    }

    /**
     * The directory where compiled application classes are found
     */
    String getClassesDir() {
        classesDir.getOrNull()
    }

    /**
     * The directory where compiled application classes are found
     */
    void setClassesDir(String dir) {
        classesDir.set(dir)
    }
}