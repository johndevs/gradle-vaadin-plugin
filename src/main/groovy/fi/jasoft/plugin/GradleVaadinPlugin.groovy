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
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.FileTree
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.WarPluginConvention
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
        gradle.taskGraph.addTaskExecutionListener(new TaskListener(project))

        // Plugins
        project.plugins.apply(WarPlugin)

        // Repositories
        applyRepositories(project)

        // Dependencies
        applyDependencies(project)

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

        project.beforeEvaluate { Project p ->
            def plugins = p.plugins
            if (plugins.findPlugin('eclipse') && !plugins.findPlugin('eclipse-wtp')) {
                p.logger.warn("You are using the eclipse plugin which does not support all " +
                        "features of the Vaadin plugin. Please use the eclipse-wtp plugin instead.")
            }
        }

        project.afterEvaluate { Project p ->
            def v = Util.getVaadinVersion(p)
            if(v !=null && v.startsWith("6")){
                p.logger.error("Plugin no longer supports Vaadin 6, to use Vaadin 6 apply an older version of the plugin.")
                throw new InvalidUserDataException("Unsupported Vaadin version.")
            }

            // Remove configurations if the plugin shouldn't manage them
            if(!project.vaadin.manageDependencies){
                project.configurations.removeAll({ Configuration conf ->
                   conf.name.startsWith('vaadin-')
                })
            }
        }
    }

    static void applyRepositories(Project project) {
        project.afterEvaluate {
            if(!project.vaadin.manageRepositories) {
                return
            }

            def repositories = project.repositories

            repositories.mavenCentral()
            repositories.mavenLocal()

            repositories.maven { repository ->
                repository.name = 'Vaadin addons'
                repository.url = 'http://maven.vaadin.com/vaadin-addons'
            }

            repositories.maven { repository ->
                repository.name = 'Vaadin snapshots'
                repository.url = 'http://oss.sonatype.org/content/repositories/vaadin-snapshots'
            }

            repositories.maven { repository ->
                repository.name = 'Jasoft.fi Maven repository'
                repository.url = 'http://mvn.jasoft.fi/maven2'
            }

            repositories.maven { repository ->
                repository.name = 'Bintray.com Maven repository'
                repository.url = 'http://dl.bintray.com/johndevs/maven'
            }

            // Add plugin development repository if specified
            if((debugDir as File)?.exists()
                    && !repositories.findByName('Gradle Vaadin plugin development repository')) {
                if (GradleVaadinPlugin.firstPlugin) {
                    project.logger.lifecycle("Using development libs found at " + debugDir)
                }
                repositories.flatDir(name: 'Gradle Vaadin plugin development repository', dirs: debugDir)
            }
        }
    }

    static void applyDependencies(Project project) {
        def configurations = project.configurations
        def projectDependencies = project.dependencies
        def sources = project.sourceSets.main
        def testSources = project.sourceSets.test

        configurations.create('vaadin-server', { conf ->
            conf.description = 'Libraries needed by Vaadin server side applications.'
            conf.defaultDependencies { dependencies ->
                def vaadinServer = projectDependencies.create("com.vaadin:vaadin-server:${Util.getVaadinVersion(project)}")
                dependencies.add(vaadinServer)

                def vaadinThemes = projectDependencies.create("com.vaadin:vaadin-themes:${Util.getVaadinVersion(project)}")
                dependencies.add(vaadinThemes)

                def servletAPI = projectDependencies.create('javax.servlet:javax.servlet-api:3.0.1')
                dependencies.add(servletAPI)

                // Theme compiler
                if(!Util.isSassCompilerSupported(project)){
                    File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
                    FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
                    if (!themes.isEmpty()) {
                        def themeCompiler = projectDependencies.create("com.vaadin:vaadin-theme-compiler:${Util.getVaadinVersion(project)}")
                        dependencies.add(themeCompiler)
                    }
                }
            }

            sources.compileClasspath += [conf]
            testSources.compileClasspath += [conf]
        })

        configurations.create('vaadin-client', { conf ->
            conf.description = 'Libraries needed for compiling the widgetset.'
            conf.defaultDependencies { dependencies ->
                if(!project.vaadin.widgetset){
                    def widgetsetCompiled = projectDependencies.create("com.vaadin:vaadin-client-compiled:${Util.getVaadinVersion(project)}")
                    dependencies.add(widgetsetCompiled)
                } else {
                    def vaadinClient = projectDependencies.create("com.vaadin:vaadin-client:${Util.getVaadinVersion(project)}")
                    dependencies.add(vaadinClient)

                    def widgetsetCompiler = projectDependencies.create("com.vaadin:vaadin-client-compiler:${Util.getVaadinVersion(project)}")
                    dependencies.add(widgetsetCompiler)

                    def validationAPI = projectDependencies.create('javax.validation:validation-api:1.0.0.GA')
                    dependencies.add(validationAPI)
                }
            }

            sources.compileClasspath += [conf]

            testSources.compileClasspath += [conf]
            testSources.runtimeClasspath += [conf]
        })

        configurations.create('vaadin-javadoc', { conf ->
            conf.description = 'Libraries for compiling JavaDoc for a Vaadin project.'
            conf.defaultDependencies { dependencies ->
                def portletAPI = projectDependencies.create('javax.portlet:portlet-api:2.0')
                dependencies.add(portletAPI)

                def servletAPI = projectDependencies.create('javax.servlet:javax.servlet-api:3.0.1')
                dependencies.add(servletAPI)

                if(Util.isPushSupported(project)){
                    def push = projectDependencies.create("com.vaadin:vaadin-push:${Util.getVaadinVersion(project)}")
                    dependencies.add(push)
                }
            }
        })

        configurations.create('vaadin-jetty9', { conf ->
            conf.description = ' Libraries for running the embedded Jetty 9 server'
            conf.defaultDependencies { dependencies ->
                def jettyVersion = '9.3.0.v20150612'

                def jettyAll = projectDependencies.create("org.eclipse.jetty.aggregate:jetty-all:$jettyVersion")
                dependencies.add(jettyAll)

                def jettyAnnotations = projectDependencies.create("org.eclipse.jetty:jetty-annotations:$jettyVersion")
                dependencies.add(jettyAnnotations)

                def jettyPlus = projectDependencies.create("org.eclipse.jetty:jetty-plus:$jettyVersion")
                dependencies.add(jettyPlus)

                def jettyDeploy = projectDependencies.create("org.eclipse.jetty:jetty-deploy:$jettyVersion")
                dependencies.add(jettyDeploy)

                def slf4j = projectDependencies.create('org.slf4j:slf4j-simple:1.7.12')
                dependencies.add(slf4j)

                def plugin = projectDependencies.create("fi.jasoft.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}")
                dependencies.add(plugin)

                def asm = projectDependencies.create('org.ow2.asm:asm:5.0.3')
                dependencies.add(asm)

                def asmCommons = projectDependencies.create('org.ow2.asm:asm-commons:5.0.3')
                dependencies.add(asmCommons)

                def jsp = projectDependencies.create('javax.servlet.jsp:jsp-api:2.2')
                dependencies.add(jsp)
            }
        })

        configurations.create('vaadin-push', { conf ->
            conf.description = 'Libraries needed for using Vaadin Push features.'
            conf.defaultDependencies { dependencies ->
                if(Util.isPushSupportedAndEnabled(project)) {
                    def push = projectDependencies.create("com.vaadin:vaadin-push:${Util.getVaadinVersion(project)}")
                    dependencies.add(push)
                }
            }

            sources.compileClasspath += [conf]

            testSources.compileClasspath += [conf]
            testSources.runtimeClasspath += [conf]
        })

        configurations.create('vaadin-testbench', { conf ->
            conf.description = 'Libraries needed by Vaadin Testbench.'
            conf.defaultDependencies { dependencies ->
                if(project.vaadin.testbench.enabled) {
                    def testbench = projectDependencies.create("com.vaadin:vaadin-testbench:${project.vaadin.testbench.version}")
                    dependencies.add(testbench)
                }
            }

            testSources.compileClasspath += [conf]
            testSources.runtimeClasspath += [conf]
        })

        configurations.create('vaadin-superdevmode', { conf ->
            conf.description = 'Libraries needed by Vaadin Superdevmode.'
            conf.defaultDependencies { dependencies ->
                if(project.vaadin.devmode.superDevMode){
                    def jettyAll = projectDependencies.create( 'org.eclipse.jetty.aggregate:jetty-all-server:8.1.15.v20140411')
                    dependencies.add(jettyAll)

                    def plugin = projectDependencies.create("fi.jasoft.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}")
                    dependencies.add(plugin)

                    def asm = projectDependencies.create('org.ow2.asm:asm:5.0.3')
                    dependencies.add(asm)

                    def asmCommons = projectDependencies.create('org.ow2.asm:asm-commons:5.0.3')
                    dependencies.add(asmCommons)

                    def jsp = projectDependencies.create('javax.servlet.jsp:jsp-api:2.2')
                    dependencies.add(jsp)
                }
            }
        })
    }

}
