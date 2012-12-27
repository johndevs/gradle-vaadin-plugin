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

import org.gradle.api.ProjectEvaluationListener;
import org.gradle.api.Project;
import org.gradle.api.ProjectState;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.file.FileTree;

class DependencyListener implements ProjectEvaluationListener{
	
	void beforeEvaluate(Project project){		
	
	}

	void afterEvaluate(Project project, ProjectState state){

		if(!project.hasProperty('vaadin') || !project.vaadin.manageDependencies){
			return
		}

		// Repositories 
		project.repositories.mavenCentral()

		if(project.repositories.findByName('Vaadin addons') == null) {
			project.repositories.mavenRepo(name: 'Vaadin addons', url: 'http://maven.vaadin.com/vaadin-addons')
		}

		if(project.repositories.findByName('Jasoft.fi Maven repository') == null) {
			project.repositories.mavenRepo(name: 'Jasoft.fi Maven repository', url: 'http://mvn.jasoft.fi/maven2')
		}
		
		// Configurations
		if(!project.configurations.hasProperty('vaadin')){
			project.configurations.add('vaadin')
			project.sourceSets.main.compileClasspath += project.configurations.vaadin
			project.sourceSets.test.compileClasspath += project.configurations.vaadin
			project.sourceSets.test.runtimeClasspath += project.configurations.vaadin
			project.war.classpath(project.configurations.vaadin)	
		}

		if(!project.configurations.hasProperty('gwt')){
			project.configurations.add('gwt')
			project.sourceSets.main.compileClasspath += project.configurations.gwt
			project.sourceSets.test.compileClasspath += project.configurations.gwt
			project.sourceSets.test.runtimeClasspath += project.configurations.gwt
		}

		if(!project.configurations.hasProperty('jetty8')){
			project.configurations.add("jetty8")	
		}
		
		// Tasks
		def jettyVersion = "8.1.5.v20120716"	
		project.dependencies.add('jetty8', "org.mortbay.jetty:jetty-runner:$jettyVersion")

		def version = project.vaadin.version;
		if(version.startsWith("6")){
			println "Building a Vaadin 6.x project"
			project.dependencies.add("vaadin", "com.vaadin:vaadin:"+version)
			if(project.vaadin.widgetset != null){
				project.dependencies.add("gwt", "com.google.gwt:gwt-user:2.3.0")
				project.dependencies.add("gwt", "com.google.gwt:gwt-dev:2.3.0")
				project.dependencies.add("gwt",	"javax.validation:validation-api:1.0.0.GA")
				project.dependencies.add("gwt",	"javax.validation:validation-api:1.0.0.GA:sources")
			}
		} else{ 
			println "Building a Vaadin 7.x project"


			File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
    		FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
			if(!themes.isEmpty()){
				project.dependencies.add("vaadin",	"com.vaadin:vaadin-theme-compiler:"+version)	
			}

			if(project.vaadin.widgetset == null){
				project.dependencies.add("vaadin",	"com.vaadin:vaadin-client-compiled:"+version)
			} else {
				project.dependencies.add("gwt",	"com.vaadin:vaadin-client-compiler:"+version)
				project.dependencies.add("gwt",	"com.vaadin:vaadin-client:"+version)
				project.dependencies.add("gwt",	"javax.validation:validation-api:1.0.0.GA")
				project.dependencies.add("gwt",	"javax.validation:validation-api:1.0.0.GA:sources")

				// For devmode
				project.dependencies.add("jetty8", "javax.servlet:servlet-api:"+project.vaadin.servletVersion)
				project.dependencies.add("jetty8", "jspapi:jsp-api:2.0")
			}

			project.dependencies.add("vaadin", "com.vaadin:vaadin-server:"+version)
			project.dependencies.add("vaadin", "com.vaadin:vaadin-themes:"+version)
		}
	}
}