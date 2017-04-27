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


import fi.jasoft.plugin.Util
import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.bundling.War

/**
 * Actions applied when the war plugin is added to the build
 */
class WarPluginAction extends PluginAction {

    @Override
    String getPluginId() {
        return WarPlugin.WAR_TASK_NAME
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        War war = project.war
        war.dependsOn(CompileWidgetsetTask.NAME)
        war.dependsOn(CompileThemeTask.NAME)
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case 'war':
                configureWAR(task)
                break
        }
    }

    @PackageScope
    static configureWAR(Task task) {
        assert task in War
        War war = (War) task
        war.exclude('VAADIN/gwt-unitCache/**')
        if ( task.project.vaadin.manageDependencies ) {
            war.classpath = Util.getWarClasspath(task.project).files
        }
    }
}
