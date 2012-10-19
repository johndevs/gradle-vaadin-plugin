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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.JavaExec;
import fi.jasoft.plugin.VaadinPlugin;
import fi.jasoft.plugin.ui.TemplateUtil;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.file.FileCollection;


class CompileWidgetsetTask extends JavaExec {
   
    public CompileWidgetsetTask(){
        dependsOn(project.tasks.classes)
        description = "Compiles Vaadin Addons and components into Javascript."

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File targetDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')
        getOutputs().dir(targetDir)

        /* Monitor changes in dependencies since upgrading a
        * dependency should also trigger a recompile of the widgetset
        */
        getInputs().files(project.configurations.compile)       

        // Monitor changes in client side classes
        project.sourceSets.main.java.srcDirs.each{
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
        }  
    }

    @Override
    public void exec(){
    	if(project.vaadin.widgetset == null){
    		println "No widgetset defined. Please add a widgetset to your Vaadin configuration."
    		return;	
    	}

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File targetDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')

    	// Create a widgetset if needed
    	TemplateUtil.ensureWidgetPresent(project)
    	
        FileCollection classpath = project.files(classpath, project.configurations.compile.asPath) 
        classpath += project.files(project.sourceSets.main.runtimeClasspath.asPath)
        project.sourceSets.main.java.srcDirs.each{
            classpath += project.files(it.absolutePath)
        }

        setClasspath(classpath)
        setMain('com.google.gwt.dev.Compiler')
        setArgs(['-style', project.vaadin.gwtStyle, '-optimize', project.vaadin.gwtOptimize, '-war', targetDir.canonicalPath, project.vaadin.widgetset, '-logLevel', project.vaadin.gwtLogLevel])
        
        super.exec();

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
         new File(targetDir.canonicalPath+"/WEB-INF").deleteDir()
    }
    
}
