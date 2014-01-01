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
package fi.jasoft.plugin;


import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.io.FileOutputStream;
import org.gradle.api.plugins.WarPluginConvention

public class ApplicationServer {

    private appServerProcess;

    private final project;

    ApplicationServer(Project project) {
        this.project = project;
    }

    public start() {

        if (appServerProcess != null) {
            project.logger.error('Server is already running.')
            return
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = project.configurations.jetty8 + Util.getClassPath(project)

        File logDir = project.file('build/jetty/')
        logDir.mkdirs()

        appServerProcess = ['java']

        // Debug
        if (project.vaadin.debug) {
            appServerProcess.add('-Xdebug')
            appServerProcess.add("-Xrunjdwp:transport=dt_socket,address=${project.vaadin.debugPort},server=y,suspend=n")
        }

        // Jrebel
        if (project.vaadin.jrebel.enabled && project.vaadin.debug) {
            if (project.vaadin.jrebel.location != null && new File(project.vaadin.jrebel.location).exists()) {
                appServerProcess.add('-noverify')
                appServerProcess.add("-javaagent:${project.vaadin.jrebel.location}")
            } else {
                project.logger.warn('jrebel.jar not found, running without jrebel')
            }
        }

        // JVM options
        if (project.vaadin.debug) {
            appServerProcess.add('-ea')
        }

        appServerProcess.add('-cp')
        appServerProcess.add(cp.getAsPath())

        if (project.vaadin.jvmArgs != null) {
            appServerProcess.addAll(project.vaadin.jvmArgs)
        }

        // Program args
        appServerProcess.add('fi.jasoft.plugin.server.ApplicationServerRunner')

        appServerProcess.add(project.vaadin.serverPort)

        appServerProcess.add(webAppDir.canonicalPath + '/')

        File classesDir = project.file("build/classes")
        appServerProcess.add(classesDir.canonicalPath + '/')

        // Execute server
        appServerProcess = appServerProcess.execute()

        if (project.vaadin.plugin.logToConsole) {
            appServerProcess.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/jetty8-devMode.log')
            appServerProcess.consumeProcessOutputStream(new FileOutputStream(log))
        }

        def resultStr = "Application running on http://0.0.0.0:${project.vaadin.serverPort} "
        if (project.vaadin.jrebel.enabled) {
            resultStr += "(debugger on ${project.vaadin.debugPort}, JRebel active)"
        } else if (project.vaadin.debug) {
            resultStr += "(debugger on ${project.vaadin.debugPort})"
        } else {
            resultStr += '(debugger off)'
        }
        project.logger.lifecycle(resultStr)
    }


    public startAndBlock() {
        start()
        project.logger.lifecycle('Press [Ctrl+C] to terminate server...')
        appServerProcess.waitFor()
        terminate()
    }

    public terminate() {
        appServerProcess.in.close()
        appServerProcess.out.close()
        appServerProcess.err.close()
        appServerProcess.destroy()
        appServerProcess = null;

        project.logger.lifecycle("Application server terminated.")
    }
}
