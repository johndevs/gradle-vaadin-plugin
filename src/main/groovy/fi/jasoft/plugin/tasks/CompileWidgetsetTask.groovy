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
import org.gradle.api.tasks.JavaExec;
import fi.jasoft.plugin.VaadinPlugin;
import fi.jasoft.plugin.TemplateUtil;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.file.FileCollection;


class CompileWidgetsetTask extends DefaultTask {
   
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

        // Monitor changes in client side classes and resources
        project.sourceSets.main.java.srcDirs.each{
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }  

        //Monitor changes in resources
        project.sourceSets.main.resources.srcDirs.each{
           getInputs().files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*')) 
           getInputs().files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }
    }

    @TaskAction
    public void run() {
    	if(project.vaadin.widgetset == null){
    		return;	
    	}

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File targetDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')

        // Ensure unit cache dir is present so the compiler does not complain
        new File(targetDir.canonicalPath+'/VAADIN/gwt-unitCache').mkdirs()

    	// Create a widgetset if needed
    	TemplateUtil.ensureWidgetPresent(project)

        FileCollection classpath = getClassPath()
        
        project.javaexec{
            setClasspath(classpath)
            setMain('com.google.gwt.dev.Compiler')

            def args = ['-style', project.vaadin.gwt.style] +
                       ['-optimize', project.vaadin.gwt.optimize] +
                       ['-war', targetDir.canonicalPath] +
                       ['-logLevel', project.vaadin.gwt.logLevel] +
                       ['-localWorkers', project.vaadin.gwt.localWorkers]

            if(project.vaadin.gwt.draftCompile){
                args.add('-draftCompile')
            }

            if(project.vaadin.gwt.strict){
                args.add('-strict')
            }

            args.add(project.vaadin.widgetset)

            setArgs(args)
        }

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
         new File(targetDir.canonicalPath+"/WEB-INF").deleteDir()
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
