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
package fi.jasoft.plugin

import fi.jasoft.plugin.actions.JavaPluginAction
import fi.jasoft.plugin.actions.PluginAction
import fi.jasoft.plugin.actions.SpringBootAction
import fi.jasoft.plugin.actions.VaadinPluginAction
import fi.jasoft.plugin.actions.WarPluginAction
import fi.jasoft.plugin.configuration.TestBenchConfiguration
import fi.jasoft.plugin.configuration.TestBenchHubConfiguration
import fi.jasoft.plugin.configuration.TestBenchNodeConfiguration
import fi.jasoft.plugin.configuration.VaadinPluginExtension
import fi.jasoft.plugin.ides.EclipseUtil
import fi.jasoft.plugin.ides.IDEAUtil
import fi.jasoft.plugin.servers.ApplicationServer
import fi.jasoft.plugin.tasks.BuildClassPathJar
import fi.jasoft.plugin.tasks.BuildJavadocJarTask
import fi.jasoft.plugin.tasks.BuildSourcesJarTask
import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import fi.jasoft.plugin.tasks.CompressCssTask
import fi.jasoft.plugin.tasks.CreateAddonProjectTask
import fi.jasoft.plugin.tasks.CreateAddonThemeTask
import fi.jasoft.plugin.tasks.CreateComponentTask
import fi.jasoft.plugin.tasks.CreateCompositeTask
import fi.jasoft.plugin.tasks.CreateDesignTask
import fi.jasoft.plugin.tasks.CreateDirectoryZipTask
import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.CreateTestbenchTestTask
import fi.jasoft.plugin.tasks.CreateThemeTask
import fi.jasoft.plugin.tasks.CreateWidgetsetGeneratorTask
import fi.jasoft.plugin.tasks.DevModeTask
import fi.jasoft.plugin.tasks.DirectorySearchTask
import fi.jasoft.plugin.tasks.RunTask
import fi.jasoft.plugin.tasks.SuperDevModeTask
import fi.jasoft.plugin.tasks.UpdateAddonStylesTask
import fi.jasoft.plugin.tasks.UpdateWidgetsetTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.DependencySubstitutions
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.dsl.ArtifactHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.FileTree
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gradle Plugin for Vaadin Projects
 *
 * @author John Ahlroos
 */
class GradleVaadinPlugin implements Plugin<Project> {

    static final String PLUGIN_VERSION
    static final Properties PLUGIN_PROPERTIES
    static final String PLUGIN_DEBUG_DIR

    static int PLUGINS_IN_PROJECT = 0

    static final String CONFIGURATION_SERVER = 'vaadin-server'
    static final String CONFIGURATION_CLIENT = 'vaadin-client'
    static final String CONFIGURATION_TESTBENCH = 'vaadin-testbench'
    static final String CONFIGURATION_PUSH = 'vaadin-push'
    static final String CONFIGURATION_JAVADOC = 'vaadin-javadoc'
    static final String CONFIGURATION_SPRING_BOOT = 'vaadin-spring-boot'
    static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'
    static final String CONFIGURATION_RUN_SERVER = 'vaadin-run-server'
    static final String CONFIGURATION_SUPERDEVMODE = 'vaadin-superdevmode'
    static final String CONFIGURATION_THEME = 'vaadin-theme-compiler'
    static final String VAADIN_TASK_GROUP = 'Vaadin'
    static final String VAADIN_UTIL_TASK_GROUP = 'Vaadin Utility'
    static final String VAADIN_TESTBENCH_TASK_GROUP = 'Vaadin Testbench'
    static final String VAADIN_DIRECTORY_TASK_GROUP = 'Vaadin Directory'
    static final String ADDON_REPOSITORY_NAME = 'Vaadin addons'
    static final String VAADIN_SNAPSHOT_REPOSITORY_NAME = 'Vaadin snapshots'
    static final String GRADLE_PORTAL_PLUGIN_REPOSITORY_NAME = 'Bintray.com Maven repository'
    static final String PLUGIN_DEVELOPMENTTIME_REPOSITORY_NAME = 'Gradle Vaadin plugin development repository'
    static final String VAADIN_PRERELEASE_REPOSITORY_NAME = 'Vaadin Pre-releases'
    static final String SPRING_BOOT_PLUGIN = 'org.springframework.boot'
    static final String VALIDATION_API_1_0 = 'javax.validation:validation-api:1.0.0.GA'
    static final String PROVIDED_RUNTIME_CONFIGURATION = 'providedRuntime'

    static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1)
    static final Executor THREAD_POOL = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "gradle-vaadin-plugin-thread-${THREAD_COUNTER.getAndIncrement()}")
    })

    static {
        PLUGIN_PROPERTIES = new Properties()
        PLUGIN_PROPERTIES.load(GradleVaadinPlugin.getResourceAsStream('/vaadin_plugin.properties'))
        PLUGIN_VERSION = PLUGIN_PROPERTIES.getProperty('version')
        PLUGIN_DEBUG_DIR = PLUGIN_PROPERTIES.getProperty('debugdir')
    }

    static String getVersion() {
        PLUGIN_VERSION
    }

    static String getDebugDir() {
        PLUGIN_DEBUG_DIR
    }

    static int getNumberOfPluginsInProject() {
        PLUGINS_IN_PROJECT
    }

    static boolean isFirstPlugin() {
        PLUGINS_IN_PROJECT == 1
    }

    static String getPluginId() {
         'fi.jasoft.plugin.vaadin'
    }

    @Override
    void apply(Project project) {

        Gradle gradle = project.gradle
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = new VersionNumber(3, 0, 0, null)
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }

        PLUGINS_IN_PROJECT++

        if ( firstPlugin ) {
            project.logger.quiet("Using Gradle Vaadin Plugin $PLUGIN_VERSION")
            VersionNumber latestReleasePluginVersion = Util.getLatestReleaseVersion()
            VersionNumber pluginVersion = VersionNumber.parse(PLUGIN_VERSION)
            if(latestReleasePluginVersion > pluginVersion){
                project.logger.warn "!! A newer version of the Gradle Vaadin plugin is available, " +
                        "please upgrade to $latestReleasePluginVersion !!"
            }
        }

        // Extensions
        Util.findOrCreateExtension(project, VaadinPluginExtension, project)
        Util.findOrCreateExtension(project, TestBenchConfiguration, project)
        Util.findOrCreateExtension(project, TestBenchHubConfiguration)
        Util.findOrCreateExtension(project, TestBenchNodeConfiguration)

        // Apply plugins
        project.plugins.apply(JavaPlugin)

        // Repositories
        applyRepositories(project)

        // Dependencies
        applyDependencies(project)

        // Tasks
        applyVaadinTasks(project)
        applyVaadinUtilityTasks(project)
        applyVaadinTestbenchTasks(project)
        applyVaadinDirectoryTasks(project)

        // Configure plugins
        new VaadinPluginAction().apply(project)
        new JavaPluginAction().apply(project)
        new WarPluginAction().apply(project)
        new SpringBootAction().apply(project)

        // Cleanup plugin outputs
        def clean = project.clean
        def tasks = project.tasks
        String cleanTaskName = 'clean'
        clean.dependsOn(tasks[cleanTaskName + CompileWidgetsetTask.NAME.capitalize()])
        clean.dependsOn(tasks[cleanTaskName + RunTask.NAME.capitalize()])
        clean.dependsOn(tasks[cleanTaskName + CompileThemeTask.NAME.capitalize()])
        clean.dependsOn(tasks[cleanTaskName + CompressCssTask.NAME.capitalize()])
        clean.dependsOn(tasks[cleanTaskName + SuperDevModeTask.NAME.capitalize()])
        clean.dependsOn(tasks[cleanTaskName + DevModeTask.NAME.capitalize()])

        // Utilities
        ArtifactHandler artifacts = project.artifacts
        String archivesArtifactsName = 'archives'
        artifacts.add(archivesArtifactsName, tasks[BuildSourcesJarTask.NAME])
        artifacts.add(archivesArtifactsName, tasks[BuildJavadocJarTask.NAME])

        project.afterEvaluate { Project p ->
            String v = Util.getVaadinVersion(p)
            if ( v?.startsWith('6') ) {
                p.logger.error('Plugin no longer supports Vaadin 6, to use Vaadin 6 ' +
                        'apply an older version of the plugin.')
                throw new InvalidUserDataException('Unsupported Vaadin version.')
            }

            // Remove configurations if the plugin shouldn't manage them
            if ( !p.vaadin.manageDependencies ) {
                p.configurations.removeAll({ Configuration conf ->
                   conf.name.startsWith('vaadin-')
                })
            }
        }

        // Configure IDEA
        IDEAUtil.configureIDEAModule(project)

        // Configure Eclipse
        EclipseUtil.configureEclipsePlugin(project)
    }

    static void applyRepositories(Project project) {
        project.afterEvaluate { Project p ->
            if ( !p.vaadin.manageRepositories ) {
                return
            }

            RepositoryHandler repositories = p.repositories

            repositories.mavenCentral()
            repositories.mavenLocal()

            repositories.maven { repository ->
                repository.name = ADDON_REPOSITORY_NAME
                repository.url = 'http://maven.vaadin.com/vaadin-addons'
            }

            repositories.maven { repository ->
                repository.name = VAADIN_SNAPSHOT_REPOSITORY_NAME
                repository.url = 'http://oss.sonatype.org/content/repositories/vaadin-snapshots'
            }

            repositories.maven { repository ->
                repository.name = VAADIN_PRERELEASE_REPOSITORY_NAME
                repository.url = 'https://maven.vaadin.com/vaadin-prereleases'
            }

            repositories.maven { repository ->
                repository.name = GRADLE_PORTAL_PLUGIN_REPOSITORY_NAME
                repository.url = 'https://plugins.gradle.org/m2'
            }

            // Add plugin development repository if specified
            if ( (debugDir as File)?.exists( )
                    && !repositories.findByName(PLUGIN_DEVELOPMENTTIME_REPOSITORY_NAME)) {
                if ( GradleVaadinPlugin.firstPlugin ) {
                    project.logger.lifecycle("Using development libs found at " + debugDir)
                }
                repositories.flatDir(name:PLUGIN_DEVELOPMENTTIME_REPOSITORY_NAME, dirs:debugDir)
            }
        }
    }

    static void applyServletApi(DependencyHandler projectDependencies, DependencySet dependencies) {
        Dependency servletAPI = projectDependencies.create('javax.servlet:javax.servlet-api:3.1.0')
        dependencies.add(servletAPI)
    }

    static void applyDependencies(Project project) {
        ConfigurationContainer configurations = project.configurations
        DependencyHandler projectDependencies = project.dependencies
        def sources = project.sourceSets.main
        def testSources = project.sourceSets.test

        configurations.create(CONFIGURATION_SERVER) { conf ->
            conf.description = 'Libraries needed by Vaadin server side applications.'
            conf.defaultDependencies { dependencies ->
                Dependency vaadinServer = projectDependencies.create(
                        "com.vaadin:vaadin-server:${Util.getVaadinVersion(project)}")
                dependencies.add(vaadinServer)

                Dependency vaadinThemes = projectDependencies.create(
                        "com.vaadin:vaadin-themes:${Util.getVaadinVersion(project)}")
                dependencies.add(vaadinThemes)

                applyServletApi(projectDependencies, dependencies)
            }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_SERVER)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_SERVER)
        }

        configurations.create(CONFIGURATION_CLIENT) { conf ->
            conf.description = 'Libraries needed for compiling the widgetset.'
            conf.defaultDependencies { dependencies ->
                if ( !project.vaadinCompile.widgetsetCDN ) {
                    if ( !Util.getWidgetset(project) ) {
                        Dependency widgetsetCompiled = projectDependencies.create(
                                "com.vaadin:vaadin-client-compiled:${Util.getVaadinVersion(project)}")
                        dependencies.add(widgetsetCompiled)
                    } else {
                        Dependency vaadinClient = projectDependencies.create(
                                "com.vaadin:vaadin-client:${Util.getVaadinVersion(project)}")
                        dependencies.add(vaadinClient)

                        Dependency widgetsetCompiler = projectDependencies.create(
                                "com.vaadin:vaadin-client-compiler:${Util.getVaadinVersion(project)}")
                        dependencies.add(widgetsetCompiler)

                        Dependency validationAPI = projectDependencies.create(
                                VALIDATION_API_1_0)
                        dependencies.add(validationAPI)
                    }
                }
            }

            sources.compileClasspath += conf

            testSources.compileClasspath += conf
            testSources.runtimeClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_CLIENT)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_CLIENT, false)
        }

        configurations.create(CONFIGURATION_JAVADOC) { conf ->
            conf.description = 'Libraries for compiling JavaDoc for a Vaadin project.'
            conf.defaultDependencies { dependencies ->
                Dependency portletAPI = projectDependencies.create('javax.portlet:portlet-api:2.0')
                dependencies.add(portletAPI)

                applyServletApi(projectDependencies, dependencies)

                Dependency widgetsetCompiler = projectDependencies.create(
                        "com.vaadin:vaadin-client-compiler:${Util.getVaadinVersion(project)}")
                dependencies.add(widgetsetCompiler)

                if ( Util.isPushSupported(project) ) {
                    Dependency push = projectDependencies.create(
                            "com.vaadin:vaadin-push:${Util.getVaadinVersion(project)}")
                    dependencies.add(push)
                }
            }
        }

        configurations.create(CONFIGURATION_RUN_SERVER) { conf ->
            conf.description = 'Libraries for running the embedded server'
            conf.defaultDependencies { dependencies ->
                if( project.pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
                    // No server runner is needed, spring boot will run the project
                    return
                }

                // Needed for server runners
                Dependency plugin = projectDependencies.create(
                        "fi.jasoft.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}") {
                    transitive = false
                }

                dependencies.add(plugin)

                // Add server dependencies
                ApplicationServer.get(project).defineDependecies(projectDependencies, dependencies)
            }

            if(configurations.findByName(PROVIDED_RUNTIME_CONFIGURATION)){
                conf.extendsFrom(configurations.findByName(PROVIDED_RUNTIME_CONFIGURATION))
            }
        }

        configurations.create(CONFIGURATION_PUSH) { conf ->
            conf.description = 'Libraries needed for using Vaadin Push features.'
            conf.defaultDependencies { dependencies ->
                if ( Util.isPushSupportedAndEnabled(project) ) {
                    Dependency push = projectDependencies.create(
                            "com.vaadin:vaadin-push:${Util.getVaadinVersion(project)}")
                    dependencies.add(push)
                }
            }

            sources.compileClasspath += conf

            testSources.compileClasspath += conf
            testSources.runtimeClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_PUSH)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_PUSH)
        }

        configurations.create(CONFIGURATION_TESTBENCH) { conf ->
            conf.description = 'Libraries needed by Vaadin Testbench.'
            conf.defaultDependencies { dependencies ->
                if ( project.vaadinTestbench.enabled ) {
                    Dependency testbench = projectDependencies.create(
                            "com.vaadin:vaadin-testbench:${project.vaadinTestbench.version}")
                    dependencies.add(testbench)
                    Dependency driverManager = projectDependencies.create(
                            'io.github.bonigarcia:webdrivermanager:1.6.+')
                    dependencies.add(driverManager)
                }
            }

            testSources.compileClasspath += conf
            testSources.runtimeClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_TESTBENCH, true)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_TESTBENCH)
        }

        configurations.create(CONFIGURATION_SUPERDEVMODE) { conf ->
            conf.description = 'Libraries needed by Vaadin Superdevmode.'
            conf.defaultDependencies { dependencies ->

                Dependency jettyAll = projectDependencies.create(
                        'org.eclipse.jetty.aggregate:jetty-all-server:8.1.15.v20140411')
                dependencies.add(jettyAll)

                Dependency plugin = projectDependencies.create(
                        "fi.jasoft.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}")
                dependencies.add(plugin)

                Dependency asm = projectDependencies.create('org.ow2.asm:asm:5.0.3')
                dependencies.add(asm)

                Dependency asmCommons = projectDependencies.create('org.ow2.asm:asm-commons:5.0.3')
                dependencies.add(asmCommons)

                Dependency jsp = projectDependencies.create('javax.servlet.jsp:jsp-api:2.2')
                dependencies.add(jsp)
            }
        }

        configurations.create(CONFIGURATION_THEME) { conf ->
            conf.description = 'Libraries needed for SASS theme compilation'
            conf.defaultDependencies { dependencies ->
                FileTree themes = project.fileTree(
                        dir: Util.getThemesDirectory(project).canonicalPath,
                        include: '**/styles.scss')

                if ( !themes.isEmpty() ) {
                    switch (project.vaadinThemeCompile.compiler) {
                        case 'vaadin':
                            if(Util.isThemeDependencyNeeded(project)) {
                                VersionNumber version = VersionNumber.parse(Util.getResolvedVaadinVersion(project))
                                if(version.major == 7) {
                                    Dependency themeCompiler = projectDependencies.create(
                                            "com.vaadin:vaadin-theme-compiler:${Util.getVaadinVersion(project)}")
                                    dependencies.add(themeCompiler)
                                } else {
                                    Dependency themeCompiler = projectDependencies.create(
                                            "com.vaadin:vaadin-sass-compiler:+")
                                    dependencies.add(themeCompiler)
                                }
                            }
                            break
                        case 'compass':
                            Dependency jruby = projectDependencies.create('org.jruby:jruby-complete:1.7.3')
                            dependencies.add(jruby)
                            break
                        case 'libsass':
                            Dependency libsass = projectDependencies.create('io.bit3:jsass:5.3.0')
                            dependencies.add(libsass)

                            Dependency plugin = projectDependencies.create(
                                    "fi.jasoft.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}")
                            dependencies.add(plugin)
                            break
                        default:
                            throw new GradleException(
                                    "Selected theme compiler \"${project.vaadinThemeCompile.compiler}\" is not valid")
                    }
                } else {
                    project.logger.warn("No themes were found in themes directory $themes, " +
                            "skipping theme compiler dependencies")
                }
            }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_THEME)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_THEME)
        }

        configurations.create(CONFIGURATION_SPRING_BOOT) { conf ->
            conf.description = 'Libraries needed when running with Spring Boot'
            conf.defaultDependencies { dependencies ->
                if(project.pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)){
                    Dependency springBootStarter = projectDependencies.create(
                            'com.vaadin:vaadin-spring-boot-starter:2.0-SNAPSHOT')
                    dependencies.add(springBootStarter)
                }
            }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf

            IDEAUtil.addConfigurationToProject(project, CONFIGURATION_SPRING_BOOT)
            EclipseUtil.addConfigurationToProject(project, CONFIGURATION_SPRING_BOOT)
        }

        // Ensure vaadin version is correct across configurations
        project.configurations.all { config ->
            configureResolutionStrategy(project, config)
        }
    }

    /**
     * Configures the resolution strategy for a configuration. Ensures Vaadin version is the correct one.
     *
     * @param project
     *      The project of the configuration
     * @param configuration
     *      The configuration
     */
    static void configureResolutionStrategy(Project project, Configuration config) {
        // Ensure validation-api uses the correct version
        if ( config.name == CONFIGURATION_CLIENT) {
            config.resolutionStrategy.dependencySubstitution({ DependencySubstitutions substitutions ->
                substitutions.substitute(
                        substitutions.module('javax.validation:validation-api')
                ).with(
                        substitutions.module(VALIDATION_API_1_0)
                )
            } as Action<DependencySubstitutions>)
        }

        config.resolutionStrategy.eachDependency({ DependencyResolveDetails details ->
            def whitelist = [
                    'com.vaadin:vaadin-client',
                    'com.vaadin:vaadin-client-compiled',
                    'com.vaadin:vaadin-client-compiler',
                    'com.vaadin:vaadin-server',
                    'com.vaadin:vaadin-shared',
                    'com.vaadin:vaadin-themes',
                    'com.vaadin:vaadin-push',
                    'com.vaadin:vaadin-compatibility-client',
                    'com.vaadin:vaadin-compatibility-client-compiled',
                    'com.vaadin:vaadin-compatibility-server',
                    'com.vaadin:vaadin-compatibility-shared',
                    'com.vaadin:vaadin-compatibility-themes'
            ]

            ModuleVersionSelector dependency = details.requested
            String group = dependency.group
            String name = dependency.name

            if ( "$group:$name".toString() in whitelist ) {
                details.useVersion Util.getVaadinVersion(project)
            }
        } as Action<DependencyResolveDetails>)
    }

    static void applyVaadinTasks(Project project) {
        TaskContainer tasks = project.tasks
        tasks.create(name:CreateProjectTask.NAME, type:CreateProjectTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateAddonProjectTask.NAME, type:CreateAddonProjectTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateComponentTask.NAME, type:CreateComponentTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateCompositeTask.NAME, type:CreateCompositeTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateThemeTask.NAME, type:CreateThemeTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateWidgetsetGeneratorTask.NAME, type:CreateWidgetsetGeneratorTask,
                group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateDesignTask.NAME, type:CreateDesignTask, group:VAADIN_TASK_GROUP)

        tasks.create(name:CompileWidgetsetTask.NAME, type:CompileWidgetsetTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:DevModeTask.NAME, type:DevModeTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:SuperDevModeTask.NAME, type:SuperDevModeTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CompileThemeTask.NAME, type:CompileThemeTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CompressCssTask.NAME, type:CompressCssTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:RunTask.NAME, type:RunTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:UpdateWidgetsetTask.NAME, type:UpdateWidgetsetTask, group:VAADIN_TASK_GROUP)

        tasks.create(name:UpdateAddonStylesTask.NAME, type:UpdateAddonStylesTask, group:VAADIN_TASK_GROUP)
        tasks.create(name:CreateAddonThemeTask.NAME, type:CreateAddonThemeTask, group:VAADIN_TASK_GROUP)
    }

    static void applyVaadinUtilityTasks(Project project) {
        TaskContainer tasks = project.tasks
        tasks.create(name:BuildSourcesJarTask.NAME, type:BuildSourcesJarTask, group:VAADIN_UTIL_TASK_GROUP)
        tasks.create(name:BuildJavadocJarTask.NAME, type:BuildJavadocJarTask, group:VAADIN_UTIL_TASK_GROUP)
        tasks.create(name:BuildClassPathJar.NAME, type:BuildClassPathJar, group:VAADIN_UTIL_TASK_GROUP)
    }

    static void applyVaadinTestbenchTasks(Project project) {
        TaskContainer tasks = project.tasks
        tasks.create(name:CreateTestbenchTestTask.NAME, type:CreateTestbenchTestTask,
                group:VAADIN_TESTBENCH_TASK_GROUP)
    }

    static void applyVaadinDirectoryTasks(Project project) {
        TaskContainer tasks = project.tasks
        tasks.create(name:DirectorySearchTask.NAME, type:DirectorySearchTask,
                group:VAADIN_DIRECTORY_TASK_GROUP)
        tasks.create(name:CreateDirectoryZipTask.NAME, type:CreateDirectoryZipTask,
                group:VAADIN_DIRECTORY_TASK_GROUP)
    }
}
