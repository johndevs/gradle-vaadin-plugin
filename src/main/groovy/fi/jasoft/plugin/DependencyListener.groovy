package fi.jasoft.plugin;

import org.gradle.api.ProjectEvaluationListener;
import org.gradle.api.Project;
import org.gradle.api.ProjectState;

class DependencyListener implements ProjectEvaluationListener{
	
	void beforeEvaluate(Project project){
		
	}

	void afterEvaluate(Project project, ProjectState state){
		project.repositories.mavenCentral()
		project.repositories.mavenRepo(name: 'Vaadin addons', url: 'http://maven.vaadin.com/vaadin-addons')
		project.repositories.mavenRepo(name: 'Jasoft.fi Maven repository', url: 'http://mvn.jasoft.fi/maven2')

		project.dependencies.add('providedCompile', 'fi.jasoft.plugin:VaadinPlugin:0.0.2')

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