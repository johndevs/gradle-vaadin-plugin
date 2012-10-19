package fi.jasoft.plugin;

import org.gradle.api.Plugin
import org.gradle.api.Project
import fi.jasoft.plugin.DependencyListener
import org.gradle.api.plugins.jetty.JettyPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.compile.Compile

class VaadinPlugin implements Plugin<Project>{

	void apply(Project project){

		// Plugins
		project.plugins.apply(WarPlugin)
		project.plugins.apply(JettyPlugin)
		
		// Extensions
		project.extensions.create('vaadin', VaadinPluginExtension)
		
		// Tasks
		project.tasks.add(name: 'createVaadinProject', 	type: CreateProjectTask, 	group: 'Vaadin')	
		project.tasks.add(name: 'createVaadinComponent',type: CreateComponentTask,	group: 'Vaadin')

		project.tasks.add(name: 'widgetset', 			type: CompileWidgetsetTask, group: 'Vaadin')
		project.tasks.add(name: 'devmode', 				type: DevModeTask, 			group: 'Vaadin')
		project.tasks.add(name: 'superdevmode', 		type: SuperDevModeTask, 	group: 'Vaadin')

		// Dependency resolution
		project.getGradle().addProjectEvaluationListener(new DependencyListener());

		// Add debug information to all compilation results
		project.tasks.compileJava.options.debugOptions.debugLevel = 'source,lines,vars'

		//Ensure eclipse plugin has the right classes dir
		if (project.plugins.hasPlugin('eclipse')) {
			project.plugins.eclipse.classpath.defaultOutputDir = project.file('build/classes/main')
		} 
	}
}
