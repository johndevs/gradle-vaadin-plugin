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

import fi.jasoft.plugin.GradleVaadinPlugin

import fi.jasoft.plugin.Util
import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Actions applied when the Spring Boot plugin as added to the build
 */
class SpringBootAction extends PluginAction {

    @Override
    String getPluginId() {
        GradleVaadinPlugin.SPRING_BOOT_PLUGIN
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        // bootRun should build the widgetset and theme
        project.bootRun.dependsOn(CompileWidgetsetTask.NAME)
        project.bootRun.dependsOn(CompileThemeTask.NAME)
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case 'bootRun':
                configureBootRun(task)
                break;
        }
    }

    @PackageScope
    static configureBootRun(Task task) {
        def project = task.project
        task.classpath = Util.getWarClasspath(project)
        task.classpath = task.classpath + (project.configurations[GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT])
    }
}
