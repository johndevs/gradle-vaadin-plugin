/*
* Copyright 2012 John Ahlroos
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
package fi.jasoft.plugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.plugins.WarPluginConvention;

public class RunTask extends DefaultTask {

    public RunTask(){
        dependsOn(project.tasks.widgetset)
        dependsOn(project.tasks.themes)
        description = 'Runs the Vaadin application on an embedded Jetty Server'
    }

    @TaskAction
    public void run() {       

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        project.javaexec{

            setMain('org.mortbay.jetty.runner.Runner')

            setClasspath(project.configurations.jetty8 + 
                project.configurations.providedCompile + 
                project.configurations.compile +
                project.sourceSets.main.runtimeClasspath +
                project.sourceSets.main.compileClasspath)

            setArgs([webAppDir.canonicalPath])

            jvmArgs([
                "-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n", 
                '-Xdebug'])
        }
    }
}