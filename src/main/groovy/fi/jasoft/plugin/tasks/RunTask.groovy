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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.file.FileCollection
import fi.jasoft.plugin.Util

public class RunTask extends DefaultTask {

    public RunTask(){
        dependsOn(project.tasks.widgetset)
        dependsOn(project.tasks.themes)
        description = 'Runs the Vaadin application on an embedded Jetty Server'
    }

    @TaskAction
    public void run() {       
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = project.configurations.jetty8 + 
                    project.configurations.providedCompile + 
                    project.configurations.compile +
                    project.sourceSets.main.runtimeClasspath +
                    project.sourceSets.main.compileClasspath

        File logDir = new File('build/jetty/')
        logDir.mkdirs()

        def appServerProcess = ['java']
        
        // Debug
        appServerProcess.add('-Xdebug')
        appServerProcess.add("-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n")

        // Jrebel
        if(project.vaadin.jrebel.enabled){
            if(project.vaadin.jrebel.location != null && new File(project.vaadin.jrebel.location).exists()){
                appServerProcess.add('-noverify')
                appServerProcess.add("-javaagent:${project.vaadin.jrebel.location}")
            } else {
                println "Could not find jrebel.jar, aborting run."
                return;
            }
        }

        // JVM options
        appServerProcess.add('-cp')
        appServerProcess.add(cp.getAsPath())  
        
        if(project.vaadin.jvmArgs != null){
            appServerProcess.addAll(project.vaadin.jvmArgs)
        }      

        // Program args
        appServerProcess.add('org.mortbay.jetty.runner.Runner')

        appServerProcess.add('--port')
        appServerProcess.add(project.vaadin.serverPort)

        appServerProcess.add(webAppDir.canonicalPath)

        print "Application running on http://0.0.0.0:${project.vaadin.serverPort} "

        if(project.vaadin.jrebel.enabled){
            println "(debugger on ${project.vaadin.debugPort}, JRebel active)"
        } else {
            println "(debugger on ${project.vaadin.debugPort})"
        }

        if(project.vaadin.plugin.terminateOnEnter){
            println "Press [Enter] to stop server...";
        }

        // Excecute server
        appServerProcess = appServerProcess.execute()

        if(project.vaadin.plugin.logToConsole){
            appServerProcess.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/jetty8-vaadinRun.log')
            appServerProcess.consumeProcessOutputStream(new FileOutputStream(log))
        }

        if(project.vaadin.plugin.terminateOnEnter){
            // Wait for enter
            Util.readLine("")

            // Terminate server
            appServerProcess.in.close()
            appServerProcess.out.close()
            appServerProcess.err.close()
            appServerProcess.destroy()
            appServerProcess = null;

        } else {
            // Block
            appServerProcess.waitFor()
        }
    }
}