/*
* Copyright 2013 John Ahlroos
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

import fi.jasoft.plugin.ApplicationServer;
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.file.FileCollection
import fi.jasoft.plugin.Util

public class RunTask extends DefaultTask {

    public RunTask(){
        dependsOn(project.tasks.widgetset)
        dependsOn(project.tasks.themes)
        description = 'Runs the Vaadin application on an embedded Jetty ApplicationServer'
    }

    @TaskAction
    public void run() {
        ApplicationServer server = new ApplicationServer(project)
        if(project.vaadin.plugin.terminateOnEnter){
            server.start()
            Util.readLine("")        // Wait for enter
            server.terminate()
        } else {
            server.startAndBlock()
        }
    }
}