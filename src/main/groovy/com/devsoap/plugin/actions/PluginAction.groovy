/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.actions

import com.devsoap.plugin.GradleVaadinGroovyPlugin
import com.devsoap.plugin.GradleVaadinPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.plugins.PluginManager
import org.gradle.api.tasks.TaskState

/**
 * Base class for reacting to other plugins
 */
abstract class PluginAction {

    abstract String getPluginId()

    protected final TaskListener taskListener = new TaskListener()

    protected void execute(Project project) {
        project.logger.info("Applying ${getClass().simpleName} actions to project $project.name")
    }

    protected void executeAfterEvaluate(Project project){
        project.logger.debug("Executing afterEvaluate hook for ${getClass().simpleName}")
    }

    protected void beforeTaskExecuted(Task task) {
        task.project.logger.debug("Executing pre task hook for ${getClass().simpleName} for task $task.name")
    }

    protected void afterTaskExecuted(Task task) {
        task.project.logger.debug("Executing post task hook for ${getClass().simpleName} for task $task.name")
    }

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

    final static boolean isApplicable(Task task) {
        PluginManager pluginManager = task.project.pluginManager
        pluginManager.hasPlugin(GradleVaadinPlugin.pluginId) || pluginManager
                .hasPlugin(GradleVaadinGroovyPlugin.pluginId)
    }

    final class TaskListener implements TaskExecutionListener {

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
