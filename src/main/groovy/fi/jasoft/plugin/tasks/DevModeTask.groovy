/*
* Copyright 2014 John Ahlroos
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

import fi.jasoft.plugin.ApplicationServer
import fi.jasoft.plugin.DependencyListener
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class DevModeTask extends DefaultTask {

    public static final String NAME = 'vaadinDevMode'

    def Process devModeProcess

    def server

    public DevModeTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME)
        description = "Run Development Mode for easier debugging and development of client widgets."

        addShutdownHook {
            if(devModeProcess) {
                devModeProcess.destroy()
                devModeProcess = null
            }
            if(server) {
                server.terminate()
                server = null
            }
        }
    }

    @TaskAction
    public void run() {

        if (project.vaadin.widgetset == null) {
            project.logger.error("No widgetset defined. Please define a widgetset by using the vaadin.widgetset property.")
            return
        }

        runDevelopmentMode()

        if (!project.vaadin.devmode.noserver) {
            server = new ApplicationServer(
                    project, ["gwt.codesvr=${project.vaadin.devmode.bindAddress}:${project.vaadin.devmode.codeServerPort}"]
            ).startAndBlock()
            devModeProcess.waitForOrKill(1)
        } else {
            devModeProcess.waitFor()
        }
    }

    protected void runDevelopmentMode() {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        def classpath = Util.getClassPath(project)

        if(project.vaadin.gwt.gwtSdkFirstInClasspath){
            FileCollection gwtCompilerClasspath = project.configurations[DependencyListener.Configuration.CLIENT.caption];
            classpath = gwtCompilerClasspath + classpath.minus(gwtCompilerClasspath);
        }

        def devmodeProcess = ['java',
            '-cp', classpath.getAsPath(),
            'com.google.gwt.dev.DevMode',
            project.vaadin.widgetset,
            '-noserver',
            '-war', webAppDir.canonicalPath + '/VAADIN/widgetsets',
            '-gen', 'build/devmode/gen',
            '-startupUrl', "http://localhost:${project.vaadin.serverPort}",
            '-logLevel', project.vaadin.gwt.logLevel,
            '-deploy', 'build/devmode/deploy',
            '-workDir', 'build/devmode/',
            '-logdir', 'build/devmode/logs',
            '-codeServerPort', project.vaadin.devmode.codeServerPort,
            '-bindAddress', project.vaadin.devmode.bindAddress
        ]

        devModeProcess = devmodeProcess.execute()

        Util.logProcess(project, devModeProcess, 'devmode.log')
    }
}