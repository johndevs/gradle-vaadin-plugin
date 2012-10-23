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
import fi.jasoft.plugin.ui.TemplateUtil

class DevModeTask extends JavaExec  {
	
    public DevModeTask(){
        dependsOn(project.tasks.classes)
        description = "Run Development Mode for easier debugging and development of client widgets."
    }

	 @Override
    public void exec(){        

        if(project.vaadin.widgetset == null){
            println "No widgetset defined. Please define a widgetset by using the vaadin.widgetset property."
            return
        }

        // ensure the widgetset is up-2-date
        if(TemplateUtil.ensureWidgetPresent(project)){
            println "A new widgetset was just created for the project. You need to add it to web.xml before running devmode again."
            return
        }
        
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        String widgetset = project.vaadin.widgetset

    	setMain('com.google.gwt.dev.DevMode')
        
        setClasspath(getClassPath())

        setArgs([widgetset, 
            '-war',         webAppDir.canonicalPath+'/VAADIN/widgetsets', 
            '-gen',         'build/gen', 
            '-startupUrl',  'http://localhost:8080', 
            '-logLevel',    project.vaadin.gwtLogLevel,
            '-server',      'com.github.shyiko.gists.vaadin.DevModeJettyLauncher',
            '-port',        8080,
            '-bindAddress', '0.0.0.0'])

        jvmArgs([ 
            '-Ddev.mode.app.root='+webAppDir.canonicalPath, 
            "-Xrunjdwp:transport=dt_socket,address=${project.vaadin.devModeDebugPort},server=y,suspend=n", 
            '-Xdebug'])

        println "Vaadin Application is running on http://localhost:8080"

    	super.exec()
	}

    private FileCollection getClassPath(){

        FileCollection classpath = project.configurations.vaadinPlugin +
            project.configurations.providedCompile + 
            project.configurations.compile +
            project.sourceSets.main.runtimeClasspath

        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it)
        }
        return classpath   
    }
}