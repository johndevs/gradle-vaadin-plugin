package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.War

/**
 * Created by john on 7/22/14.
 */
class GroovyTaskListener implements TaskExecutionListener {

    private final Project project

    public GroovyTaskListener(Project project) {
        this.project = project
    }

    @Override
    void beforeExecute(Task task) {
        if (project != task.getProject() || !project.hasProperty('vaadin')) {
            return
        }

        /*
         * Dependency related configurations
         */
        if (project.vaadin.manageDependencies) {

            if(task.getName() == 'ideaModule'){
                configureIdeaModule(task)
            }

            if (task.getName() == 'war') {
                War war = (War) task;

                // Add groovy libs to war
                project.war.classpath += project.configurations[DependencyListener.Configuration.GROOVY.caption]

                // Ensure no duplicates
                project.war.classpath = war.classpath.files
            }

            if (task.getName() == 'eclipseClasspath') {
                configureEclipsePlugin(task)
            }
        }
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {

    }

    /**
     * Configure the idea module
     */
    def configureIdeaModule(Task task) {

        def conf = project.configurations
        def module = project.idea.module

        module.scopes.COMPILE.plus += [conf[DependencyListener.Configuration.GROOVY.caption]]
    }

    def configureEclipsePlugin(Task task) {
        def cp = project.eclipse.classpath
        def conf = project.configurations

        // Add dependencies to eclipse classpath
        cp.plusConfigurations += [conf[DependencyListener.Configuration.GROOVY.caption]]
    }
}
