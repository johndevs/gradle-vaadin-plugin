package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState

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

        module.scopes.COMPILE.plus += [conf['vaadin-groovy']]
    }
}
