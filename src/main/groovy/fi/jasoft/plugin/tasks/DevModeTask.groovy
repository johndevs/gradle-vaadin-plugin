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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.configuration.SuperDevModeConfiguration
import fi.jasoft.plugin.servers.ApplicationServer
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.WarPluginConvention
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

    def Process devModeProcess

    def server

    def SuperDevModeConfiguration configuration

    def cleanupThread = new Thread({
        if ( devModeProcess ) {
            devModeProcess.destroy()
            devModeProcess = null
        }
        if ( server ) {
            server.terminate()
            server = null
        }
        try {
            Runtime.getRuntime().removeShutdownHook(cleanupThread)
        } catch (IllegalStateException e) {
            // Shutdown of the JVM in progress already, we don't need to remove the hook it will be removed by the JVM
            project.logger.debug('Shutdownhook could not be removed. This can be ignored.', e)
        }
    })

    public DevModeTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME)
        description = "Run Development Mode for easier debugging and development of client widgets."
        Runtime.getRuntime().addShutdownHook(cleanupThread)
        configuration = Util.findOrCreateExtension(project, NAME, SuperDevModeConfiguration)
    }

    @TaskAction
    public void run() {
        if ( !Util.getWidgetset(project) ) {
            throw new GradleException("No widgetset found in project.")
        }

        runDevelopmentMode()

        if ( !configuration.noserver ) {
            server = ApplicationServer.get(
                    project,
                    ["gwt.codesvr=${configuration.bindAddress}:${configuration.codeServerPort}"]
            ).startAndBlock()
            devModeProcess.waitForOrKill(1)
        } else {
            devModeProcess.waitFor()
        }
    }

    protected void runDevelopmentMode() {
        def classpath = Util.getClientCompilerClassPath(project)

        def devmodeDir = new File(project.buildDir, 'devmode')

        def deployDir = new File(devmodeDir, 'deploy')
        deployDir.mkdirs()

        def logsDir = new File(devmodeDir, 'logs')
        logsDir.mkdirs()

        def genDir = new File(devmodeDir, 'gen')
        genDir.mkdirs()

        def widgetsetDir = Util.getWidgetsetDirectory(project)
        widgetsetDir.mkdirs()

        def devmodeProcess = [Util.getJavaBinary(project)]
        devmodeProcess += ['-cp', classpath.asPath]
        devmodeProcess += 'com.google.gwt.dev.DevMode'
        devmodeProcess += Util.getWidgetset(project)
        devmodeProcess += '-noserver'
        devmodeProcess += ['-war', widgetsetDir.canonicalPath]
        devmodeProcess += ['-gen', genDir.canonicalPath]
        devmodeProcess += ['-startupUrl', "http://localhost:${project.vaadinRun.serverPort}"]
        devmodeProcess += ['-logLevel', configuration.logLevel]
        devmodeProcess += ['-deploy', deployDir.canonicalPath]
        devmodeProcess += ['-workDir', devmodeDir.canonicalPath]
        devmodeProcess += ['-logdir', logsDir.canonicalPath]
        devmodeProcess += ['-codeServerPort', configuration.codeServerPort]
        devmodeProcess += ['-bindAddress', configuration.bindAddress]

        if ( configuration.extraArgs ) {
            devmodeProcess += configuration.extraArgs as List
        }

        devModeProcess = devmodeProcess.execute()

        Util.logProcess(project, devModeProcess, 'devmode.log') { true }
    }
}