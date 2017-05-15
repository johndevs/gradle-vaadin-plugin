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

import com.devsoap.plugin.configuration.ApplicationServerConfiguration
import com.devsoap.plugin.configuration.SuperDevModeConfiguration
import com.devsoap.plugin.servers.ApplicationServer
import com.devsoap.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

/**
 * Runs the GWT codeserver as well as runs the application in SuperDevMode mode.
 *
 * @author John Ahlroos
 */
class SuperDevModeTask extends DefaultTask {

    static final String NAME = 'vaadinSuperDevMode'

    def Process codeserverProcess = null

    def ApplicationServer server = null

    def cleanupThread = new Thread({
        if ( codeserverProcess ) {
            codeserverProcess.destroy()
            codeserverProcess = null
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

    SuperDevModeTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        description = "Run Super Development Mode for easier client widget development."
        Runtime.getRuntime().addShutdownHook(cleanupThread)
    }

    @TaskAction
    def run() {
        if ( !Util.getWidgetset(project) ) {
            throw new GradleException("No widgetset found in project.")
        }

        def configuration = Util.findOrCreateExtension(project, SuperDevModeConfiguration)
        if(configuration.noserver){
            runCodeServer {
                codeserverProcess.waitForOrKill(1)
            }
        } else {
            runCodeServer({
                def serverConf = Util.findOrCreateExtension(project, ApplicationServerConfiguration)
                server = ApplicationServer.get(project,
                        [superdevmode:"$configuration.bindAddress:$configuration.codeServerPort"],
                        serverConf)
                server.startAndBlock()
                codeserverProcess.waitForOrKill(1)
            })
        }
    }

    def runCodeServer(Closure readyClosure) {
        def configuration = Util.findOrCreateExtension(project, SuperDevModeConfiguration)
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetsDir = Util.getWidgetsetDirectory(project)
        widgetsetsDir.mkdirs()

        FileCollection classpath = Util.getClientCompilerClassPath(project)

        List superdevmodeProcess = [Util.getJavaBinary(project)]
        superdevmodeProcess += ['-cp', classpath.asPath]
        superdevmodeProcess += 'com.google.gwt.dev.codeserver.CodeServer'
        superdevmodeProcess += ['-bindAddress', configuration.bindAddress]
        superdevmodeProcess += ['-port', configuration.codeServerPort ]
        superdevmodeProcess += ['-workDir', widgetsetsDir.canonicalPath]
        superdevmodeProcess += ['-src', javaDir.canonicalPath]
        superdevmodeProcess += ['-logLevel', configuration.logLevel]
        superdevmodeProcess += ['-noprecompile']

        if ( configuration.extraArgs ) {
            superdevmodeProcess += configuration.extraArgs as List
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
}
