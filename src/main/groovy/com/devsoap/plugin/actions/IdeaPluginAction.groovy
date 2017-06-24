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

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.tasks.RunTask
import groovy.transform.PackageScope
import org.gradle.api.Project

/**
 * Actions applied to project when idea plugin is applied
 */
class IdeaPluginAction extends PluginAction {

    @Override
    String getPluginId() {
        'idea'
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        def module = project.idea.module

        // Module name is project name
        module.name = project.name
        module.downloadJavadoc = true
        module.downloadSources = true

        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SERVER)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_CLIENT)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_PUSH)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_TESTBENCH, true)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_THEME)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT)
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        def module = project.idea.module

        // Configure output dirs only if user has not defined it himself
        if ( module.inheritOutputDirs == null ) {
            module.inheritOutputDirs = false
            RunTask runTask = project.tasks.getByName(RunTask.NAME)
            if ( runTask.classesDir == null ) {
                module.outputDir = project.sourceSets.main.output.classesDir
                module.testOutputDir = project.sourceSets.test.output.classesDir
            } else {
                module.outputDir = project.file(runTask.classesDir)
                module.testOutputDir = project.file(runTask.classesDir)
            }
        }
    }

    /**
     * Adds a dependency configuration to the module scope
     *
     * @param project
     *      the project to add the configuration to
     * @param conf
     *      the configuration name (must exist in project.configurations)
     * @param test
     *      is the configuration a test dependency
     */
    @PackageScope
    static void addConfigurationToProject(Project project, String conf, boolean test=false) {
        def scopes = project.idea.module.scopes
        if ( test ) {
            scopes.TEST.plus += [project.configurations[conf]]
        } else {
            scopes.COMPILE.plus += [project.configurations[conf]]
        }
    }
}
