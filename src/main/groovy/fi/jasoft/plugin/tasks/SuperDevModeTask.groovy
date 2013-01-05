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
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import java.lang.Process
import fi.jasoft.plugin.TemplateUtil

class SuperDevModeTask extends DefaultTask  {

    private Process appServerProcess;

    public SuperDevModeTask(){
        dependsOn(project.tasks.widgetset)
        description = "Run Super Development Mode for easier client widget development."
    }

    @TaskAction
    public void run() {  
    	
        if(!project.vaadin.devmode.superDevMode){
            println "SuperDevMode is a experimental feature and is not enabled for project by default. To enable it set vaadin.superDevModeEnabled to true"
            return;
        }

        TemplateUtil.ensureWidgetPresent(project)

    	File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()

    	File widgetsetsDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')
    	widgetsetsDir.mkdirs()

    	String widgetset = project.vaadin.widgetset == null ? 'com.vaadin.terminal.gwt.DefaultWidgetSet' : project.vaadin.widgetset

        launchApplicationServer()

        def classpath = getClassPath()

        project.javaexec{
            setMain('com.google.gwt.dev.codeserver.CodeServer')
            setClasspath(classpath)
            setArgs([
                '-port', 9876,
                '-workDir', widgetsetsDir.canonicalPath,
                '-src', javaDir.canonicalPath,
                widgetset ])
            jvmArgs('-Dgwt.compiler.skip=true')
        }

        terminateApplicationServer()
    }

    protected void launchApplicationServer(){
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = project.configurations.jetty8 + 
                    project.configurations.providedCompile + 
                    project.configurations.compile +
                    project.sourceSets.main.runtimeClasspath +
                    project.sourceSets.main.compileClasspath

        appServerProcess = ['java', 
            "-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n",
            '-Xdebug',
            '-cp', cp.getAsPath(), 
            'org.mortbay.jetty.runner.Runner', 
            webAppDir.canonicalPath].execute()
       
        println "Application running on http://localhost:8080 (debugger on ${project.vaadin.debugPort})"
    }

    protected void terminateApplicationServer(){
        appServerProcess.in.close()
        appServerProcess.out.close()
        appServerProcess.err.close()
        appServerProcess.destroy()
        appServerProcess = null;
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