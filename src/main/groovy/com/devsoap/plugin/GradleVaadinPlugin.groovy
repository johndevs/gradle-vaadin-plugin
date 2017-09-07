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
package com.devsoap.plugin

import com.devsoap.plugin.actions.EclipsePluginAction
import com.devsoap.plugin.actions.EclipseWtpPluginAction
import com.devsoap.plugin.actions.GrettyAction
import com.devsoap.plugin.actions.IdeaPluginAction
import com.devsoap.plugin.actions.JavaPluginAction
import com.devsoap.plugin.actions.SpringBootAction
import com.devsoap.plugin.actions.VaadinPluginAction
import com.devsoap.plugin.actions.WarPluginAction
import com.devsoap.plugin.extensions.SpringBootExtension
import com.devsoap.plugin.extensions.TestBenchExtension
import com.devsoap.plugin.extensions.TestBenchHubExtension
import com.devsoap.plugin.extensions.TestBenchNodeExtension
import com.devsoap.plugin.extensions.AddonExtension
import com.devsoap.plugin.extensions.VaadinPluginExtension
import com.devsoap.plugin.extensions.WidgetsetCDNExtension
import com.devsoap.plugin.servers.ApplicationServer
import com.devsoap.plugin.tasks.BuildClassPathJar
import com.devsoap.plugin.tasks.BuildJavadocJarTask
import com.devsoap.plugin.tasks.BuildSourcesJarTask
import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CompileWidgetsetTask
import com.devsoap.plugin.tasks.CompressCssTask
import com.devsoap.plugin.tasks.CreateAddonProjectTask
import com.devsoap.plugin.tasks.CreateAddonThemeTask
import com.devsoap.plugin.tasks.CreateComponentTask
import com.devsoap.plugin.tasks.CreateCompositeTask
import com.devsoap.plugin.tasks.CreateDesignTask
import com.devsoap.plugin.tasks.CreateDirectoryZipTask
import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.CreateTestbenchTestTask
import com.devsoap.plugin.tasks.CreateThemeTask
import com.devsoap.plugin.tasks.CreateWidgetsetGeneratorTask
import com.devsoap.plugin.tasks.DevModeTask
import com.devsoap.plugin.tasks.DirectorySearchTask
import com.devsoap.plugin.tasks.RunTask
import com.devsoap.plugin.tasks.SuperDevModeTask
import com.devsoap.plugin.tasks.UpdateAddonStylesTask
import com.devsoap.plugin.tasks.UpdateWidgetsetTask
import com.devsoap.plugin.tasks.VersionCheckTask

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
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.WarPlugin
import org.gradle.tooling.UnsupportedVersionException
import org.gradle.util.VersionNumber

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Gradle Plugin for Vaadin Projects
 *
 * @author John Ahlroos
 * @since 1.0
 */
class GradleVaadinPlugin implements Plugin<Project> {

    /**
     * The configuration name for Vaadin server side dependencies
     */
    static final String CONFIGURATION_SERVER = 'vaadin-server'

    /**
     * The configuration name for Vaadin client side dependencies
     */
    static final String CONFIGURATION_CLIENT = 'vaadin-client'

    /**
     * The configuration name for Vaadin Testbench dependencies
     */
    static final String CONFIGURATION_TESTBENCH = 'vaadin-testbench'

    /**
     * The configuration name for Vaadin push dependencies
     */
    static final String CONFIGURATION_PUSH = 'vaadin-push'

    /**
     * The configuration name for Vaadin javadoc dependencies
     */
    static final String CONFIGURATION_JAVADOC = 'vaadin-javadoc'

    /**
     * The configuration name for Vaadin Spring Boot dependencies
     */
    static final String CONFIGURATION_SPRING_BOOT = 'vaadin-spring-boot'

    /**
     * The default widgetset name
     */
    static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'

    /**
     * The configuration name for Vaadin Run dependencies
     */
    static final String CONFIGURATION_RUN_SERVER = 'vaadin-run-server'

    /**
     * The configuration name for Vaadin theme compiler dependencies
     */
    static final String CONFIGURATION_THEME = 'vaadin-theme-compiler'

    /**
     * The task group name for Vaadin generic tasks
     */
    static final String VAADIN_TASK_GROUP = 'Vaadin'

    /**
     * The task group name for Vaadin utility tasks
     */
    static final String VAADIN_UTIL_TASK_GROUP = 'Vaadin Utility'

    /**
     * The task group name for Vaadin Testbench tasks
     */
    static final String VAADIN_TESTBENCH_TASK_GROUP = 'Vaadin Testbench'

    /**
     * The task group name for Vaadin Directory tasks
     */
    static final String VAADIN_DIRECTORY_TASK_GROUP = 'Vaadin Directory'

    /**
     * The plugin id of the Spring Boot plugin
     */
    static final String SPRING_BOOT_PLUGIN = 'org.springframework.boot'

    private static final String VALIDATION_API_VERSION = 'validation.api.version'
    private static final String ADDON_REPOSITORY_NAME = 'Vaadin addons'
    private static final String VAADIN_SNAPSHOT_REPOSITORY_NAME = 'Vaadin snapshots'
    private static final String GRADLE_PORTAL_PLUGIN_REPOSITORY_NAME = 'Bintray.com Maven repository'
    private static final String PLUGIN_DEVELOPMENTTIME_REPOSITORY_NAME = 'Gradle Vaadin plugin development repository'
    private static final String VAADIN_PRERELEASE_REPOSITORY_NAME = 'Vaadin Pre-releases'
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1)
    private static final String PLUGIN_VERSION

    /**
     * The common thread pool for all Gradle Vaadin plugin threads
     */
    static final Executor THREAD_POOL = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "gradle-vaadin-plugin-thread-${THREAD_COUNTER.getAndIncrement()}")
    })


    private static final Properties PLUGIN_PROPERTIES
    private static final String PLUGIN_DEBUG_DIR
    static {
        PLUGIN_PROPERTIES = new Properties()
        PLUGIN_PROPERTIES.load(GradleVaadinPlugin.getResourceAsStream('/vaadin_plugin.properties'))
        PLUGIN_VERSION = PLUGIN_PROPERTIES.getProperty('version')
        PLUGIN_DEBUG_DIR = PLUGIN_PROPERTIES.getProperty('debugdir')
    }

    /**
     * Get the plugin version
     */
    static String getVersion() {
        PLUGIN_VERSION
    }

    /**
     * Get the directory for the development plugin versions
     */
    static String getDebugDir() {
        PLUGIN_DEBUG_DIR
    }

    /**
     * Get the plugin id of this plugin
     */
    static String getPluginId() {
         'com.devsoap.plugin.vaadin'
    }

    @Override
    void apply(Project project) {

        Gradle gradle = project.gradle
        VersionNumber version = VersionNumber.parse(gradle.gradleVersion)
        VersionNumber requiredVersion = new VersionNumber(4, 0, 0, null)
        if ( version.baseVersion < requiredVersion ) {
            throw new UnsupportedVersionException("Your gradle version ($version) is too old. " +
                    "Plugin requires Gradle $requiredVersion+")
        }

        // Add version check as first task
        if(gradle.startParameter.taskNames.find { it.contains('vaadin')} &&
            !gradle.startParameter.taskNames.find { it.contains(VersionCheckTask.NAME)}){
            if(gradle.rootProject == project) {
                gradle.startParameter.taskNames = gradle.startParameter.taskNames.plus(0,
                        ":$VersionCheckTask.NAME".toString())
            } else {
                gradle.startParameter.taskNames = gradle.startParameter.taskNames.plus(0,
                        "$project.name:$VersionCheckTask.NAME".toString())
            }
        }

        // Ensure the build dir exists as all external processes will be run inside that directory
        project.buildDir.mkdirs()

        // Extensions (must be added before any action)
        project.extensions.create(VaadinPluginExtension.NAME, VaadinPluginExtension, project)
        project.extensions.create(WidgetsetCDNExtension.NAME, WidgetsetCDNExtension, project)
        project.extensions.create(AddonExtension.NAME, AddonExtension, project)
        project.extensions.create(TestBenchExtension.NAME, TestBenchExtension, project)
        project.extensions.create(TestBenchHubExtension.NAME, TestBenchHubExtension, project)
        project.extensions.create(TestBenchNodeExtension.NAME, TestBenchNodeExtension, project)
        project.extensions.create(SpringBootExtension.NAME, SpringBootExtension, project)

        // Configure plugins
        new JavaPluginAction().apply(project)
        new WarPluginAction().apply(project)
        new VaadinPluginAction().apply(project)

        // Repositories
        applyRepositories(project)

        // Dependencies
        applyDependencies(project)

        // Tasks
        applyVaadinTasks(project)
        applyVaadinUtilityTasks(project)
        applyVaadinTestbenchTasks(project)
        applyVaadinDirectoryTasks(project)

        // Configure plugins that depends on tasks
        new SpringBootAction().apply(project)
        new GrettyAction().apply(project)
        new IdeaPluginAction().apply(project)
        new EclipsePluginAction().apply(project)
        new EclipseWtpPluginAction().apply(project)

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
    }

    private static void applyRepositories(Project project) {
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
                project.logger.lifecycle("Using development libs found at " + debugDir)
                repositories.flatDir(name:PLUGIN_DEVELOPMENTTIME_REPOSITORY_NAME, dirs:debugDir)
            }
        }
    }

    private static void applyServletApi(DependencyHandler projectDependencies, DependencySet dependencies) {
        Dependency servletAPI = projectDependencies.create(
                "javax.servlet:javax.servlet-api:${Util.pluginProperties.getProperty('servlet.version')}")
        dependencies.add(servletAPI)
    }

    private static void applyDependencies(Project project) {
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
                              "javax.validation:validation-api:${Util.pluginProperties.get(VALIDATION_API_VERSION)}")
                        dependencies.add(validationAPI)
                    }
                }
            }

            sources.compileClasspath += conf

            testSources.compileClasspath += conf
            testSources.runtimeClasspath += conf
        }

        configurations.create(CONFIGURATION_JAVADOC) { conf ->
            conf.description = 'Libraries for compiling JavaDoc for a Vaadin project.'
            conf.defaultDependencies { dependencies ->
                Dependency portletAPI = projectDependencies.create(
                        "javax.portlet:portlet-api:${Util.pluginProperties.get('portlet.version')}")
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
                if(SpringBootAction.isSpringBootPresent(project)) {
                    // No server runner is needed, spring boot will run the project
                    return
                }

                if(GrettyAction.isGrettyPresent(project)) {
                    // No server runner is needed, gretty will run the project
                    return
                }

                // Needed for server runners
                Dependency plugin = projectDependencies.create(
                        "com.devsoap.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}") {
                    transitive = false
                }

                dependencies.add(plugin)

                // Add server dependencies
                ApplicationServer.get(project, [:]).defineDependecies(projectDependencies, dependencies)

                // Add spring-reloaded for hotswapping
                Dependency springLoaded = projectDependencies.create(
                        "org.springframework:springloaded:${Util.pluginProperties.get('spring.loaded.version')}")
                dependencies.add(springLoaded)

                if(configurations.findByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME)){
                    conf.extendsFrom(configurations.findByName(WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME))
                }
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
        }

        configurations.create(CONFIGURATION_TESTBENCH) { conf ->
            conf.description = 'Libraries needed by Vaadin Testbench.'
            conf.defaultDependencies { dependencies ->
                if ( project.vaadinTestbench.enabled ) {
                    Dependency testbench = projectDependencies.create(
                            "com.vaadin:vaadin-testbench:${project.vaadinTestbench.version}")
                    dependencies.add(testbench)
                    Dependency driverManager = projectDependencies.create(
               "io.github.bonigarcia:webdrivermanager:${Util.pluginProperties.getProperty('webdrivermanager.version')}")
                    dependencies.add(driverManager)
                }
            }

            testSources.compileClasspath += conf
            testSources.runtimeClasspath += conf
        }

        configurations.create(CONFIGURATION_THEME) { conf ->
            conf.description = 'Libraries needed for SASS theme compilation'
            conf.defaultDependencies { dependencies ->
                CompileThemeTask themeConf = project.tasks.getByName(CompileThemeTask.NAME)
                switch (themeConf.compiler) {
                    case 'vaadin':
                        if(Util.isThemeDependencyNeeded(project)) {
                            VersionNumber version = VersionNumber.parse(Util.getResolvedVaadinVersion(project))
                            if (version.major == 7) {
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
                            Dependency jruby = projectDependencies.create(
                                    "org.jruby:jruby-complete:${Util.pluginProperties.getProperty('jruby.version')}")
                            dependencies.add(jruby)
                        break
                        case 'libsass':
                            Dependency libsass = projectDependencies.create(
                                    "io.bit3:jsass:${Util.pluginProperties.getProperty('jsass.version')}")
                            dependencies.add(libsass)

                            Dependency plugin = projectDependencies.create(
                                    "com.devsoap.plugin:gradle-vaadin-plugin:${GradleVaadinPlugin.version}")
                            dependencies.add(plugin)
                        break
                        default:
                            throw new GradleException(
                                    "Selected theme compiler \"${project.vaadinThemeCompile.compiler}\" is not valid")
                    }
                }

            sources.compileClasspath += conf
            testSources.compileClasspath += conf
        }

        configurations.create(CONFIGURATION_SPRING_BOOT) { conf ->
            conf.description = 'Libraries needed when running with Spring Boot'
            conf.defaultDependencies { dependencies ->
                if(SpringBootAction.isSpringBootPresent(project)){
                    SpringBootExtension extension = project.extensions.getByType(SpringBootExtension)
                    Dependency springBootStarter = projectDependencies.create(
                            "com.vaadin:vaadin-spring-boot-starter:${extension.starterVersion}")
                    dependencies.add(springBootStarter)

                    // Needed so bootRepackage can include all dependencies in Jar
                    conf.extendsFrom(
                            project.configurations['compile'],
                            project.configurations['runtime'],
                            project.configurations[CONFIGURATION_PUSH]
                    )
                }
            }
            sources.compileClasspath += conf
            testSources.compileClasspath += conf
        }

        // Ensure vaadin version is correct across configurations
        project.configurations.all { config ->
            configureResolutionStrategy(project, config)
        }
    }

    /*
     * Configures the resolution strategy for a configuration. Ensures Vaadin version is the correct one.
     */
    private static void configureResolutionStrategy(Project project, Configuration config) {
        // Ensure validation-api uses the correct version
        if ( config.name == CONFIGURATION_CLIENT) {
            config.resolutionStrategy.dependencySubstitution({ DependencySubstitutions substitutions ->
                substitutions.substitute(
                        substitutions.module('javax.validation:validation-api')
                ).with(
                        substitutions.module(
                          "javax.validation:validation-api:${Util.pluginProperties.get(VALIDATION_API_VERSION)}")
                )
            } as Action<DependencySubstitutions>)
        }

        config.resolutionStrategy.eachDependency({ DependencyResolveDetails details ->
            List<String> whitelist = [
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

    private static void applyVaadinTasks(Project project) {
        addTask(project, CreateProjectTask.NAME, CreateProjectTask, VAADIN_TASK_GROUP)
        addTask(project, CreateAddonProjectTask.NAME, CreateAddonProjectTask, VAADIN_TASK_GROUP)
        addTask(project, CreateComponentTask.NAME, CreateComponentTask, VAADIN_TASK_GROUP)
        addTask(project, CreateCompositeTask.NAME, CreateCompositeTask, VAADIN_TASK_GROUP)
        addTask(project, CreateThemeTask.NAME, CreateThemeTask, VAADIN_TASK_GROUP)
        addTask(project, CreateWidgetsetGeneratorTask.NAME, CreateWidgetsetGeneratorTask,VAADIN_TASK_GROUP)
        addTask(project, CreateDesignTask.NAME, CreateDesignTask, VAADIN_TASK_GROUP)

        addTask(project, CompileWidgetsetTask.NAME, CompileWidgetsetTask, VAADIN_TASK_GROUP) {
            CompileWidgetsetTask task ->
            WidgetsetCDNExtension cdnExtension = project.extensions.getByType(WidgetsetCDNExtension)
            task.proxyEnabled = cdnExtension.proxyEnabledProvider
            task.proxyPort = cdnExtension.proxyPortProvider
            task.proxyScheme = cdnExtension.proxySchemeProvider
            task.proxyHost = cdnExtension.proxyHostProvider
            task.proxyAuth = cdnExtension.proxyAuthProvider
        }

        addTask(project, DevModeTask.NAME, DevModeTask, VAADIN_TASK_GROUP)
        addTask(project, SuperDevModeTask.NAME, SuperDevModeTask, VAADIN_TASK_GROUP)
        addTask(project, CompileThemeTask.NAME, CompileThemeTask, VAADIN_TASK_GROUP) { CompileThemeTask task ->
            task.useClasspathJar = project.extensions.getByType(VaadinPluginExtension).useClassPathJarProvider
        }
        addTask(project, CompressCssTask.NAME, CompressCssTask, VAADIN_TASK_GROUP)
        addTask(project, RunTask.NAME, RunTask, VAADIN_TASK_GROUP)
        addTask(project, UpdateWidgetsetTask.NAME, UpdateWidgetsetTask, VAADIN_TASK_GROUP)

        addTask(project, UpdateAddonStylesTask.NAME, UpdateAddonStylesTask, VAADIN_TASK_GROUP)
        addTask(project, CreateAddonThemeTask.NAME, CreateAddonThemeTask, VAADIN_TASK_GROUP) {
            CreateAddonThemeTask task -> task.addonTitle = project.extensions.getByType(AddonExtension).titleProvider
        }
    }

    private static void applyVaadinUtilityTasks(Project project) {
        addTask(project, BuildSourcesJarTask.NAME, BuildSourcesJarTask, VAADIN_UTIL_TASK_GROUP)
        addTask(project, BuildJavadocJarTask.NAME, BuildJavadocJarTask, VAADIN_UTIL_TASK_GROUP)
        addTask(project, BuildClassPathJar.NAME, BuildClassPathJar, VAADIN_UTIL_TASK_GROUP) { BuildClassPathJar task ->
            task.useClassPathJar = project.extensions.getByType(VaadinPluginExtension).useClassPathJarProvider
        }
        addTask(project, VersionCheckTask.NAME, VersionCheckTask, VAADIN_UTIL_TASK_GROUP)
    }

    private static void applyVaadinTestbenchTasks(Project project) {
        addTask(project, CreateTestbenchTestTask.NAME, CreateTestbenchTestTask,VAADIN_TESTBENCH_TASK_GROUP)
    }

    private static void applyVaadinDirectoryTasks(Project project) {
        addTask(project, DirectorySearchTask.NAME, DirectorySearchTask, VAADIN_DIRECTORY_TASK_GROUP)
        addTask(project, CreateDirectoryZipTask.NAME, CreateDirectoryZipTask, VAADIN_DIRECTORY_TASK_GROUP)
    }

    private static void addTask(Project project, String name, Class type, String group, Closure configure={}) {
        project.tasks.create(name:name, type:type, group:group, configure)
        project.clean.dependsOn(project.tasks["clean${name.capitalize()}" ])
    }
}
