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
package fi.jasoft.plugin.actions

import fi.jasoft.plugin.GradleVaadinGroovyPlugin
import fi.jasoft.plugin.GradleVaadinPlugin
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

    protected void execute(Project project) {
        project.logger.debug("Applying ${getClass().simpleName} actions")
    }

    protected void beforeTaskExecuted(Task task) {
        task.project.logger.debug("Applying ${getClass().simpleName} before task actions")
    }

    protected void afterTaskExecuted(Task task, TaskState state) {
        task.project.logger.debug("Applying ${getClass().simpleName} after task actions")
    }

    final void apply(Project project) {
        project.plugins.withId(pluginId) {
            project.gradle.taskGraph.addTaskExecutionListener(new TaskExecutionListener() {
                @Override
                void beforeExecute(Task task) {
                    if (isApplicable(task) ) {
                        beforeTaskExecuted(task)
                    }
                }

                @Override
                void afterExecute(Task task, TaskState state) {
                    if (isApplicable(task) ) {
                        afterTaskExecuted(task, state)
                    }
                }
            })
            execute(project)
        }
    }

    final static boolean isApplicable(Task task) {
        PluginManager pluginManager = task.project.pluginManager
        pluginManager.hasPlugin(GradleVaadinPlugin.getPluginId()) || pluginManager
                .hasPlugin(GradleVaadinGroovyPlugin.getPluginId())
    }
}