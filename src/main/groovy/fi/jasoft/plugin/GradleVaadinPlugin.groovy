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

import fi.jasoft.plugin.tasks.CreateCompositeTask
import fi.jasoft.plugin.tasks.CreateServlet3ProjectTask
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

    static {
        PLUGIN_PROPERTIES = new Properties()
        PLUGIN_PROPERTIES.load(GradleVaadinPlugin.class.getResourceAsStream('/plugin.properties'))
        PLUGIN_VERSION = PLUGIN_PROPERTIES.getProperty('version')
    }

    static String getVersion() {
        return PLUGIN_VERSION
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
        project.tasks.create(name: 'createVaadinProject', type: CreateProjectTask, group: 'Vaadin')
        project.tasks.create(name: 'createVaadinServlet3Project', type: CreateServlet3ProjectTask, group: 'Vaadin')
        project.tasks.create(name: 'createVaadinComponent', type: CreateComponentTask, group: 'Vaadin')
        project.tasks.create(name: 'createVaadinComposite', type: CreateCompositeTask, group: 'Vaadin')
        project.tasks.create(name: 'createVaadinTheme', type: CreateThemeTask, group: 'Vaadin')
        project.tasks.create(name: 'createVaadinWidgetsetGenerator', type: CreateWidgetsetGeneratorTask, group: 'Vaadin')

        project.tasks.create(name: 'widgetset', type: CompileWidgetsetTask, group: 'Vaadin')
        project.tasks.create(name: 'devmode', type: DevModeTask, group: 'Vaadin')
        project.tasks.create(name: 'superdevmode', type: SuperDevModeTask, group: 'Vaadin')
        project.tasks.create(name: 'themes', type: CompileThemeTask, group: 'Vaadin')
        project.tasks.create(name: 'vaadinRun', type: RunTask, group: 'Vaadin')
        project.tasks.create(name: 'updateWidgetset', type: UpdateWidgetsetTask, group: 'Vaadin')
        project.tasks.create(name: 'updateAddonStyles', type: UpdateAddonStylesTask, group: 'Vaadin')

        // Add debug information to all compilation results
        project.tasks.compileJava.options.debugOptions.debugLevel = 'source,lines,vars'

        // Add sources to test classpath
        project.sourceSets.test.runtimeClasspath += project.files(project.sourceSets.main.java.srcDirs)

        // War project should build the widgetset and themes
        project.war.dependsOn(project.tasks.widgetset)
        project.war.dependsOn(project.tasks.themes)

        // Ensure widgetset is up-2-date
        project.processResources.dependsOn(project.tasks.updateWidgetset)

        // Ensure addon themes are up2date
        project.processResources.dependsOn(project.tasks.updateAddonStyles)

        // Cleanup plugin outputs
        project.clean.dependsOn(project.tasks.cleanWidgetset)
        project.clean.dependsOn(project.tasks.cleanVaadinRun)
        project.clean.dependsOn(project.tasks.cleanThemes)
        project.clean.dependsOn(project.tasks.cleanSuperdevmode)
        project.clean.dependsOn(project.tasks.cleanDevmode)
    }
}
