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

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.util.VersionNumber

class DependencyListener implements ProjectEvaluationListener {

    /**
     * Added configurations to project
     */
    static enum Configuration {
        SERVER('vaadin', 'Vaadin server side libraries'),
        CLIENT('vaadin-client', 'Client side libraries used by Vaadin to compile the widgetset'),
        TESTBENCH('vaadin-testbench', 'Testing libraries needed by Vaadin Testbench.'),
        @Deprecated JETTY8('jetty8', 'Jetty 8 server used by the GWT CodeServer'),
        JETTY9('jetty9', 'Jetty 9 server used by the vaadinRun task'),
        PUSH('vaadin-push', 'Libraries used by vaadin push'),
        JAVADOC('vaadin-javadoc', 'Classpath for compiling Javadoc for Vaadin projects'),
        GROOVY('vaadin-groovy', 'Groovy library for vaadin projects')

        def String caption
        def String description;

        public Configuration(String caption, String description) {
            this.caption = caption
            this.description = description
        }
    }

    /**
     * Added repositories to project
     */
    static enum Repositories {
        ADDONS('Vaadin addons', 'http://maven.vaadin.com/vaadin-addons'),
        SNAPSHOTS('Vaadin snapshots', 'http://oss.sonatype.org/content/repositories/vaadin-snapshots'),
        JASOFT('Jasoft.fi Maven repository', 'http://mvn.jasoft.fi/maven2'),
        BINTRAY('Bintray.com Maven repository', 'http://dl.bintray.com/johndevs/maven')

        def String caption
        def String url

        Repositories(String caption, String url) {
            this.caption = caption;
            this.url = url
        }
    }

    void beforeEvaluate(Project project) {

        // Check to see if we are using the eclipse plugin instead of the eclipse-wtp plugin
        if (project.plugins.findPlugin('eclipse') && !project.plugins.findPlugin('eclipse-wtp')) {
            project.getLogger().warn("You are using the eclipse plugin which does not support all " +
                    "features of the Vaadin plugin. Please use the eclipse-wtp plugin instead.")
        }
    }

    void afterEvaluate(Project project, ProjectState state) {

        if (!project.hasProperty('vaadin') || !project.vaadin.manageDependencies) {
            return
        }

        String version = project.vaadin.version

        if(version !=null && version.startsWith("6")){
            project.logger.error("Plugin no longer supports Vaadin 6, to use Vaadin 6 apply an older version of the plugin.")
            throw new InvalidUserDataException("Unsupported Vaadin version.")
        }

        // Add repositories unless specified otherwise
        if (project.vaadin.manageRepositories) {
            addRepositories(project)
        }

        createJetty9Configuration(project)

        if(project.vaadin.devmode.superDevMode) {
            // To fix #125
            createJetty8Configuration(project)
        }

        createVaadin7Configuration(project, version)

        createJavadocConfiguration(project, version)

        if (project.vaadin.testbench.enabled) {
            createTestbenchConfiguration(project)
        }

        // Assign resolution strategy so vaadin version is kept in sync
        project.configurations.all { config ->
            configureResolutionStrategy(project, config)
        }
    }

    def static addRepositories(Project project) {
        def repositories = project.repositories

        // Ensure maven central and maven local are included
        repositories.mavenCentral()
        repositories.mavenLocal()

        // Add repositories
        Repositories.values().each { repository ->
            if (repositories.findByName(repository.caption) == null) {
                repositories.maven({
                    name = repository.caption
                    url = repository.url
                })
            }
        }

        // Add plugin development repository if specified
        if (new File(GradleVaadinPlugin.getDebugDir()).exists()
                && repositories.findByName('Gradle Vaadin plugin development repository') == null) {

            if (GradleVaadinPlugin.isFirstPlugin()) {
                project.logger.lifecycle("Using development libs found at " + GradleVaadinPlugin.getDebugDir())
            }

            repositories.flatDir(name: 'Gradle Vaadin plugin development repository', dirs: GradleVaadinPlugin.getDebugDir())
        }
    }

    def static createJetty9Configuration(Project project) {
        def conf = createConfiguration(project, Configuration.JETTY9, [
                'org.eclipse.jetty.aggregate:jetty-all:9.2.2.v20140723',
                'org.eclipse.jetty:jetty-annotations:9.2.2.v20140723',
                'org.eclipse.jetty:jetty-plus:9.2.2.v20140723',
                'org.eclipse.jetty:jetty-deploy:9.2.2.v20140723',
                'fi.jasoft.plugin:gradle-vaadin-plugin:' + GradleVaadinPlugin.getVersion(),
                'org.ow2.asm:asm:5.0.3',
                'org.ow2.asm:asm-commons:5.0.3',
                'javax.servlet.jsp:jsp-api:2.2'
        ])

        def sources = project.sourceSets.main
        sources.compileClasspath += [conf]

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }

    /**
     * Creates a new configuration with dependencies
     *
     * @param project
     *      The project to add the configuration to
     * @param conf
     *      The configuration enum
     * @param dependencies
     *      The dependencies of the configuration in string notation
     * @param extendsFrom
     *      Should the configuration extend another configuration(s)
     * @return
     *      The created configuration
     */
    def static org.gradle.api.artifacts.Configuration createConfiguration(Project project,
                                       Configuration conf,
                                       List<String> dependencies,
                                       Iterable<org.gradle.api.artifacts.Configuration> extendsFrom=null) {

       def org.gradle.api.artifacts.Configuration configuration

       if(extendsFrom){
           configuration = project.configurations.maybeCreate(conf.caption).setExtendsFrom(extendsFrom as Set)
       } else {
           configuration = project.configurations.maybeCreate(conf.caption)
       }

       configuration.description = conf.description

       dependencies.each { dependency ->
           project.dependencies.add(conf.caption, dependency)
       }

       configuration
    }

    /**
     * Configures the resolution strategy for a configuration. Ensures Vaadin version is the correct one.
     *
     * @param project
     *      The project of the configuration
     * @param configuration
     *      The configuration
     */
    def static configureResolutionStrategy(Project project, org.gradle.api.artifacts.Configuration config) {
        final List whitelist = [
                'com.vaadin:vaadin-client',
                'com.vaadin:vaadin-client-compiled',
                'com.vaadin:vaadin-client-compiler',
                'com.vaadin:vaadin-server',
                'com.vaadin:vaadin-shared',
                'com.vaadin:vaadin-themes',
                'com.vaadin:vaadin-push'
        ]

        config.resolutionStrategy.eachDependency(new Action<DependencyResolveDetails>() {
            @Override
            void execute(DependencyResolveDetails details) {
                def dependency = details.requested
                String group = dependency.group
                String name = dependency.name
                if("$group:$name".toString() in whitelist){
                    details.useVersion project.vaadin.version
                }
            }
        })
    }

    @Deprecated
    def static createJetty8Configuration(Project project) {
        createConfiguration(project, Configuration.JETTY8, [
                'org.eclipse.jetty.aggregate:jetty-all-server:8.1.15.v20140411',
                'fi.jasoft.plugin:gradle-vaadin-plugin:' + GradleVaadinPlugin.getVersion(),
                'org.ow2.asm:asm:5.0.3',
                'org.ow2.asm:asm-commons:5.0.3',
                'javax.servlet.jsp:jsp-api:2.2'
        ], [ project.configurations.runtime ])
    }

    def static createServerConfiguration(Project project) {
        def conf = createConfiguration(project, Configuration.SERVER, [], [ project.configurations.compile])

        def sources = project.sourceSets.main
        sources.compileClasspath += [conf]

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }

    /**
     * Creates the configuration for generating Javadoc
     */
    private static void createJavadocConfiguration(Project project, String version) {
        createConfiguration(project, Configuration.JAVADOC, [
                'javax.portlet:portlet-api:2.0',
                'javax.servlet:javax.servlet-api:3.0.1'
        ])

        if(Util.isPushSupported(project)){
            project.dependencies.add(Configuration.JAVADOC.caption, "com.vaadin:vaadin-push:${version}")
        }
    }

    private static void createVaadin7Configuration(Project project, String version) {

        createServerConfiguration(project)

        createClientConfiguration(project)

        if (Util.isPushSupportedAndEnabled(project)) {
            createPushConfiguration(project, version)
        }

        def serverConf = Configuration.SERVER
        def clientConf = Configuration.CLIENT
        def dependencies = project.dependencies

        // Theme compiler
        if(!Util.isSassCompilerSupported(project)){
            File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
            FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
            if (!themes.isEmpty()) {
                dependencies.add(serverConf.caption, "com.vaadin:vaadin-theme-compiler:${version}")
            }
        }

        // Client compiler or pre-compiled theme
        if (project.vaadin.widgetset == null) {
            dependencies.add(serverConf.caption, "com.vaadin:vaadin-client-compiled:${version}")
        } else {
            dependencies.add(clientConf.caption, "com.vaadin:vaadin-client-compiler:${version}", {

                // Project already has jetty, no need for it to be included again
                exclude([group: 'org.mortbay.jetty'])  // pre 7.2.2
                exclude([group: 'org.eclipse.jetty'])
            })

            dependencies.add(clientConf.caption, "com.vaadin:vaadin-client:${version}")
            dependencies.add(clientConf.caption, "javax.validation:validation-api:1.0.0.GA")
        }

        // Server
        dependencies.add(serverConf.caption, "com.vaadin:vaadin-server:${version}")

        // Themes
        dependencies.add(serverConf.caption, "com.vaadin:vaadin-themes:${version}")
    }

    private static void createClientConfiguration(Project project) {
        def conf = createConfiguration(project, Configuration.CLIENT, [], [project.configurations.compile])

        def sources = project.sourceSets.main
        sources.compileClasspath += [conf]

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }

    private static void createTestbenchConfiguration(Project project) {
        def conf = createConfiguration(project, Configuration.TESTBENCH, [
           "com.vaadin:vaadin-testbench:${project.vaadin.testbench.version}"
        ], [project.configurations.testCompile])

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }

    private static void createPushConfiguration(Project project, String version) {
        def conf = createConfiguration(project, Configuration.PUSH, [
                "com.vaadin:vaadin-push:${version}"
        ], [project.configurations.compile, project.configurations.runtime])

        def sources = project.sourceSets.main
        sources.compileClasspath += [conf]

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }
}
