package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CreateDirectoryZipTask
import org.junit.Ignore
import org.junit.Test

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Created by john on 1/6/15.
 */
class TaskConfigurationsTest extends IntegrationTest {

    @Test void 'Vaadin task action is run'() {
        String output = runWithArguments('--info', 'build')
        assertTrue output.contains('Applying VaadinPluginAction')
    }

    @Test void 'Eclipse default configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyEclipseClassPath(dependsOn: 'eclipseClasspath') {
                doLast {
                    def classpath = project.eclipse.classpath
                    println 'Download sources ' +  classpath.downloadSources
                    println 'Eclipse output dir ' + project.vaadinRun.classesDir
    
                    def confs = project.configurations
                    println 'Server in classpath ' + (confs.getByName('vaadin-server') in classpath.plusConfigurations)
                    println 'Client in classpath ' + (confs.getByName('vaadin-client') in classpath.plusConfigurations)
    
                    def natures = project.eclipse.project.natures
                    println 'Springsource nature ' + ('org.springsource.ide.eclipse.gradle.core.nature' in natures)
                }
            }    
        """.stripIndent()

        def result = runWithArguments('verifyEclipseClassPath')

        assertTrue result, result.contains( 'Download sources true')
        assertTrue result, result.contains( 'Eclipse output dir null')

        assertTrue result, result.contains( 'Server in classpath true')
        assertTrue result, result.contains( 'Client in classpath true')
      
        assertTrue result, result.contains( 'Springsource nature true')
    }

    @Test void 'Eclipse configuration with custom output dir'() {
        buildFile << """
            apply plugin: 'eclipse-wtp'

            vaadinRun.classesDir 'custom/dir'

            task verifyOutputDir(dependsOn:'eclipseClasspath') {
                doLast {
                    println project.eclipse.classpath.defaultOutputDir
                    println project.file('custom/dir')
                    println 'Default output dir is set to eclipseOutputDir ' +
                        (project.eclipse.classpath.defaultOutputDir == project.file('custom/dir'))
                }
            }    
        """.stripIndent()

        def result = runWithArguments('verifyOutputDir')
        assertTrue result, result.contains('Default output dir is set to eclipseOutputDir true')
    }

    @Test void 'Eclipse configuration with Testbench enabled'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            vaadinTestbench {
               enabled true                
            }

            task verifyTestbenchDependency(dependsOn: 'eclipseClasspath') {
                doLast {
                    def confs = project.configurations
                    def classpath = project.eclipse.classpath
                    println 'Testbench on classpath ' +
                        (confs.getByName('vaadin-testbench') in classpath.plusConfigurations)
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyTestbenchDependency')
        assertTrue result.contains('Testbench on classpath true')
    }

    @Test void 'Eclipse WTP component configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyWTP(dependsOn:eclipseWtpComponent) {
                doLast {
                    def confs = project.configurations
                    println 'Server in components ' +
                        (confs.getByName('vaadin-server') in project.eclipse.wtp.component.plusConfigurations)
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyWTP')
        assertTrue result, result.contains('Server in components true')
    }

    @Ignore("Extra facets currently disabled")
    @Test void 'Eclipse WTP facet configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyWTP(dependsOn:eclipseWtpFacet) {
                doLast {
                    def facets = project.eclipse.wtp.facet.facets
                    def JavaVersion javaVersion = project.sourceCompatibility
                    println 'Vaadin Facet version ' + (facets.find {
                        it.name=='com.vaadin.integration.eclipse.core'
                    }.version)
                    println 'jst.web Facet version ' + (facets.find { it.name=='jst.web'}.version)
                    println 'Java Facet version equals sourceCompatibility ' +
                        (javaVersion.toString() == facets.find { it.name=='java'}.version)
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyWTP')
        assertTrue result, result.contains('Vaadin Facet version 7.0')
        assertTrue result, result.contains('jst.web Facet version 3.0')
        assertTrue result, result.contains('Java Facet version equals sourceCompatibility true')
    }

    @Test void 'IDEA default configuration'() {

        buildFile << """
            apply plugin: 'idea'

            task verifyIdeaModule(dependsOn: 'ideaModule') {
                doLast {
                    def module = project.idea.module
                    println 'Module and Project name is equal ' + (project.name == module.name)
                    println 'Output dir is classes dir ' +
                        (project.sourceSets.main.output.classesDir == module.outputDir)
                    println 'Test output dir is classes dir ' +
                        (project.sourceSets.test.output.classesDir == module.testOutputDir)
    
                    println 'Download Javadoc ' + module.downloadJavadoc
                    println 'Download Sources ' + module.downloadSources
    
                    def conf = project.configurations
                    def scopes = module.scopes
                    println 'Server configuration included ' + (conf.getByName('vaadin-server') in scopes.COMPILE.plus)
                    println 'Client configuration included ' + (conf.getByName('vaadin-client') in scopes.COMPILE.plus)
                }
            }    
        """.stripIndent()

        def result = runWithArguments('verifyIdeaModule')
        assertTrue result, result.contains('Module and Project name is equal true')
        assertTrue result, result.contains('Output dir is classes dir true')
        assertTrue result, result.contains('Test output dir is classes dir true')

        assertTrue result, result.contains('Download Javadoc true')
        assertTrue result, result.contains('Download Sources true')

        assertTrue result, result.contains('Server configuration included true')
        assertTrue result, result.contains('Client configuration included true')
    }

    @Test void 'IDEA configuration with Testbench'() {

        buildFile << """
             apply plugin: 'idea'

             vaadinTestbench {
                enabled true
             }

             task verifyTestBench(dependsOn: 'ideaModule') {
                 doLast {
                    def conf = project.configurations
                    def module = project.idea.module
                    def scopes = module.scopes
                    println 'Test configuration has testbench ' + (conf.getByName('vaadin-testbench') in scopes.TEST.plus)
                 }
             }    
        """.stripIndent()

        def result = runWithArguments('verifyTestBench')

        assertTrue result, result.contains('Test configuration has testbench true')
    }

    @Test void 'IDEA configuration with push'() {

        buildFile << """
            apply plugin: 'idea'

            vaadin {
                push true
            }

            task verifyPush(dependsOn: 'ideaModule') {
                doLast {
                    def conf = project.configurations
                    def module = project.idea.module
                    def scopes = module.scopes
                    println 'Compile configuration has push ' + (conf.getByName('vaadin-push') in scopes.COMPILE.plus)
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyPush')

        assertTrue result, result.contains('Compile configuration has push true')
    }

    @Test void 'Update widgetset generator before compile'() {

        buildFile << """
             vaadinCompile {
                widgetset 'com.example.Widgetset'
                widgetsetGenerator 'com.example.WidgetsetGenerator'
             }

             task verifyWidgetsetGenerator(dependsOn:compileJava) {
                 doLast {
                    def generatorFile = file('src/main/java/com/example/WidgetsetGenerator.java')
                    println generatorFile
                    println 'Generator File was created ' + generatorFile.exists()
                 }
             }    
        """.stripIndent()

        def result = runWithArguments('verifyWidgetsetGenerator')

        assertTrue result, result.contains('Generator File was created true')
    }

    @Test void 'Compile with a widgetset generator'() {
        buildFile << """
             vaadinCompile {
                widgetset 'com.example.Widgetset'
                widgetsetGenerator 'com.example.WidgetsetGenerator'
             }
        """.stripIndent()

        runWithArguments('vaadinCreateWidgetsetGenerator')

        String result = runWithArguments('vaadinCompile')
        assertTrue result, result.contains('BUILD SUCCESSFUL')
    }

    @Test void 'Fail if widgetset generator in client package'() {
        buildFile << """
             vaadinCompile {
                widgetset 'com.example.Widgetset'
                widgetsetGenerator 'com.example.client.WidgetsetGenerator'
             }
        """.stripIndent()

        String result = runFailureExpected('vaadinCreateWidgetsetGenerator')
        assertTrue result, result.contains('Widgetset generator cannot be placed inside the client package')
    }

    @Test void 'Addon Jar Metadata'() {

        buildFile << """
            version '1.2.3'
            
            vaadinCompile {
                widgetset 'com.example.Widgetset'
            }
            
            vaadinAddon {
                title 'test-addon'
                license 'my-license'
                author 'test-author'
            }

            task verifyAddonJarManifest(dependsOn: 'jar') {
                doLast {
                    def attributes = project.tasks.jar.manifest.attributes
                    println 'Vaadin-Widgetsets ' + attributes['Vaadin-Widgetsets']
                    println 'Implementation-Title ' + attributes['Implementation-Title']
                    println 'Implementation-Version ' + attributes['Implementation-Version']
                    println 'Implementation-Vendor ' + attributes['Implementation-Vendor']
                    println 'Vaadin-License-Title ' + attributes['Vaadin-License-Title']
                    println 'Vaadin-Package-Version ' + attributes['Vaadin-Package-Version']
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyAddonJarManifest')
        assertTrue result, result.contains('Vaadin-Widgetsets com.example.Widgetset')
        assertTrue result, result.contains('Implementation-Title test-addon')
        assertTrue result, result.contains('Implementation-Version 1.2.3')
        assertTrue result, result.contains('Implementation-Vendor test-author')
        assertTrue result, result.contains('Vaadin-License-Title my-license')
        assertTrue result, result.contains('Vaadin-Package-Version 1')
    }

    @Test void 'Addon Zip Metadata'() {
        buildFile << """
            version '1.2.3'

            vaadinCompile {
                widgetset 'com.example.Widgetset'
            }

            vaadinAddon {
                title 'test-addon'
                license 'my-license'
                author 'test-author'
            }

            task verifyAddonZipManifest(dependsOn: '${CreateDirectoryZipTask.NAME}') {
                doLast {
                    def manifestFile = project.file('build/tmp/zip/META-INF/MANIFEST.MF')
                    println 'Zip manifest exists ' + manifestFile.exists()
    
                    def manifest = new java.util.jar.Manifest()
                    manifest.read(new ByteArrayInputStream(manifestFile.text.bytes))
    
                    def attributes = manifest.mainAttributes
                    attributes.entrySet().each { entry ->
                        println entry.key.toString() + ' ' + entry.value.toString()
                    }
                }
            }    
        """.stripIndent()

        def result = runWithArguments('verifyAddonZipManifest')
        assertTrue result, result.contains('Zip manifest exists true')
        assertTrue result, result.contains('Implementation-Title test-addon')
        assertTrue result, result.contains('Implementation-Version 1.2.3')
        assertTrue result, result.contains('Implementation-Vendor test-author')
        assertTrue result, result.contains('Vaadin-License-Title my-license')
    }

    @Test void 'Plugin task configurations are not applied on non-vaadin sub-projects'() {
        File projectDir = projectDir.newFolder('noVaadinSubProject')
        settingsFile << "include 'noVaadinSubProject'\n"

        buildFile << """
            project(':noVaadinSubProject') {
                apply plugin: 'java'
                // No vaadin dependencies, only plain java
            }
        """

        // Vaadin jar configurations are not applied to subproject
        def result = runWithArguments(':noVaadinSubProject:jar')
        assertFalse result, result.contains('No addon title has been specified')
        assertFalse result, result.contains('No version specified for the project')
    }
}
