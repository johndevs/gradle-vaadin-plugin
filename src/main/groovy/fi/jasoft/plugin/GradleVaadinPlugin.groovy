/*
* Copyright 2014 John Ahlroos
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
import fi.jasoft.plugin.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin

class GradleVaadinPlugin implements Plugin<Project> {

    public static final PLUGIN_VERSION
    public static final PLUGIN_PROPERTIES
    public static final PLUGIN_DEBUG_DIR

    public static int PLUGINS_IN_PROJECT = 0;

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

    static int getNumberOfPluginsInProject() {
        return PLUGINS_IN_PROJECT
    }

    static boolean isFirstPlugin() {
        return PLUGINS_IN_PROJECT == 1;
    }

    void apply(Project project) {

        PLUGINS_IN_PROJECT++;

        if (isFirstPlugin()) {
            project.logger.quiet("Using Gradle Vaadin Plugin " + PLUGIN_VERSION)
        }

        // Extensions
        project.extensions.create('vaadin', VaadinPluginExtension)

        // Dependency resolution
        project.getGradle().addProjectEvaluationListener(new DependencyListener())
        project.getGradle().getTaskGraph().addTaskExecutionListener(new TaskListener(project))

        // Plugins
        project.plugins.apply(WarPlugin)

        // Tasks
        project.tasks.create(name: CreateProjectTask.NAME, type: CreateProjectTask, group: 'Vaadin')
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
        project.tasks.create(name: CreateAddonThemeTask.NAME, type: CreateAddonThemeTask, group: 'Vaadin')

        project.tasks.create(name: BuildSourcesJarTask.NAME, type: BuildSourcesJarTask, group: 'Vaadin Utility')
        project.tasks.create(name: BuildJavadocJarTask.NAME, type: BuildJavadocJarTask, group: 'Vaadin Utility')

        project.tasks.create(name: CreateTestbenchTestTask.NAME, type: CreateTestbenchTestTask, group: 'Vaadin Testbench')

        project.tasks.create(name: DirectorySearchTask.NAME, type: DirectorySearchTask, group: 'Vaadin Directory')
        project.tasks.create(name: CreateDirectoryZipTask.NAME, type: CreateDirectoryZipTask, group: 'Vaadin Directory')

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
        project.clean.dependsOn(project.tasks['clean' + CompileWidgetsetTask.NAME.capitalize()])
        project.clean.dependsOn(project.tasks['clean' + RunTask.NAME.capitalize()])
        project.clean.dependsOn(project.tasks['clean' + CompileThemeTask.NAME.capitalize()])
        project.clean.dependsOn(project.tasks['clean' + SuperDevModeTask.NAME.capitalize()])
        project.clean.dependsOn(project.tasks['clean' + DevModeTask.NAME.capitalize()])

        // Utilities
        project.artifacts.add('archives', project.tasks[BuildSourcesJarTask.NAME])
        project.artifacts.add('archives', project.tasks[BuildJavadocJarTask.NAME])
    }

}
