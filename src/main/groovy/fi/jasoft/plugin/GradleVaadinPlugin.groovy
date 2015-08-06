/*
* Copyright 2015 John Ahlroos
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
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

class GradleVaadinPlugin implements Plugin<Project> {

    public static final PLUGIN_VERSION
    public static final PLUGIN_PROPERTIES
    public static final PLUGIN_DEBUG_DIR

    public static int PLUGINS_IN_PROJECT = 0;

    static {
        PLUGIN_PROPERTIES = new Properties()
        PLUGIN_PROPERTIES.load(GradleVaadinPlugin.class.getResourceAsStream('/vaadin_plugin.properties'))
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

        def gradle = project.gradle
        def version = VersionNumber.parse(gradle.gradleVersion)
        def requiredVersion = new VersionNumber(2, 6, 0, null)
        if(version.baseVersion < requiredVersion) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. Plugin requires Gradle $requiredVersion+")
        }

        PLUGINS_IN_PROJECT++;

        if (firstPlugin) {
            project.logger.quiet("Using Gradle Vaadin Plugin " + PLUGIN_VERSION)
        }

        // Extensions
        project.extensions.create('vaadin', VaadinPluginExtension)

        // Dependency resolution
        gradle.addProjectEvaluationListener(new DependencyListener())
        gradle.taskGraph.addTaskExecutionListener(new TaskListener(project))

        // Plugins
        project.plugins.apply(WarPlugin)

        // Tasks
        def tasks = project.tasks
        tasks.create(name: CreateProjectTask.NAME, type: CreateProjectTask, group: 'Vaadin')
        tasks.create(name: CreateComponentTask.NAME, type: CreateComponentTask, group: 'Vaadin')
        tasks.create(name: CreateCompositeTask.NAME, type: CreateCompositeTask, group: 'Vaadin')
        tasks.create(name: CreateThemeTask.NAME, type: CreateThemeTask, group: 'Vaadin')
        tasks.create(name: CreateWidgetsetGeneratorTask.NAME, type: CreateWidgetsetGeneratorTask, group: 'Vaadin')

        tasks.create(name: CompileWidgetsetTask.NAME, type: CompileWidgetsetTask, group: 'Vaadin')
        tasks.create(name: DevModeTask.NAME, type: DevModeTask, group: 'Vaadin')
        tasks.create(name: SuperDevModeTask.NAME, type: SuperDevModeTask, group: 'Vaadin')
        tasks.create(name: CompileThemeTask.NAME, type: CompileThemeTask, group: 'Vaadin')
        tasks.create(name: RunTask.NAME, type: RunTask, group: 'Vaadin')
        tasks.create(name: UpdateWidgetsetTask.NAME, type: UpdateWidgetsetTask, group: 'Vaadin')

        tasks.create(name: UpdateAddonStylesTask.NAME, type: UpdateAddonStylesTask, group: 'Vaadin')
        tasks.create(name: CreateAddonThemeTask.NAME, type: CreateAddonThemeTask, group: 'Vaadin')

        tasks.create(name: BuildSourcesJarTask.NAME, type: BuildSourcesJarTask, group: 'Vaadin Utility')
        tasks.create(name: BuildJavadocJarTask.NAME, type: BuildJavadocJarTask, group: 'Vaadin Utility')
        tasks.create(name: BuildClassPathJar.NAME, type: BuildClassPathJar, group: 'Vaadin Utility')

        tasks.create(name: CreateTestbenchTestTask.NAME, type: CreateTestbenchTestTask, group: 'Vaadin Testbench')

        tasks.create(name: DirectorySearchTask.NAME, type: DirectorySearchTask, group: 'Vaadin Directory')
        tasks.create(name: CreateDirectoryZipTask.NAME, type: CreateDirectoryZipTask, group: 'Vaadin Directory')

        // Add debug information to all compilation results
        tasks.compileJava.options.debugOptions.debugLevel = 'source,lines,vars'

        // Add sources to test classpath
        project.sourceSets.test.runtimeClasspath += [project.files(project.sourceSets.main.java.srcDirs)]

        // War project should build the widgetset and themes
        def war = project.war
        war.dependsOn(CompileWidgetsetTask.NAME)
        war.dependsOn(CompileThemeTask.NAME)

        // Ensure widgetset is up-2-date
        def resources = project.processResources
        resources.dependsOn(UpdateWidgetsetTask.NAME)

        // Ensure addon themes are up2date
        resources.dependsOn(UpdateAddonStylesTask.NAME)

        // Cleanup plugin outputs
        def clean = project.clean
        clean.dependsOn(tasks['clean' + CompileWidgetsetTask.NAME.capitalize()])
        clean.dependsOn(tasks['clean' + RunTask.NAME.capitalize()])
        clean.dependsOn(tasks['clean' + CompileThemeTask.NAME.capitalize()])
        clean.dependsOn(tasks['clean' + SuperDevModeTask.NAME.capitalize()])
        clean.dependsOn(tasks['clean' + DevModeTask.NAME.capitalize()])

        // Utilities
        def artifacts = project.artifacts
        artifacts.add('archives', tasks[BuildSourcesJarTask.NAME])
        artifacts.add('archives', tasks[BuildJavadocJarTask.NAME])
    }

}
