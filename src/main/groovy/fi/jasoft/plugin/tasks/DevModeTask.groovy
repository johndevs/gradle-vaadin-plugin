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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.servers.ApplicationServer
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class DevModeTask extends DefaultTask {

    public static final String NAME = 'vaadinDevMode'

    def Process devModeProcess

    def server

    def cleanupThread = new Thread({
        if(devModeProcess) {
            devModeProcess.destroy()
            devModeProcess = null
        }
        if(server) {
            server.terminate()
            server = null
        }
        try {
            Runtime.getRuntime().removeShutdownHook(cleanupThread)
        } catch(IllegalStateException e){
            // Shutdown of the JVM in progress already, we don't need to remove the hook it will be removed by the JVM
        }
    })

    public DevModeTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME)
        description = "Run Development Mode for easier debugging and development of client widgets."
        Runtime.getRuntime().addShutdownHook(cleanupThread)
    }

    @TaskAction
    public void run() {

        if (project.vaadin.widgetset == null) {
            project.logger.error("No widgetset defined. Please define a widgetset by using the vaadin.widgetset property.")
            return
        }

        runDevelopmentMode()

        if (!project.vaadin.devmode.noserver) {
            server = ApplicationServer.create(
                    project, ["gwt.codesvr=${project.vaadin.devmode.bindAddress}:${project.vaadin.devmode.codeServerPort}"]
            ).startAndBlock()
            devModeProcess.waitForOrKill(1)
        } else {
            devModeProcess.waitFor()
        }
    }

    protected void runDevelopmentMode() {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        def classpath = Util.getClientCompilerClassPath(project)

        def devmodeProcess = [Util.getJavaBinary(project)]
        devmodeProcess += ['-cp', classpath.getAsPath()]
        devmodeProcess += 'com.google.gwt.dev.DevMode'
        devmodeProcess += project.vaadin.widgetset
        devmodeProcess += '-noserver'
        devmodeProcess += ['-war', webAppDir.canonicalPath + '/VAADIN/widgetsets']
        devmodeProcess += ['-gen', 'build/devmode/gen']
        devmodeProcess += ['-startupUrl', "http://localhost:${project.vaadin.serverPort}"]
        devmodeProcess += ['-logLevel', project.vaadin.gwt.logLevel]
        devmodeProcess += ['-deploy', 'build/devmode/deploy']
        devmodeProcess += ['-workDir', 'build/devmode/']
        devmodeProcess += ['-logdir', 'build/devmode/logs']
        devmodeProcess += ['-codeServerPort', project.vaadin.devmode.codeServerPort]
        devmodeProcess += ['-bindAddress', project.vaadin.devmode.bindAddress]

        if (project.vaadin.devmode.extraArgs) {
            devmodeProcess += project.vaadin.devmode.extraArgs as List
        }

        devModeProcess = devmodeProcess.execute()

        Util.logProcess(project, devModeProcess, 'devmode.log')
    }
}