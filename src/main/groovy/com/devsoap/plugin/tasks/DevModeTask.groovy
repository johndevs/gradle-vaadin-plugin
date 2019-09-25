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
import com.devsoap.plugin.Util
import com.devsoap.plugin.servers.ApplicationServer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Runs GWT DevMode
 *
 * @author John Ahlroos
 * @since 1.0
 * @deprecated Replaced by SuperDevMode
 */
@Deprecated
class DevModeTask extends DefaultTask {

    static final String NAME = 'vaadinDevMode'

    /**
     * The development mode process. Internal only, made public so cleanup thread can clean it up.
     */
    @Internal
    Process devModeProcess

    /**
     * The server instance of the running server. Internal only, made public so cleanup thread can clean it up.
     */
    @Internal
    ApplicationServer serverInstance

    private final Property<String> server = project.objects.property(String)
    private final Property<Boolean> debug = project.objects.property(Boolean)
    private final Property<Integer> debugPort = project.objects.property(Integer)
    private final ListProperty<String> jvmArgs = project.objects.listProperty(String)
    private final Property<Integer> serverPort = project.objects.property(Integer)
    private final Property<Boolean> themeAutoRecompile = project.objects.property(Boolean)
    private final Property<Boolean> openInBrowser = project.objects.property(Boolean)
    private final Property<String> classesDir = project.objects.property(String)
    private final Property<Boolean> noserver = project.objects.property(Boolean)
    private final Property<String> bindAddress = project.objects.property(String)
    private final Property<Integer> codeServerPort = project.objects.property(Integer)
    private final ListProperty<String> extraArgs = project.objects.listProperty(String)
    private final Property<String> logLevel = project.objects.property(String)

    /**
     * Intern cleanup thread for when the JVM terminates. Needs to be public so it can be accessed from another thread.
     */
    @Internal
    final Thread cleanupThread = new Thread({
        if ( devModeProcess ) {
            devModeProcess.destroy()
            devModeProcess = null
        }
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

    DevModeTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME)
        description = "Run Development Mode for easier debugging and development of client widgets."
        Runtime.getRuntime().addShutdownHook(cleanupThread)

        server.set('payara')
        debug.set(true)
        debugPort.set(8000)
        jvmArgs.empty()
        serverPort.set(8080)
        themeAutoRecompile.set(true)
        openInBrowser.set(true)
        classesDir.set(null)
        noserver.set(false)
        bindAddress.set('127.0.0.1')
        codeServerPort.set(9997)
        extraArgs.empty()
        logLevel.set('INFO')
    }

    /**
     * Starts the server and runs the DevMode server
     */
    @TaskAction
    void run() {
        if ( !Util.getWidgetset(project) ) {
            throw new GradleException("No widgetset found in project.")
        }

        runDevelopmentMode()

        if ( !noserver ) {
            serverInstance = ApplicationServer.get(
                    project,
                    ['gwt.codesvr':"${getBindAddress()}:${getCodeServerPort()}"]
            ).startAndBlock()
            devModeProcess.waitForOrKill(1)
        } else {
            devModeProcess.waitFor()
        }
    }

    private void runDevelopmentMode() {
        def classpath = Util.getClientCompilerClassPath(project)
        RunTask runTask = project.tasks.getByName(RunTask.NAME)

        File devmodeDir = new File(project.buildDir, 'devmode')

        File deployDir = new File(devmodeDir, 'deploy')
        deployDir.mkdirs()

        File logsDir = new File(devmodeDir, 'logs')
        logsDir.mkdirs()

        File genDir = new File(devmodeDir, 'gen')
        genDir.mkdirs()

        File widgetsetDir = Util.getWidgetsetDirectory(project)
        widgetsetDir.mkdirs()

        List devmodeProcess = [Util.getJavaBinary(project)]
        devmodeProcess += ["-Djava.io.tmpdir=${temporaryDir.canonicalPath}"]
        devmodeProcess += ['-cp', classpath.asPath]
        devmodeProcess += 'com.google.gwt.dev.DevMode'
        devmodeProcess += Util.getWidgetset(project)
        devmodeProcess += '-noserver'
        devmodeProcess += ['-war', widgetsetDir.canonicalPath]
        devmodeProcess += ['-gen', genDir.canonicalPath]
        devmodeProcess += ['-startupUrl', "http://localhost:${runTask.serverPort}"]
        devmodeProcess += ['-logLevel', getLogLevel()]
        devmodeProcess += ['-deploy', deployDir.canonicalPath]
        devmodeProcess += ['-workDir', devmodeDir.canonicalPath]
        devmodeProcess += ['-logdir', logsDir.canonicalPath]
        devmodeProcess += ['-codeServerPort', getCodeServerPort()]
        devmodeProcess += ['-bindAddress', getBindAddress()]

        if ( getExtraArgs() ) {
            devmodeProcess += getExtraArgs() as List
        }

        devModeProcess = devmodeProcess.execute([], project.buildDir)

        Util.logProcess(project, devModeProcess, 'devmode.log') { true }
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
        serverInstance.get()
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
    void setServer(String serverName) {
        serverInstance.set(serverName)
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
    void setDebug(Boolean debugEnabled) {
        debug.set(debugEnabled)
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
        MessageLogger.nagUserOfDiscontinuedProperty(new Throwable(RunTask.SERVER_RESTART_DEPRECATED_MESSAGE))
        false
    }

    /**
     * Should the server restart after every change.
     *
     * @deprecated No longer in use since JVM hotswapping was taken into use in 1.2.4
     */
    @Deprecated
    void setServerRestart(Boolean restart) {
        MessageLogger.nagUserOfDiscontinuedProperty(new Throwable(RunTask.SERVER_RESTART_DEPRECATED_MESSAGE))
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

    /**
     * Should the internal server be used.
     */
    Boolean getNoserver() {
        noserver.get()
    }

    /**
     * Should the internal server be used.
     */
    void setNoserver(Boolean noServer) {
        noserver.set(noServer)
    }

    /**
     * To what host or ip should development mode bind itself to. By default localhost.
     */
    String getBindAddress() {
        bindAddress.get()
    }

    /**
     * To what host or ip should development mode bind itself to. By default localhost.
     */
    void setBindAddress(String bindAddress) {
        this.bindAddress.set(bindAddress)
    }

    /**
     * To what port should development mode bind itself to.
     */
    Integer getCodeServerPort() {
        codeServerPort.get()
    }

    /**
     * To what port should development mode bind itself to.
     */
    void setCodeServerPort(Integer port) {
        codeServerPort.set(port)
    }

    /**
     * Extra arguments passed to the code server
     */
    String[] getExtraArgs() {
        extraArgs.getOrNull()?.toArray(new String[extraArgs.get().size()])
    }

    /**
     * Extra arguments passed to the code server
     */
    void setExtraArgs(String[] args) {
        extraArgs.set(Arrays.asList(args))
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
    void setLogLevel(String level) {
        logLevel.set(level)
    }
}