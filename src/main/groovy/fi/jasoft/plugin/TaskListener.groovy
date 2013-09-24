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

import groovy.xml.MarkupBuilder;
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.Task
import org.gradle.api.tasks.TaskState

public class TaskListener implements TaskExecutionListener {

    private TestbenchHub testbenchHub;


    public void beforeExecute(Task task) {
        def project = task.getProject()
        if (!project.hasProperty('vaadin')) {
            return
        }

        /*
         * Dependency related configurations
         */
        if (project.vaadin.manageDependencies) {

            if (task.getName() == 'eclipseClasspath') {
                configureEclipsePlugin(task)
            }

            if (task.getName() == 'eclipseWtpComponent') {
                configureEclipseWtpPluginComponent(task)
            }
        }

        if (task.getName() == 'eclipseWtpFacet') {
            configureEclipseWtpPluginFacet(task)
        }

        if (task.getName() == 'compileJava') {
            ensureWidgetsetGeneratorExists(task)
        }

        if (task.getName() == 'jar') {
            configureAddonMetadata(task)

            // Notify users that sources are not present in the jar
            task.getLogger().lifecycle("Please note that the jar archive will NOT by default include the source files.\n" +
                    "You can add them to the jar by adding jar{ from sourceSets.main.allJava } to build.gradle.")
        }

        if (task.getName() == 'war') {
            configureJRebel(task)
        }

        if (task.getName() == 'test' && project.vaadin.testbench.enabled){
            testbenchHub = new TestbenchHub(project)
            testbenchHub.start()
        }
    }

    public void afterExecute(Task task, TaskState state) {
        def project = task.getProject()

        if (task.getName() == 'test' && project.vaadin.testbench.enabled){
            testbenchHub.terminate()
            testbenchHub = null
        }
    }

    private void configureEclipsePlugin(Task task) {
        def project = task.getProject()
        def cp = project.eclipse.classpath
        cp.downloadSources = true
        cp.defaultOutputDir = project.file('build/classes/main')
        cp.plusConfigurations += project.configurations.vaadin
        cp.plusConfigurations += project.configurations['vaadin-client']
        cp.plusConfigurations += project.configurations.jetty8
    }

    private void configureEclipseWtpPluginComponent(Task task) {
        def project = task.getProject()
        def wtp = project.eclipse.wtp
        wtp.component.plusConfigurations += project.configurations.vaadin
    }

    private void configureEclipseWtpPluginFacet(Task task) {
        def project = task.getProject()
        def wtp = project.eclipse.wtp

        if (project.vaadin.version.startsWith('6')) {
            wtp.facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '1.0')
        } else {
            wtp.facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '7.0')
        }
        wtp.facet.facet(name: 'jst.web', version: project.vaadin.servletVersion)
        wtp.facet.facet(name: 'java', version: project.sourceCompatibility)
    }

    private void ensureWidgetsetGeneratorExists(Task task) {
        def project = task.getProject()
        if (project.vaadin.widgetsetGenerator != null) {
            String name = project.vaadin.widgetsetGenerator.tokenize('.').last()
            String pkg = project.vaadin.widgetsetGenerator.replaceAll('.' + name, '')
            String filename = name + ".java"
            File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
            File f = new File(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./, '/') + '/' + filename)
            if (!f.exists()) {
                project.tasks.createVaadinWidgetsetGenerator.run()
            }
        }
    }

    private void configureAddonMetadata(Task task) {
        def project = task.getProject()

        // Resolve widgetset
        def widgetset = project.vaadin.widgetset
        if (widgetset == null) {
            if (project.vaadin.version.startsWith('6')) {
                widgetset = 'com.vaadin.terminal.gwt.DefaultWidgetSet'
            } else {
                widgetset = 'com.vaadin.DefaultWidgetSet'
            }
        }

        // Add metadata to jar manifest
        project.tasks.jar.manifest.attributes(
                'Vaadin-Package-Version': 1,
                'Vaadin-Widgetsets': widgetset,
                'Vaadin-License-Title': project.vaadin.addon.license,
                'Implementation-Title': project.vaadin.addon.title,
                'Implementation-Version': project.version != null ? project.version : '',
                'Implementation-Vendor': project.vaadin.addon.author,
        )
    }

    private void configureJRebel(Task task) {
        def project = task.getProject()
        def rebelFile = project.sourceSets.main.output.classesDir.absolutePath + '/rebel.xml'
        def srcWebApp = project.webAppDir.absolutePath
        def writer = new FileWriter(rebelFile)

        new MarkupBuilder(writer).application() {
            classpath {
                dir(name: project.sourceSets.main.output.classesDir.absolutePath)
            }
            web {
                link(target: '/') {
                    dir(name: srcWebApp)
                }
            }
        }
    }
}