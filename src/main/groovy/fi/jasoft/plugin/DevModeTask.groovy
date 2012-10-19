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
package fi.jasoft.plugin;

import org.gradle.api.tasks.JavaExec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.WarPluginConvention;

class DevModeTask extends JavaExec  {
	
    public DevModeTask(){
        dependsOn(project.tasks.classes)
        description = "Run Development Mode for easier debugging and development of client widgets."
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

        println "Vaadin Application is running on " + project.vaadin.devModeStartupUrl

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