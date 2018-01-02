/*
 * Copyright 2018 John Ahlroos
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
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util
import com.devsoap.plugin.extensions.AddonExtension
import com.devsoap.plugin.extensions.TestBenchExtension
import com.devsoap.plugin.extensions.TestBenchHubExtension
import com.devsoap.plugin.extensions.TestBenchNodeExtension
import com.devsoap.plugin.servers.ApplicationServer
import com.devsoap.plugin.tasks.CompileWidgetsetTask
import com.devsoap.plugin.tasks.CreateWidgetsetGeneratorTask
import com.devsoap.plugin.tasks.UpdateWidgetsetTask
import com.devsoap.plugin.testbench.TestbenchHub
import com.devsoap.plugin.testbench.TestbenchNode
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.jvm.tasks.ProcessResources

import java.nio.file.Paths

/**
 * Actions applied when the java plugin is added to the build
 *
 * @author John Ahlroos
 * @since 1.2
 */
class JavaPluginAction extends PluginAction {

    /**
     * The testbench hub instance
     */
    TestbenchHub testbenchHub

    /**
     * The testbench node instance
     */
    TestbenchNode testbenchNode

    /**
     * The application server instance
     */
    ApplicationServer testbenchAppServer

    @Override
    String getPluginId() {
        'java'
    }

    @Override
    void apply(Project project) {
        super.apply(project)
        project.plugins.apply(JavaPlugin)
    }

    @Override
    void execute(Project project) {
        super.execute(project)

        // Add debug information to all compilation results
        TaskContainer tasks = project.tasks
        tasks.compileJava.options.debugOptions.debugLevel = 'source,lines,vars'

        // Add sources to test classpath
        project.sourceSets.test.runtimeClasspath =
                project.sourceSets.test.runtimeClasspath + (project.files(project.sourceSets.main.java.srcDirs))

        // Ensure widgetset is up-2-date
        ProcessResources resources = project.processResources
        resources.dependsOn(UpdateWidgetsetTask.NAME)
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case 'compileJava':
                ensureWidgetsetGeneratorExists(task)
                break
            case 'jar':
                configureAddonMetadata(task)
                break
            case 'javadoc':
                configureJavadoc(task)
                break
            case 'test':
                configureTest(task, this)
                break
        }
    }

    @Override
    protected void afterTaskExecuted(Task task) {
        super.afterTaskExecuted(task)
        switch (task.name) {
            case 'test':
                terminateTestbench(this)
                break
        }
    }

    private static ensureWidgetsetGeneratorExists(Task task) {
        CompileWidgetsetTask compileWidgetsetTask = task.project.tasks.getByName(CompileWidgetsetTask.NAME)
        String generator = compileWidgetsetTask.widgetsetGenerator
        if ( generator != null ) {
            String name = generator.tokenize('.').last()
            String pkg = generator.replaceAll(".$name", '')
            String filename = "${name}.java"
            File javaDir = Util.getMainSourceSet(task.project).srcDirs.iterator().next()
            File f = Paths.get(javaDir.canonicalPath, TemplateUtil.convertFQNToFilePath(pkg), filename).toFile()
            if ( !f.exists() ) {
                task.project.tasks[CreateWidgetsetGeneratorTask.NAME].run()
            }
        }
    }

    private static configureAddonMetadata(Task task) {
        Project project = task.project
        CompileWidgetsetTask compileWidgetsetTask = project.tasks.getByName(CompileWidgetsetTask.NAME)

        // Resolve widgetset
        String widgetset = compileWidgetsetTask.widgetset
        if ( widgetset == null ) {
            widgetset = GradleVaadinPlugin.DEFAULT_WIDGETSET
        }

        // Scan for existing manifest in source folder and reuse if possible
        File manifest = getManifest(task)
        if ( manifest != null ) {
            project.logger.warn('Manifest found in project, possibly overwriting existing values.')
            task.manifest.from(manifest)
        }

        //Validate values
        AddonExtension addonExtension = project.extensions.getByType(AddonExtension)
        if ( addonExtension.title == '' ) {
            project.logger.warn('No addon title has been specified, ' +
                    'jar not compatible with Vaadin Directory.')
        }

        if ( project.version == 'unspecified' ) {
            project.logger.warn('No version specified for the project, jar not ' +
                    'compatible with Vaadin Directory.')
        }

        // Get stylesheets
        List styles = Util.findAddonSassStylesInProject(project)
        if ( addonExtension.styles ) {
            addonExtension.styles.each({ path ->
                if ( path.endsWith('scss') || path.endsWith('.css') ) {
                    styles.add(path)
                } else {
                    project.logger.warn("Could not add $path to jar manifest. " +
                            'Only CSS and SCSS files are supported as addon styles.')
                }
            })
        }

        // Add metadata to jar manifest
        Map attributes = [:]
        attributes['Vaadin-Package-Version'] = 1
        attributes['Implementation-Version'] = project.version
        attributes['Built-By'] = "Gradle Vaadin Plugin ${GradleVaadinPlugin.version}"
        if ( widgetset ) {
            attributes['Vaadin-Widgetsets'] = widgetset
        }
        if ( styles ) {
            attributes['Vaadin-Stylesheets'] = styles.join(',')
        }
        if ( addonExtension.license ) {
            attributes['Vaadin-License-Title'] = addonExtension.license
        }
        if ( addonExtension.title ) {
            attributes['Implementation-Title'] = addonExtension.title
        }
        if ( addonExtension.author ) {
            attributes['Implementation-Vendor'] = addonExtension.author
        }
        task.manifest.attributes(attributes)
    }

    private static configureTest(Task task, JavaPluginAction listener) {
        Project project = task.project
        TestBenchExtension tb = project.extensions.getByType(TestBenchExtension)

        if ( tb.enabled ) {
            TestBenchHubExtension tbHub = project.extensions.getByType(TestBenchHubExtension)
            if ( tbHub.enabled ) {
                listener.testbenchHub = new TestbenchHub(project)
                listener.testbenchHub.start()
            }

            TestBenchNodeExtension tbNode = project.extensions.getByType(TestBenchNodeExtension)
            if ( tbNode.enabled ) {
                listener.testbenchNode = new TestbenchNode(project)
                listener.testbenchNode.start()
            }

            if ( tb.runApplication ) {
                listener.testbenchAppServer = ApplicationServer.get(project)
                listener.testbenchAppServer.start()

                // Ensure everything is up and running before continuing with the tests
                sleep(5000)
            }
        }
    }

    private static terminateTestbench(JavaPluginAction listener) {
        if ( listener.testbenchAppServer ) {
            listener.testbenchAppServer.terminate()
            listener.testbenchAppServer = null
        }

        if ( listener.testbenchNode ) {
            listener.testbenchNode.terminate()
            listener.testbenchNode = null
        }

        if ( listener.testbenchHub ) {
            listener.testbenchHub.terminate()
            listener.testbenchHub = null
        }
    }

    private static configureJavadoc(Task task) {
        Project project = task.project
        task.source = Util.getMainSourceSet(project)
        if ( project.configurations.findByName(GradleVaadinPlugin.CONFIGURATION_JAVADOC) ) {
            task.classpath = task.classpath + (project.configurations[GradleVaadinPlugin.CONFIGURATION_JAVADOC])
        }
        if ( project.configurations.findByName(GradleVaadinPlugin.CONFIGURATION_SERVER) ) {
            task.classpath = task.classpath + (project.configurations[GradleVaadinPlugin.CONFIGURATION_SERVER])
        }
        task.options.addStringOption('sourcepath', '')
    }

    private static File getManifest(Task task) {
        Project project = task.project
        List sources = Util.getMainSourceSet(project).srcDirs.asList()
        sources.addAll(project.sourceSets.main.resources.srcDirs.asList())

        File manifest = null
        sources.each {
            project.fileTree(it).matching({
                include '**/META-INF/MANIFEST.MF'
            }).each {
                manifest = it
            }
        }
        return manifest
    }
}
