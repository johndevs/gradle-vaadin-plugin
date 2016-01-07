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
import org.gradle.api.GradleException
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class SuperDevModeTask extends DefaultTask {

    static final String NAME = 'vaadinSuperDevMode'

    def Process codeserverProcess = null

    def server = null

    def SuperDevModeTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        description = "Run Super Development Mode for easier client widget development."


        addShutdownHook {
           if(codeserverProcess){
               codeserverProcess.destroy()
               codeserverProcess = null
           }

            if(server) {
                server.terminate()
                server = null
            }
        }
    }

    @TaskAction
    def run() {
        if(!project.vaadin.widgetset) {
            logger.error 'No widgetset defined (can be set with vaadin.widgetset in build.gradle)'
            throw new GradleException("Property vaadin.widgetset not set.")
        }

        runCodeServer({

            server = ApplicationServer.create(project, ['superdevmode'])

            server.startAndBlock();

            codeserverProcess.waitForOrKill(1)
        })
    }

    def runCodeServer(Closure readyClosure) {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetsDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        widgetsetsDir.mkdirs()

        def SDMClassPath = project.configurations['vaadin-superdevmode'];
        def classpath = SDMClassPath + Util.getClientCompilerClassPath(project)

        def superdevmodeProcess = ['java']
        superdevmodeProcess += ['-cp', classpath.getAsPath()]
        superdevmodeProcess += 'com.google.gwt.dev.codeserver.CodeServer'
        superdevmodeProcess += ['-bindAddress', project.vaadin.devmode.bindAddress]
        superdevmodeProcess += ['-port', 9876]
        superdevmodeProcess += ['-workDir', widgetsetsDir.canonicalPath]
        superdevmodeProcess += ['-src', javaDir.canonicalPath]
        superdevmodeProcess += ['-logLevel', project.vaadin.gwt.logLevel]
        superdevmodeProcess += ['-noprecompile']

        if (project.vaadin.devmode.extraArgs) {
            superdevmodeProcess += project.vaadin.devmode.extraArgs as List
        }

        superdevmodeProcess += project.vaadin.widgetset

        codeserverProcess = superdevmodeProcess.execute()

        Util.logProcess(project, codeserverProcess, 'superdevmode.log', { line ->
            if(line.contains('The code server is ready')){
                readyClosure.call()
            }
        })

        codeserverProcess.waitFor()
    }
}