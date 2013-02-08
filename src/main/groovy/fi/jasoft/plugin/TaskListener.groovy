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

import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState

public class TaskListener implements TaskExecutionListener{

	public void  beforeExecute(Task task){
		def project = task.getProject()
		if(!project.hasProperty('vaadin')){
			return
		}

		/*
		 * Dependency related configurations
		 */
		if(project.vaadin.manageDependencies){

			if(task.getName() == 'eclipseClasspath'){
				configureEclipsePlugin(task)
			} 

			if(task.getName() == 'eclipseWtpComponent'){
				configureEclipseWtpPlugin(task)
			}
		}

		if(task.getName() == 'compileJava'){
			ensureWidgetsetGeneratorExists(task)
		}
	}

	public void  afterExecute(Task task, TaskState state){
		
	}

	private void configureEclipsePlugin(Task task){
		def project = task.getProject()
		def cp = project.eclipse.classpath
		cp.defaultOutputDir = project.file('build/classes/main')
		cp.plusConfigurations += project.configurations.vaadin
		cp.plusConfigurations += project.configurations.gwt
	}

	private void configureEclipseWtpPlugin(Task task){
		def project = task.getProject()
		def wtp = project.eclipse.wtp.component
		wtp.plusConfigurations += project.configurations.vaadin		
	}

	private void ensureWidgetsetGeneratorExists(Task task){
		def project = task.getProject()
		if(project.vaadin.widgetsetGenerator != null ){
			String name = project.vaadin.widgetsetGenerator.tokenize('.').last()
            String pkg = project.vaadin.widgetsetGenerator.replaceAll('.'+ name,'')
            String filename = name + ".java"
			File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
    		File f = new File(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./,'/') + '/' + filename)
    		if(!f.exists()){
    			project.tasks.createVaadinWidgetsetGenerator.run()	
    		}
        }
	}
}