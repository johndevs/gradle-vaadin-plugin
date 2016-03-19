package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.War

/**
 * Created by john on 7/22/14.
 */
class GroovyTaskListener implements TaskExecutionListener {

    public static final String GROOVY_CONFIGURATION_NAME = 'vaadin-groovy'
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
                project.war.classpath += project.configurations[GROOVY_CONFIGURATION_NAME]

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

        def conf = task.project.configurations
        def module = task.project.idea.module

        module.scopes.COMPILE.plus += [conf[GROOVY_CONFIGURATION_NAME]]
    }

    def configureEclipsePlugin(Task task) {
        def cp = task.project.eclipse.classpath
        def conf = task.project.configurations

        // Add dependencies to eclipse classpath
        cp.plusConfigurations += [conf[GROOVY_CONFIGURATION_NAME]]
    }
}
