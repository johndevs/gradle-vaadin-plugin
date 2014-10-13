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

import fi.jasoft.plugin.DependencyListener.Configuration
import fi.jasoft.plugin.tasks.CreateDirectoryZipTask
import fi.jasoft.plugin.tasks.CreateWidgetsetGeneratorTask
import fi.jasoft.plugin.testbench.TestbenchHub
import fi.jasoft.plugin.testbench.TestbenchNode
import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.War

public class TaskListener implements TaskExecutionListener {

    private TestbenchHub testbenchHub

    private TestbenchNode testbenchNode

    ApplicationServer testbenchAppServer

    private final Project project

    public TaskListener(Project project) {
        this.project = project
    }

    public void beforeExecute(Task task) {

        if (project != task.getProject() || !project.hasProperty('vaadin')) {
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

            if(task.getName() == 'ideaModule'){
               configureIdeaModule(task)
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
        }

        if (task.getName() == 'war') {
            configureJRebel(task)

            // Exclude unit cache
            War war = (War) task;
            war.exclude('VAADIN/gwt-unitCache/**')

            // Include project classes and resources
            war.classpath = project.files(
                    project.sourceSets.main.output.classesDir,
                    project.sourceSets.main.output.resourcesDir
            )

            // remove duplicates and providedCompile dependencies
            war.classpath += project.configurations[Configuration.SERVER.caption]

            // Include push dependencies if enabled
            if(Util.isPushSupportedAndEnabled(project)) {
                war.classpath += project.configurations[Configuration.PUSH.caption]
            }

            // Remove providedCompile dependencies
            war.classpath -= project.configurations.providedCompile

            // Ensure no duplicates
            war.classpath = war.classpath.files
        }

        if (task.getName() == 'test' && project.vaadin.testbench.enabled) {

            if (project.vaadin.testbench.hub.enabled) {
                testbenchHub = new TestbenchHub(project)
                testbenchHub.start()
            }

            if (project.vaadin.testbench.node.enabled) {
                testbenchNode = new TestbenchNode(project)
                testbenchNode.start()
            }

            if (project.vaadin.testbench.runApplication) {
                testbenchAppServer = new ApplicationServer(project)
                testbenchAppServer.start()

                // Ensure everything is up and running before continuing with the tests
                sleep(5000)
            }
        }

        if (task.getName() == 'javadoc') {
            task.source = Util.getMainSourceSet(project)
            if (project.configurations.findByName(Configuration.JAVADOC.caption) != null) {
                task.classpath += [project.configurations[Configuration.JAVADOC.caption]]
            }
            if (project.configurations.findByName(Configuration.SERVER.caption) != null) {
                task.classpath += [project.configurations[Configuration.SERVER.caption]]
            }
            task.failOnError = false
            task.options.addStringOption("sourcepath", "")
        }

        if (task.getName() == CreateDirectoryZipTask.NAME) {
            configureAddonZipMetadata(task)

        }
    }

    public void afterExecute(Task task, TaskState state) {

        if (project != task.getProject() || !project.hasProperty('vaadin')) {
            return
        }

        if (task.getName() == 'test' && project.vaadin.testbench.enabled) {

            if (testbenchAppServer != null) {
                testbenchAppServer.terminate()
                testbenchAppServer = null
            }

            if (testbenchNode != null) {
                testbenchNode.terminate()
                testbenchNode = null
            }

            if (testbenchHub != null) {
                testbenchHub.terminate()
                testbenchHub = null
            }
        }

        // Notify users that sources are not present in the jar
        if (task.getName() == 'jar' && !state.getSkipped()) {
            task.getLogger().warn("Please note that the jar archive will NOT by default include the source files.\n" +
                    "You can add them to the jar by adding jar{ from sourceSets.main.allJava } to build.gradle.")
        }
    }

    private void configureEclipsePlugin(Task task) {
        def cp = project.eclipse.classpath
        def conf = project.configurations

        // Always download sources
        cp.downloadSources = true

        // Set Eclipse's class output dir
        if (project.vaadin.plugin.eclipseOutputDir == null) {
            cp.defaultOutputDir = project.sourceSets.main.output.classesDir
        }
        else {
            cp.defaultOutputDir = project.file(project.vaadin.plugin.eclipseOutputDir)
        }

        // Add dependencies to eclipse classpath
        cp.plusConfigurations += [conf[Configuration.SERVER.caption]]
        cp.plusConfigurations += [conf[Configuration.CLIENT.caption]]
        cp.plusConfigurations += [conf[Configuration.JETTY9.caption]]

        if (project.vaadin.testbench.enabled) {
            cp.plusConfigurations += [conf[Configuration.TESTBENCH.caption]]
        }

        if (Util.isPushSupportedAndEnabled(project)) {
            cp.plusConfigurations += [conf[Configuration.PUSH.caption]]
        }
    }

    private void configureIdeaModule(Task task) {

        def conf = project.configurations
        def module = project.idea.module

        // Module name is project name
        module.name = project.name

        module.inheritOutputDirs = false
        module.outputDir = project.file('build/classes/main')
        module.testOutputDir = project.file('build/classes/test')


        // Download sources and javadoc
        module.downloadJavadoc = true
        module.downloadSources = true

        // Add configurations to classpath
        module.scopes.COMPILE.plus += [conf[Configuration.SERVER.caption]]
        module.scopes.COMPILE.plus += [conf[Configuration.CLIENT.caption]]
        module.scopes.PROVIDED.plus += [conf[Configuration.JETTY9.caption]]

        if (project.vaadin.testbench.enabled) {
            module.scopes.TEST.plus += [conf[Configuration.TESTBENCH.caption]]
        }

        if (Util.isPushSupportedAndEnabled(project)) {
            module.scopes.COMPILE.plus += [conf[Configuration.PUSH.caption]]
        }
    }

    private void configureEclipseWtpPluginComponent(Task task) {
        def wtp = project.eclipse.wtp
        wtp.component.plusConfigurations += [project.configurations[Configuration.SERVER.caption]]
    }

    private void configureEclipseWtpPluginFacet(Task task) {
        def wtp = project.eclipse.wtp
        wtp.facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '7.0')
        wtp.facet.facet(name: 'jst.web', version: '3.0')
        wtp.facet.facet(name: 'java', version: project.sourceCompatibility)
    }

    private void ensureWidgetsetGeneratorExists(Task task) {
        def generator = project.vaadin.widgetsetGenerator
        if (generator != null) {
            String name = generator.tokenize('.').last()
            String pkg = generator.replaceAll('.' + name, '')
            String filename = name + ".java"
            File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
            File f = project.file(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./, '/') + '/' + filename)
            if (!f.exists()) {
                project.tasks[CreateWidgetsetGeneratorTask.NAME].run()
            }
        }
    }

    private File getManifest() {
        def sources = Util.getMainSourceSet(project).srcDirs.asList() + project.sourceSets.main.resources.srcDirs.asList()
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

    private void configureAddonMetadata(Task task) {

        // Resolve widgetset
        def widgetset = project.vaadin.widgetset
        if (widgetset == null) {
            widgetset = 'com.vaadin.DefaultWidgetSet'
        }

        // Scan for existing manifest in source folder and reuse if possible
        File manifest = getManifest()
        if (manifest != null) {
            project.logger.warn("Manifest found in project, possibly overwriting existing values.")
            task.manifest.from(manifest)
        }

        //Validate values
        if (project.vaadin.addon.title == '') {
            project.logger.warn("No vaadin.addon.title has been specified, jar not compatible with Vaadin Directory.")
        }

        if (project.version == 'unspecified') {
            project.logger.warn("No version specified for the project, jar not compatible with Vaadin Directory.")
        }

        // Get stylesheets
        def styles = Util.findAddonSassStylesInProject(project)
        if(project.vaadin.addon.styles != null){
            project.vaadin.addon.styles.each({ path ->
                if(path.endsWith('scss') || path.endsWith('.css')){
                    styles.add(path)
                } else {
                    project.logger.warn("Could not add '"+path+"' to jar manifest. Only CSS and SCSS files are supported as addon styles.")
                }
            })
        }

        // Add metadata to jar manifest
        task.manifest.attributes(
                'Vaadin-Package-Version': 1,
                'Vaadin-Widgetsets': widgetset,
                'Vaadin-Stylesheets': styles.join(','),
                'Vaadin-License-Title': project.vaadin.addon.license,
                'Implementation-Title': project.vaadin.addon.title,
                'Implementation-Version': project.version,
                'Implementation-Vendor': project.vaadin.addon.author,
                'Built-By': "Gradle Vaadin Plugin ${GradleVaadinPlugin.PLUGIN_VERSION}"
        )
    }

    private void configureAddonZipMetadata(Task task) {

        // Create metadata file
        def buildDir = project.file('build/tmp/zip')
        buildDir.mkdirs()

        def meta = project.file(buildDir.absolutePath + '/META-INF')
        meta.mkdirs()

        def manifest = project.file(meta.absolutePath + '/MANIFEST.MF')
        manifest.createNewFile()

        manifest << """
            Vaadin-Package-Version: 1
            Vaadin-License-Title: ${project.vaadin.addon.license}
            Implementation-Title: ${project.vaadin.addon.title}
            Implementation-Version: ${project.version != null ? project.version : ''}
            Implementation-Vendor: ${project.vaadin.addon.author}
            Vaadin-Addon: libs/${project.jar.archiveName}
        """.stripIndent().trim()
    }

    private void configureJRebel(Task task) {
        if (project.vaadin.jrebel.enabled) {

            def classes = project.sourceSets.main.output.classesDir

            // Ensure classes dir exists
            classes.mkdirs();

            File rebelFile = project.file(classes.absolutePath + '/rebel.xml')

            def srcWebApp = project.webAppDir.absolutePath
            def writer = new FileWriter(rebelFile)

            new MarkupBuilder(writer).application() {
                classpath {
                    dir(name: classes.absolutePath)
                }
                web {
                    link(target: '/') {
                        dir(name: srcWebApp)
                    }
                }
            }
        }
    }
}