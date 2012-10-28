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
		project.repositories.mavenCentral()
		project.repositories.mavenRepo(name: 'Vaadin addons', url: 'http://maven.vaadin.com/vaadin-addons')
		project.repositories.mavenRepo(name: 'Jasoft.fi Maven repository', url: 'http://mvn.jasoft.fi/maven2')

		project.configurations.add('vaadinPlugin')
		project.dependencies.add('vaadinPlugin', 'fi.jasoft.plugin:VaadinPlugin:0.0.2')

		def jettyVersion = "8.1.5.v20120716"
		project.configurations.add("jetty8")
		project.dependencies.add('jetty8', "org.mortbay.jetty:jetty-runner:$jettyVersion")

		def version = project.vaadin.version;
		if(version.startsWith("6")){
			println "Building a Vaadin 6.x project"
			project.dependencies.add("compile", "com.vaadin:vaadin:"+version)
			if(project.vaadin.widgetset != null){
				project.dependencies.add("providedCompile", "com.google.gwt:gwt-user:2.3.0")
				project.dependencies.add("providedCompile", "com.google.gwt:gwt-dev:2.3.0")
				project.dependencies.add("providedCompile",	"javax.validation:validation-api:1.0.0.GA")
				project.dependencies.add("providedCompile",	"javax.validation:validation-api:1.0.0.GA:sources")
			}
		} else{ 
			println "Building a Vaadin 7.x project"

			project.dependencies.add("compile", "com.vaadin:vaadin-server:"+version)
			project.dependencies.add("runtime",	"com.vaadin:vaadin-themes:"+version)

			File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
    		FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
			if(!themes.isEmpty()){
				project.dependencies.add("runtime",	"com.vaadin:vaadin-theme-compiler:"+version)	
			}

			if(project.vaadin.widgetset == null){
				project.dependencies.add("runtime",	"com.vaadin:vaadin-client-compiled:"+version)
			} else {
				project.dependencies.add("providedCompile",	"com.vaadin:vaadin-client-compiler:"+version)
				project.dependencies.add("providedCompile",	"com.vaadin:vaadin-client:"+version)
				project.dependencies.add("providedCompile",	"javax.validation:validation-api:1.0.0.GA")
				project.dependencies.add("providedCompile",	"javax.validation:validation-api:1.0.0.GA:sources")

				// For devmode
				project.dependencies.add("providedCompile", "javax.servlet:servlet-api:"+project.vaadin.servletVersion)
				project.dependencies.add("providedCompile", "jspapi:jsp-api:2.0")
			}


		}

	}
}