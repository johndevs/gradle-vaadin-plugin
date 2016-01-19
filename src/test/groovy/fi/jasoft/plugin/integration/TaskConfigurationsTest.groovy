package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CreateDirectoryZipTask
import org.junit.Test

import static org.junit.Assert.assertTrue

/**
 * Created by john on 1/6/15.
 */
class TaskConfigurationsTest extends IntegrationTest {

    @Test void 'Eclipse default configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyEclipseClassPath(dependsOn: 'eclipseClasspath') << {
                def classpath = project.eclipse.classpath
                println 'Download sources ' +  classpath.downloadSources
                println 'Eclipse output dir ' + project.vaadin.plugin.eclipseOutputDir
                println 'Classes dir is default output dir ' + (classpath.defaultOutputDir == project.sourceSets.main.output.classesDir)

                def confs = project.configurations
                println 'Server in classpath ' + (confs.getByName('vaadin-server') in classpath.plusConfigurations)
                println 'Client in classpath ' + (confs.getByName('vaadin-client') in classpath.plusConfigurations)

                def natures = project.eclipse.project.natures
                println 'Springsource nature ' + ('org.springsource.ide.eclipse.gradle.core.nature' in natures)
            }
        """.stripIndent()

        def result = runWithArguments('verifyEclipseClassPath')

        assertTrue result, result.contains( 'Download sources true')
        assertTrue result, result.contains( 'Eclipse output dir null')
        assertTrue result, result.contains( 'Classes dir is default output dir true')

        assertTrue result, result.contains( 'Server in classpath true')
        assertTrue result, result.contains( 'Client in classpath true')
      
        assertTrue result, result.contains( 'Springsource nature true')
    }

    @Test void 'Eclipse configuration with custom output dir'() {
        buildFile << """
            apply plugin: 'eclipse-wtp'

            vaadin {
                plugin {
                    eclipseOutputDir 'custom/dir'
                }
            }

            task verifyOutputDir(dependsOn:'eclipseClasspath') << {
                println project.eclipse.classpath.defaultOutputDir
                println project.file('custom/dir')
                println 'Default output dir is set to eclipseOutputDir ' + (project.eclipse.classpath.defaultOutputDir == project.file('custom/dir'))
            }
        """.stripIndent()

        def result = runWithArguments('verifyOutputDir')
        assertTrue result, result.contains('Default output dir is set to eclipseOutputDir true')
    }

    @Test void 'Eclipse configuration with Testbench enabled'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            vaadin {
                testbench {
                    enabled true
                }
            }

            task verifyTestbenchDependency(dependsOn: 'eclipseClasspath') << {
                def confs = project.configurations
                def classpath = project.eclipse.classpath
                println 'Testbench on classpath ' + (confs.getByName('vaadin-testbench') in classpath.plusConfigurations)
            }

        """.stripIndent()

        def result = runWithArguments('verifyTestbenchDependency')
        assertTrue result.contains('Testbench on classpath true')
    }

    @Test void 'Eclipse WTP component configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyWTP(dependsOn: eclipseWtpComponent) << {
                def confs = project.configurations
                println 'Server in components ' + (confs.getByName('vaadin-server') in project.eclipse.wtp.component.plusConfigurations)
            }

        """.stripIndent()

        def result = runWithArguments('verifyWTP')
        assertTrue result, result.contains('Server in components true')
    }

    @Test void 'Eclipse WTP facet configuration'() {

        buildFile << """
            apply plugin: 'eclipse-wtp'

            task verifyWTP(dependsOn: eclipseWtpFacet) << {
                def facets = project.eclipse.wtp.facet.facets
                def JavaVersion javaVersion = project.sourceCompatibility
                println 'Vaadin Facet version ' + (facets.find { it.name=='com.vaadin.integration.eclipse.core'}.version)
                println 'jst.web Facet version ' + (facets.find { it.name=='jst.web'}.version)
                println 'Java Facet version equals sourceCompatibility ' + (javaVersion.toString() == facets.find { it.name=='java'}.version)
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

            task verifyIdeaModule(dependsOn: 'ideaModule') << {

                def module = project.idea.module
                println 'Module and Project name is equal ' + (project.name == module.name)
                println 'Output dir is classes dir ' + (project.sourceSets.main.output.classesDir == module.outputDir)
                println 'Test output dir is classes dir ' + (project.sourceSets.test.output.classesDir == module.testOutputDir)

                println 'Download Javadoc ' + module.downloadJavadoc
                println 'Download Sources ' + module.downloadSources

                def conf = project.configurations
                def scopes = module.scopes
                println 'Server configuration included ' + (conf.getByName('vaadin-server') in scopes.COMPILE.plus)
                println 'Client configuration included ' + (conf.getByName('vaadin-client') in scopes.COMPILE.plus)
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

             vaadin {
                testbench {
                    enabled true
                }
             }

             task verifyTestBench(dependsOn: 'ideaModule') << {
                def conf = project.configurations
                def module = project.idea.module
                def scopes = module.scopes
                println 'Test configuration has testbench ' + (conf.getByName('vaadin-testbench') in scopes.TEST.plus)
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

            task verifyPush(dependsOn: 'ideaModule') << {
                def conf = project.configurations
                def module = project.idea.module
                def scopes = module.scopes
                println 'Compile configuration has push ' + (conf.getByName('vaadin-push') in scopes.COMPILE.plus)
            }

        """.stripIndent()

        def result = runWithArguments('verifyPush')

        assertTrue result, result.contains('Compile configuration has push true')
    }

    @Test void 'Update widgetset generator before compile'() {

        buildFile << """
             vaadin {
                widgetset 'com.example.Widgetset'
                widgetsetGenerator 'com.example.WidgetsetGenerator'
             }

             task verifyWidgetsetGenerator(dependsOn:compileJava) << {
                def generatorFile = file('src/main/java/com/example/WidgetsetGenerator.java')
                println generatorFile
                println 'Generator File was created ' + generatorFile.exists()
             }
        """.stripIndent()

        def result = runWithArguments('verifyWidgetsetGenerator')

        assertTrue result, result.contains('Generator File was created true')
    }

    @Test void 'Addon Jar Metadata'() {

        buildFile << """
            version '1.2.3'

            vaadin {
                widgetset 'com.example.Widgetset'
                addon {
                    title 'test-addon'
                    license 'my-license'
                    author 'test-author'
                }
            }

            task verifyAddonJarManifest(dependsOn: 'jar') << {
                def attributes = project.tasks.jar.manifest.attributes
                println 'Vaadin-Widgetsets ' + attributes['Vaadin-Widgetsets']
                println 'Implementation-Title ' + attributes['Implementation-Title']
                println 'Implementation-Version ' + attributes['Implementation-Version']
                println 'Implementation-Vendor ' + attributes['Implementation-Vendor']
                println 'Vaadin-License-Title ' + attributes['Vaadin-License-Title']
                println 'Vaadin-Package-Version ' + attributes['Vaadin-Package-Version']
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

            vaadin {
                widgetset 'com.example.Widgetset'
                addon {
                    title 'test-addon'
                    license 'my-license'
                    author 'test-author'
                }
            }

            task verifyAddonZipManifest(dependsOn: '${CreateDirectoryZipTask.NAME}') << {
                def manifestFile = project.file('build/tmp/zip/META-INF/MANIFEST.MF')
                println 'Zip manifest exists ' + manifestFile.exists()

                def manifest = new java.util.jar.Manifest()
                manifest.read(new ByteArrayInputStream(manifestFile.text.bytes))

                def attributes = manifest.mainAttributes
                attributes.entrySet().each { entry ->
                    println entry.key.toString() + ' ' + entry.value.toString()
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
}
