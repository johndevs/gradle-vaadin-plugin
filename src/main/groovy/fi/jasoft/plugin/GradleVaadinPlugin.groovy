/*
* Copyright 2013 John Ahlroos
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
package fi.jasoft.plugin

import fi.jasoft.plugin.configuration.VaadinPluginExtension
import fi.jasoft.plugin.tasks.BuildJavadocJarTask
import fi.jasoft.plugin.tasks.BuildSourcesJarTask
import fi.jasoft.plugin.tasks.CreateCompositeTask
import fi.jasoft.plugin.tasks.CreateServlet3ProjectTask
import fi.jasoft.plugin.tasks.CreateTestbenchTestTask
import fi.jasoft.plugin.tasks.DirectorySearchTask
import fi.jasoft.plugin.tasks.UpdateAddonStylesTask;
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin

import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.CreateComponentTask
import fi.jasoft.plugin.tasks.CreateThemeTask
import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import fi.jasoft.plugin.tasks.DevModeTask
import fi.jasoft.plugin.tasks.SuperDevModeTask
import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.UpdateWidgetsetTask
import fi.jasoft.plugin.tasks.RunTask
import fi.jasoft.plugin.tasks.CreateWidgetsetGeneratorTask

class GradleVaadinPlugin implements Plugin<Project> {

    public static final PLUGIN_VERSION
    public static final PLUGIN_PROPERTIES
    public static final PLUGIN_DEBUG_DIR

    static {
        PLUGIN_PROPERTIES = new Properties()
        PLUGIN_PROPERTIES.load(GradleVaadinPlugin.class.getResourceAsStream('/plugin.properties'))
        PLUGIN_VERSION = PLUGIN_PROPERTIES.getProperty('version')
        PLUGIN_DEBUG_DIR = PLUGIN_PROPERTIES.getProperty("debugdir")
    }

    static String getVersion() {
        return PLUGIN_VERSION
    }

    static String getDebugDir() {
        return PLUGIN_DEBUG_DIR
    }

    void apply(Project project) {

        project.logger.quiet("Using Gradle Vaadin Plugin " + PLUGIN_VERSION)

        // Extensions
        project.extensions.create('vaadin', VaadinPluginExtension)

        // Dependency resolution
        project.getGradle().addProjectEvaluationListener(new DependencyListener());
        project.getGradle().getTaskGraph().addTaskExecutionListener(new TaskListener())

        // Plugins
        project.plugins.apply(WarPlugin)

        // Tasks
        project.tasks.create(name: CreateProjectTask.NAME, type: CreateProjectTask, group: 'Vaadin')
        project.tasks.create(name: CreateServlet3ProjectTask.NAME, type: CreateServlet3ProjectTask, group: 'Vaadin')
        project.tasks.create(name: CreateComponentTask.NAME, type: CreateComponentTask, group: 'Vaadin')
        project.tasks.create(name: CreateCompositeTask.NAME, type: CreateCompositeTask, group: 'Vaadin')
        project.tasks.create(name: CreateThemeTask.NAME, type: CreateThemeTask, group: 'Vaadin')
        project.tasks.create(name: CreateWidgetsetGeneratorTask.NAME, type: CreateWidgetsetGeneratorTask, group: 'Vaadin')

        project.tasks.create(name: CompileWidgetsetTask.NAME, type: CompileWidgetsetTask, group: 'Vaadin')
        project.tasks.create(name: DevModeTask.NAME, type: DevModeTask, group: 'Vaadin')
        project.tasks.create(name: SuperDevModeTask.NAME, type: SuperDevModeTask, group: 'Vaadin')
        project.tasks.create(name: CompileThemeTask.NAME, type: CompileThemeTask, group: 'Vaadin')
        project.tasks.create(name: RunTask.NAME, type: RunTask, group: 'Vaadin')
        project.tasks.create(name: UpdateWidgetsetTask.NAME, type: UpdateWidgetsetTask, group: 'Vaadin')
        project.tasks.create(name: UpdateAddonStylesTask.NAME, type: UpdateAddonStylesTask, group: 'Vaadin')

        project.tasks.create(name: 'sourcesJar', type: BuildSourcesJarTask, group: 'Vaadin Utility')
        project.tasks.create(name: 'javadocJar', type: BuildJavadocJarTask, group: 'Vaadin Utility')

        project.tasks.create(name: 'createTestbenchTest', type: CreateTestbenchTestTask, group: 'Vaadin Testbench')

        project.tasks.create(name: DirectorySearchTask.NAME, type: DirectorySearchTask, group: 'Vaadin Directory')

        // Add debug information to all compilation results
        project.tasks.compileJava.options.debugOptions.debugLevel = 'source,lines,vars'

        // Add sources to test classpath
        project.sourceSets.test.runtimeClasspath += project.files(project.sourceSets.main.java.srcDirs)

        // War project should build the widgetset and themes
        project.war.dependsOn(CompileWidgetsetTask.NAME)
        project.war.dependsOn(CompileThemeTask.NAME)

        // Ensure widgetset is up-2-date
        project.processResources.dependsOn(UpdateWidgetsetTask.NAME)

        // Ensure addon themes are up2date
        project.processResources.dependsOn(UpdateAddonStylesTask.NAME)

        // Cleanup plugin outputs
        project.clean.dependsOn(project.tasks.cleanWidgetset)
        project.clean.dependsOn(project.tasks.cleanVaadinRun)
        project.clean.dependsOn(project.tasks.cleanThemes)
        project.clean.dependsOn(project.tasks.cleanSuperdevmode)
        project.clean.dependsOn(project.tasks.cleanDevmode)

        // Utilities
        project.artifacts.add('archives', project.tasks.sourcesJar)
        project.artifacts.add('archives', project.tasks.javadocJar)
    }

}
