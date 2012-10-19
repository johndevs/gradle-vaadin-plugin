package fi.jasoft.plugin;

import org.gradle.api.tasks.JavaExec
import org.gradle.api.plugins.jetty.JettyRun
import org.gradle.api.plugins.jetty.JettyStop
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention

class SuperDevModeTask extends JavaExec  {

    public SuperDevModeTask(){
        dependsOn(project.tasks.widgetset)
    }

	 @Override
    public void exec(){
    	
        if(!project.vaadin.superDevModeEnabled){
            println "SuperDevMode is not enabled for project, please enable it by setting vaadin.superDevModeEnabled to true"
        }

    	File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

    	File widgetsetsDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')
    	widgetsetsDir.mkdirs()

    	String widgetset = project.vaadin.widgetset == null ? 'com.vaadin.terminal.gwt.DefaultWidgetSet' : project.vaadin.widgetset

    	setMain('com.google.gwt.dev.codeserver.CodeServer')

    	setClasspath(getClassPath())

    	setArgs([
    			'-port', 9876,
    			'-workDir', widgetsetsDir.canonicalPath,
    			'-src', 'src/main/java',
    			widgetset ])

    	jvmArgs('-Dgwt.compiler.skip=true')

    	JettyRun jetty = project.jettyRun;
    	jetty.classpath = getClassPath()
    	jetty.stopKey = 'STOP'
    	jetty.stopPort = 8181
    	jetty.daemon = true
    	jetty.execute()

    	super.exec()

    	JettyStop jettyStop = project.jettyStop
    	jettyStop.stopKey = 'STOP'
    	jettyStop.stopPort = 8181
    	jettyStop.stop()

    }

     private FileCollection getClassPath(){

        FileCollection classpath = project.files(
            project.sourceSets.main.runtimeClasspath,
            project.configurations.compile.asPath, 
            project.configurations.providedCompile.asPath,
            project.configurations.runtime.asPath)

        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it)
        }
        return classpath   
    }
}