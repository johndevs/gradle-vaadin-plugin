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
package fi.jasoft.plugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPluginConvention;
import fi.jasoft.plugin.TemplateUtil
import java.lang.Process;

class DevModeTask extends DefaultTask  {
	
    private Process appServerProcess;

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

        // ensure the widgetset is up-2-date
        if(TemplateUtil.ensureWidgetPresent(project)){
            println "A new widgetset was just created for the project. You need to add it to web.xml before running devmode again."
            return
        }
        
        if(!project.vaadin.devmode.noserver){
             launchApplicationServer()     
        }

        runDevelopmentMode()

        if(!project.vaadin.devmode.noserver){
             terminateApplicationServer()     
        }
	}

    protected void launchApplicationServer(){
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = project.configurations.jetty8 + 
                    project.configurations.providedCompile + 
                    project.configurations.compile +
                    project.sourceSets.main.runtimeClasspath +
                    project.sourceSets.main.compileClasspath


        File logDir = new File('build/jetty/')
        logDir.mkdirs()            

        appServerProcess = ['java', 
            "-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n",
            '-Xdebug',
            '-cp', cp.getAsPath(), 
            'org.mortbay.jetty.runner.Runner', 
            '--port', project.vaadin.serverPort,
            '--out', logDir.canonicalPath + '/jetty8-devmode.log',
            '--log', logDir.canonicalPath + '/jetty8-devmode.log',
            webAppDir.canonicalPath
        ].execute()
       
        println "Application running on http://0.0.0.0:${project.vaadin.serverPort} (debugger on ${project.vaadin.debugPort})"
    }

    protected void terminateApplicationServer(){
        appServerProcess.in.close()
        appServerProcess.out.close()
        appServerProcess.err.close()
        appServerProcess.destroy()
        appServerProcess = null;
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
            project.sourceSets.main.runtimeClasspath +
            project.sourceSets.main.compileClasspath

        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it)
        }
        return classpath   
    }
}