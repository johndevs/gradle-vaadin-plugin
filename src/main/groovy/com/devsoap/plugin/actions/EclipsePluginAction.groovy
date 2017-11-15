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
import com.devsoap.plugin.tasks.RunTask
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel

/**
 * Actions applied when the eclipse plugin is applied
 *
 * @author John Ahlroos
 * @since 1.2
 */
class EclipsePluginAction extends PluginAction {

    @Override
    String getPluginId() {
        'eclipse'
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)
        EclipseModel eclipse = project.extensions.getByType(EclipseModel)
        eclipse.project.comment = 'Vaadin Project'

        // Always download sources
        def cp = eclipse.classpath
        cp.downloadSources = true

        // Configure natures
        def natures = eclipse.project.natures
        natures.add(0, 'org.springsource.ide.eclipse.gradle.core.nature')
        natures.add(1, 'org.eclipse.buildship.core.gradleprojectnature')

        // Configure build commands
        eclipse.project.buildCommand('org.eclipse.buildship.core.gradleprojectbuilder')

        // Add configurations to classpath
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SERVER)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_CLIENT)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_PUSH)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_TESTBENCH)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_THEME)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_CLIENT_COMPILE)
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        EclipseModel eclipse = project.extensions.getByType(EclipseModel)
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        def cp = eclipse.classpath
        if ( runTask.classesDir == null ) {
            cp.defaultOutputDir = Util.getMainSourceSet(project).outputDir
        } else {
            cp.defaultOutputDir = project.file(runTask.classesDir)
        }
    }

    /**
     * Adds a dependency configuration to the eclipse project classpath
     *
     * @param project
     *      the project to add the configuration to
     * @param conf
     *      the configuration name (must exist in project.configurations)
     * @param deploy
     *      also add to wtp deployment assembly
     */
    private static void addConfigurationToProject(Project project, String conf) {
        EclipseModel eclipse = project.extensions.getByType(EclipseModel)
        EclipseClasspath cp = eclipse.classpath
        cp.plusConfigurations += [project.configurations[conf]]
    }
}
