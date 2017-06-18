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
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState
import org.gradle.api.tasks.TaskAction

/**
 * Runs the GWT codeserver as well as runs the application in SuperDevMode mode.
 *
 * @author John Ahlroos
 */
class SuperDevModeTask extends DefaultTask {

    static final String NAME = 'vaadinSuperDevMode'

    Process codeserverProcess = null

    ApplicationServer serverInstance = null

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
        if ( codeserverProcess ) {
            codeserverProcess.destroy()
            codeserverProcess = null
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

    SuperDevModeTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        description = "Run Super Development Mode for easier client widget development."
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
        codeServerPort.set(9876)
        extraArgs.set(null)
        logLevel.set('INFO')
    }

    @TaskAction
    def run() {
        if ( !Util.getWidgetset(project) ) {
            throw new GradleException("No widgetset found in project.")
        }


        if(getNoserver()){
            runCodeServer {
                codeserverProcess.waitForOrKill(1)
            }
        } else {
            runCodeServer({
                serverInstance = ApplicationServer.get(project,
                        [superdevmode:"${getBindAddress()}:${getCodeServerPort()}"])
                serverInstance.startAndBlock()
                codeserverProcess.waitForOrKill(1)
            })
        }
    }

    def runCodeServer(Closure readyClosure) {
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetsDir = Util.getWidgetsetDirectory(project)
        widgetsetsDir.mkdirs()

        FileCollection classpath = Util.getClientCompilerClassPath(project)

        List superdevmodeProcess = [Util.getJavaBinary(project)]
        superdevmodeProcess += ['-cp', classpath.asPath]
        superdevmodeProcess += 'com.google.gwt.dev.codeserver.CodeServer'
        superdevmodeProcess += ['-bindAddress', getBindAddress()]
        superdevmodeProcess += ['-port', getCodeServerPort()]
        superdevmodeProcess += ['-workDir', widgetsetsDir.canonicalPath]
        superdevmodeProcess += ['-src', javaDir.canonicalPath]
        superdevmodeProcess += ['-logLevel', getLogLevel()]
        superdevmodeProcess += ['-noprecompile']

        if ( getExtraArgs() ) {
            superdevmodeProcess += getExtraArgs() as List
        }

        superdevmodeProcess += Util.getWidgetset(project)

        codeserverProcess = superdevmodeProcess.execute([], project.buildDir)

        Util.logProcess(project, codeserverProcess, 'superdevmode.log') { line ->
            if ( line.contains('The code server is ready') ) {
                readyClosure.call()
                return false
            }
            true
        }

        codeserverProcess.waitFor()
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
    void setServer(String server) {
        this.serverInstance.set(server)
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
    void setLogLevel(String logLevel) {
        this.logLevel.set(logLevel)
    }
}
