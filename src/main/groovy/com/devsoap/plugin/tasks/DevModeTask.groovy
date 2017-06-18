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

import com.devsoap.plugin.servers.ApplicationServer
import com.devsoap.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.PropertyState
import org.gradle.api.tasks.TaskAction

/**
 * Runs GWT DevMode
 *
 * @author John Ahlroos
 * @deprecated Replaced by SuperDevMode
 */
@Deprecated
class DevModeTask extends DefaultTask {

    public static final String NAME = 'vaadinDevMode'

    Process devModeProcess

    def serverInstance

    final PropertyState<String> server = project.property(String)
    final PropertyState<Boolean> debug = project.property(Boolean)
    final PropertyState<Integer> debugPort = project.property(Integer)
    final PropertyState<List<String>> jvmArgs = project.property(List)
    final PropertyState<Boolean> serverRestart = project.property(Boolean)
    final PropertyState<Integer> serverPort = project.property(Integer)
    final PropertyState<Boolean> themeAutoRecompile = project.property(Boolean)
    final PropertyState<Boolean> openInBrowser = project.property(Boolean)
    final PropertyState<String> classesDir = project.property(String)
    final PropertyState<Boolean> noserver = project.property(Boolean)
    final PropertyState<String> bindAddress = project.property(String)
    final PropertyState<Integer> codeServerPort = project.property(Integer)
    final PropertyState<List<String>> extraArgs = project.property(List)
    final PropertyState<String> logLevel = project.property(String)

    def cleanupThread = new Thread({
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
        jvmArgs.set(null)
        serverRestart.set(true)
        serverPort.set(8080)
        themeAutoRecompile.set(true)
        openInBrowser.set(true)
        classesDir.set(null)
        noserver.set(false)
        bindAddress.set('127.0.0.1')
        codeServerPort.set(9997)
        extraArgs.set(null)
        logLevel.set('INFO')
    }

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

    protected void runDevelopmentMode() {
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
     */
    Boolean getServerRestart() {
        serverRestart.get()
    }

    /**
     * Should the server restart after every change.
     */
    void setServerRestart(Boolean restart) {
        serverRestart.set(restart)
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