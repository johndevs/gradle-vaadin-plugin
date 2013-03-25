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
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPluginConvention;
import fi.jasoft.plugin.TemplateUtil
import java.lang.Process;

class DevModeTask extends DefaultTask  {

    public DevModeTask(){
        dependsOn(project.tasks.classes)
        description = "Run Development Mode for easier debugging and development of client widgets."
    }

	@TaskAction
    public void run() {  

        if(project.vaadin.widgetset == null){
            println "No widgetset defined. Please define a widgetset by using the vaadin.widgetset property."
            return
        }

        if(project.vaadin.devmode.noserver){
            runDevelopmentMode()
        } else {
            ApplicationServer server = new ApplicationServer(project)

            server.start()

            runDevelopmentMode()

            server.terminate()
        }
	}

    protected void runDevelopmentMode(){
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        def classpath = getClassPath()

        project.javaexec{
            setMain('com.google.gwt.dev.DevMode')
            setClasspath(classpath)
            setArgs([project.vaadin.widgetset, 
                    '-noserver',
                    '-war',             webAppDir.canonicalPath+'/VAADIN/widgetsets', 
                    '-gen',             'build/devmode/gen', 
                    '-startupUrl',      'http://localhost:8080', 
                    '-logLevel',        project.vaadin.gwt.logLevel,
                    '-deploy',          'build/devmode/deploy',
                    '-workDir',         'build/devmode/',
                    '-logdir',          'build/devmode/logs',
                    '-codeServerPort',  project.vaadin.devmode.codeServerPort,
                    '-bindAddress',     project.vaadin.devmode.bindAddress
            ])
        }
    }

    private FileCollection getClassPath(){
        FileCollection classpath = 
            project.configurations.providedCompile + 
            project.configurations.compile +
            project.configurations.vaadinSources +
            project.configurations.gwtSources +
            project.sourceSets.main.runtimeClasspath +
            project.sourceSets.main.compileClasspath

        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it)
        }
        return classpath   
    }
}