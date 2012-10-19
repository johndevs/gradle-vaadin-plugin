package fi.jasoft.plugin;

import org.gradle.api.tasks.JavaExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPluginConvention;

class DevModeTask extends JavaExec  {
	
    public DevModeTask(){
        dependsOn(project.tasks.classes)
    }

	 @Override
    public void exec(){        
        
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        String widgetset = project.vaadin.widgetset == null ? 'com.vaadin.terminal.gwt.DefaultWidgetSet' : project.vaadin.widgetset

    	setMain('com.google.gwt.dev.DevMode')
        
        setClasspath(getClassPath())

        setArgs([widgetset, 
            '-war',         webAppDir.canonicalPath+'/VAADIN/widgetsets', 
            '-gen',         'build/gen', 
            '-startupUrl',  project.vaadin.devModeStartupUrl, 
            '-logLevel',    project.vaadin.gwtLogLevel,
            '-server',      'com.github.shyiko.gists.vaadin.DevModeJettyLauncher'])

        jvmArgs([ 
            '-Ddev.mode.app.root='+webAppDir.canonicalPath, 
            "-Xrunjdwp:transport=dt_socket,address=${project.vaadin.devModeDebugPort},server=y,suspend=n", 
            '-Xdebug'])

    	super.exec()
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