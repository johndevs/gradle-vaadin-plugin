package fi.jasoft.plugin;


import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.io.FileOutputStream;
import org.gradle.api.plugins.WarPluginConvention

public class ApplicationServer {

    private appServerProcess;

    private final project;

    ApplicationServer(Project project){
           this.project = project;
    }

    public start() {

        if(appServerProcess != null){
            throw new IllegalStateException("Server already running.")
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileCollection cp = project.configurations.jetty8 + Util.getClassPath(project)

        File logDir = new File('build/jetty/')
        logDir.mkdirs()

        appServerProcess = ['java']

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
        appServerProcess.add('fi.jasoft.plugin.server.ApplicationServerRunner')

        appServerProcess.add(project.vaadin.serverPort)

        appServerProcess.add(webAppDir.canonicalPath+'/')

        print "Application running on http://0.0.0.0:${project.vaadin.serverPort} "

        if(project.vaadin.jrebel.enabled){
            println "(debugger on ${project.vaadin.debugPort}, JRebel active)"
        } else {
            println "(debugger on ${project.vaadin.debugPort})"
        }

        // Execute server
        appServerProcess = appServerProcess.execute()

        if(project.vaadin.plugin.logToConsole){
            appServerProcess.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/jetty8-devMode.log')
            appServerProcess.consumeProcessOutputStream(new FileOutputStream(log))
        }
    }

    public startAndBlock(){
        start()
        appServerProcess.waitFor()
        terminate()
    }

    public terminate(){
        appServerProcess.in.close()
        appServerProcess.out.close()
        appServerProcess.err.close()
        appServerProcess.destroy()
        appServerProcess = null;
    }
}
