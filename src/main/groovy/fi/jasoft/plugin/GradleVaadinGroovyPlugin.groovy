package fi.jasoft.plugin

import fi.jasoft.plugin.configuration.VaadinPluginGroovyExtension
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin

/**
 * Created by john on 7/20/14.
 */
class GradleVaadinGroovyPlugin extends GradleVaadinPlugin {

    @Override
    void apply(Project project) {
        super.apply(project)

        // Extensions
        project.extensions.create('vaadin-groovy', VaadinPluginGroovyExtension)

        // Dependencies
        project.gradle.addProjectEvaluationListener(new GroovyDependencyListener())
        project.gradle.taskGraph.addTaskExecutionListener(new GroovyTaskListener(project))

        // Plugins
        project.plugins.apply(GroovyPlugin)
    }
}
