package fi.jasoft.plugin;

import org.gradle.api.plugins.WarPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project;
import fi.jasoft.plugin.DependencyListener;

class VaadinPlugin implements Plugin<Project>{

	void apply(Project project){
		project.plugins.apply(WarPlugin)
		project.extensions.create('vaadin', VaadinPluginExtension)
		project.tasks.add(name: 'createVaadinProject', type: CreateProjectTask, group: 'Vaadin')
		project.tasks.add(name: 'widgetset', type: CompileWidgetsetTask, group: 'Vaadin')
		project.tasks.add(name: 'devmode', type: DevModeTask, group: 'Vaadin')
		project.getGradle().addProjectEvaluationListener(new DependencyListener());
	}
}
