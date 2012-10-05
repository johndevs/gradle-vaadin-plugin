package fi.jasoft.plugin;

import org.gradle.api.tasks.JavaExec;
import org.gradle.api.plugins.jetty.JettyRun;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPluginConvention;

class DevModeTask extends JavaExec  {
	
	 @Override
    public void exec(){
    	dependsOn(JettyRun)

    	FileCollection classpath = project.files(project.sourceSets.main.runtimeClasspath)
    	classpath = project.files(classpath, project.configurations.compile.asPath)
        classpath = project.files(classpath, project.configurations.providedCompile.asPath)
        project.sourceSets.main.java.srcDirs.each{
            classpath = project.files(classpath, it)
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        JettyRun jetty = project.tasks.jettyRun
    	jetty.daemon = true
    	jetty.contextPath = '/'
    	jetty.classpath = classpath
    	jetty.webAppSourceDirectory = webAppDir

    	project.tasks.jettyRun.execute()

    	setMain('com.google.gwt.dev.DevMode')
        setClasspath(classpath)
        setArgs([project.vaadin.widgetset, '-war', webAppDir.canonicalPath+'/VAADIN/widgetsets', '-gen', 'build/gen', '-startupUrl', project.vaadin.startupUrl])

    	super.exec()

    	project.tasks.jettyStop.execute()
	}
}