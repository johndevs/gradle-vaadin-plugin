package fi.jasoft.plugin

import fi.jasoft.plugin.configuration.VaadinPluginExtension
import fi.jasoft.plugin.configuration.VaadinPluginGroovyExtension
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.WarPlugin

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
        project.gradle.taskGraph.addTaskExecutionListener(new GroovyTaskListener(project))

        def configurations = project.configurations
        def projectDependencies = project.dependencies
        configurations.create('vaadin-groovy', { conf ->
            conf.description = 'Libraries needed to use Groovy with Vaadin'
            conf.defaultDependencies { dependencies ->
                def groovy = projectDependencies.create('org.codehaus.groovy:groovy-all:2.3.4')
                dependencies.add(groovy)
            }
        })

        // Plugins
        project.plugins.apply(GroovyPlugin)


    }
}
