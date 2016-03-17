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
import fi.jasoft.plugin.configuration.ApplicationServerConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

class RunTask extends DefaultTask {

    public static final String NAME = 'vaadinRun'

    def server

    @Option(option = 'stopAfterStart', description = 'Should the server stop after starting')
    def boolean stopAfterStarting = false

    @Option(option = 'nobrowser', description = 'Do not open browser after server has started')
    def boolean nobrowser = false

    def ApplicationServerConfiguration configuration

    def cleanupThread = new Thread({
        if(server){
            server.terminate()
            server = null
        }

        try {
            Runtime.getRuntime().removeShutdownHook(cleanupThread)
        } catch(IllegalStateException e){
            // Shutdown of the JVM in progress already, we don't need to remove the hook it will be removed by the JVM
        }
    })

    public RunTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        dependsOn(CompileThemeTask.NAME)
        description = 'Runs the Vaadin application on an embedded Jetty ApplicationServer'
        Runtime.getRuntime().addShutdownHook(cleanupThread)
        configuration = extensions.create('configuration', ApplicationServerConfiguration)
    }

    @TaskAction
    public void run() {
        if(nobrowser){
            configuration.openInBrowser = false
        }
        server = ApplicationServer.create(project, [], configuration)
        server.startAndBlock(stopAfterStarting)
    }
}