/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.actions

import com.devsoap.plugin.GradleVaadinPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskState

/**
 * Base class for reacting to other plugins
 *
 * @author John Ahlroos
 * @since 1.2
 */
abstract class PluginAction {

    private final TaskListener taskListener = new TaskListener()

    /**
     * The unique identifer for the plugin
     */
    abstract String getPluginId()

    /**
     * Executes the defined action when then Gradle Vaadin plugin is applied. This is only called
     * if the user has applied the plugin id for this plugin action.
     *
     * @param project
     *      the project to apply the action to
     */
    protected void execute(Project project) {
        project.logger.info("Applying ${getClass().simpleName} actions to project $project.name")
    }

    /**
     * This is called after project evaluation.
     *
     * @param project
     *      the project which was evaluated
     */
    protected void executeAfterEvaluate(Project project){
        project.logger.debug("Executing afterEvaluate hook for ${getClass().simpleName}")
    }

    /**
     * Called before a task is executed. This is only called if the user has applied the plugin id for
     * this plugin action.
     *
     * @param task
     *      task that will be executed
     */
    protected void beforeTaskExecuted(Task task) {
        task.project.logger.debug("Executing pre task hook for ${getClass().simpleName} for task $task.name")
    }

    /**
     * Called after a task has executed. This is only called if the user has applied the plugin id for
     * this plugin action.
     *
     * @param task
     *      task that was executed
     */
    protected void afterTaskExecuted(Task task) {
        task.project.logger.debug("Executing post task hook for ${getClass().simpleName} for task $task.name")
    }

    /**
     * Applies the plugin action to a project.
     *
     * Please note that by applying the action to a project only adds the support for the action to the project. You
     * also have to apply the plugin id the action is for to execute action.
     *
     * @param project
     *      the project to apply the plugin action to
     */
    void apply(Project project) {
        project.plugins.withId(pluginId) {
            project.gradle.taskGraph.removeTaskExecutionListener(taskListener)
            project.gradle.taskGraph.addTaskExecutionListener(taskListener)
            execute(project)
            project.afterEvaluate {
                executeAfterEvaluate(project)
            }
        }
    }

    /**
     * Returns the internal tasklistener
     */
    @Internal
    protected TaskListener getTaskListener() {
        taskListener
    }

    /**
     * Is the task applicable for the project. The task is applicable iff the Gradle Vaadin plugin is applied to the
     * project.
     *
     * @param task
     *      the task to check.
     *
     * @return
     *      <code>true</code> if the task is applicable to the project.
     */
    final static boolean isApplicable(Task task) {
        PluginManager pluginManager = task.project.pluginManager
        pluginManager.hasPlugin(GradleVaadinPlugin.pluginId)
    }

    private final class TaskListener implements TaskExecutionListener {

        @Override
        void beforeExecute(Task task) {
            if (isApplicable(task) ) {
                beforeTaskExecuted(task)
            }
        }

        @Override
        void afterExecute(Task task, TaskState state) {
            if (isApplicable(task) && state.executed) {
                afterTaskExecuted(task)
            }
        }
    }
}
