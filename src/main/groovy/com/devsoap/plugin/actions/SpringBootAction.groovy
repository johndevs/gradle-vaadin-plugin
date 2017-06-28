/*
 * Copyright 2017 John Ahlroos
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

import com.devsoap.plugin.Util
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CompileWidgetsetTask
import com.devsoap.plugin.tasks.CompressCssTask
import com.devsoap.plugin.tasks.RunTask
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc

/**
 * Actions applied when the Spring Boot plugin as added to the build
 *
 * @author John Ahlroos
 * @since 1.2
 */
class SpringBootAction extends PluginAction {

    static final String BOOT_RUN_TASK = 'bootRun'

    @Override
    String getPluginId() {
        GradleVaadinPlugin.SPRING_BOOT_PLUGIN
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        // bootRun should build the widgetset and theme
        project.tasks.findByName(BOOT_RUN_TASK).dependsOn(CompileWidgetsetTask.NAME, CompressCssTask.NAME)

        // Delegate to bootRun if spring boot is present
        project.tasks.findByName(RunTask.NAME).dependsOn(BOOT_RUN_TASK)
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case BOOT_RUN_TASK:
                configureBootRun(task)
                break
            case 'jar':
                configureJar(task)
                break
            case 'javadoc':
                configureJavadoc(task)
                break
        }
    }

    private static configureBootRun(Task task) {
        Project project = task.project
        task.classpath = Util.getWarClasspath(project)
        task.classpath = task.classpath + (project.configurations[GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT])
    }

    private static configureJar(Task task) {
        Project project = task.project
        if (isJarProject(project)) {
            Jar jar = (Jar) task
            // Include app theme + compiled widget as well as classes into our jar
            jar.from(Util.getWebAppDirectory(project))
            // Compile theme and widgetset before creating jar
            jar.dependsOn(CompileWidgetsetTask.NAME, CompileThemeTask.NAME)
            // Sprinkle some spring boot sugar on the jar to make it runnable
            jar.finalizedBy(project.tasks.findByName('bootRepackage'))
        }
    }

    private static configureJavadoc(Task task) {
        Project project = task.project
        Javadoc javadoc = (Javadoc) task
        javadoc.classpath = javadoc.classpath + (project.configurations[GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT])
    }

    private static boolean isJarProject(Project project) {
        String layout = project.extensions.findByName('springBoot').layout
        if (!layout) {
            // Default is jar
            return true
        }
        layout.toLowerCase() == 'jar'
    }

}
