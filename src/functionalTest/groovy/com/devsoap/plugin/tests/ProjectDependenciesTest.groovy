package com.devsoap.plugin.tests

import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Tests the injected dependencies
 */
class ProjectDependenciesTest extends IntegrationTest {

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        super.applyThirdPartyPlugins(buildFile)

        buildFile << """
            plugins {
                id "de.undercouch.download" version "3.2.0"
            }
        """.stripIndent()
    }

    @Test void 'Project has Vaadin extension'() {

        buildFile << """
            import com.devsoap.plugin.extensions.VaadinPluginExtension
            task testProperties {
                doLast {
                    println 'Has Vaadin property ' + project.hasProperty('vaadin')
                    println 'Has Vaadin extension ' + (project.extensions.getByName('vaadin') != null)
                    println 'Has Vaadin type ' + (project.extensions.getByType(VaadinPluginExtension) != null)
                }
            }
        """.stripIndent()

        def result = runWithArguments('testProperties')
        assertTrue result, result.contains( 'Has Vaadin property true')
        assertTrue result, result.contains( 'Has Vaadin extension true')
        assertTrue result, result.contains( 'Has Vaadin type true')
    }

    @Test void 'Project has Vaadin configurations'() {

        buildFile << """
            task testConfigurations {
                doLast {
                    def confs = project.configurations
    
                    println 'Server configuration ' + confs.hasProperty('vaadin-server')
                    println 'Client configuration ' + confs.hasProperty('vaadin-client')
                    println 'Javadoc configuration ' + confs.hasProperty('vaadin-javadoc')
    
                    println 'Testbench configuration ' + !confs.getByName('vaadin-testbench').dependencies.empty
                    println 'Push configuration ' + !confs.getByName('vaadin-push').dependencies.empty
                    println 'Groovy configuration ' + confs.hasProperty('vaadin-groovy')
                }
            }
        """.stripIndent()

        def result = runWithArguments('testConfigurations')
        assertTrue result, result.contains( 'Server configuration true')
        assertTrue result, result.contains( 'Client configuration true')
        assertTrue result, result.contains( 'Javadoc configuration true')

        assertTrue result, result.contains( 'Testbench configuration false')
        assertTrue result, result.contains( 'Push configuration false')
        assertTrue result, result.contains( 'Groovy configuration false')
    }

    @Test void 'Project has Vaadin repositories'() {

        buildFile << """
            task testRepositories {
                doLast {
                    def repositories = [
                        'Vaadin addons',
                        'Vaadin snapshots'
                    ]
    
                    repositories.each {
                        if ( !project.repositories.hasProperty(it) ) {
                            println 'Repository missing '+it
                        }
                    }
                }
            }    
        """.stripIndent()

        def result = runWithArguments('testRepositories')
        assertFalse result, result.contains( 'Repository missing')
    }

    @Test void 'Project has pre-compiled widgetset'() {

        buildFile << """
            task hasWidgetset {
                doLast {
                    def confs = project.configurations
                    def client = confs.getByName('vaadin-client').resolvedConfiguration
                    def artifacts = client.resolvedArtifacts
                    println 'Has client dependency ' + !artifacts.empty
                    println 'Has client-compiled dependency ' + !artifacts.findAll {
                        it.moduleVersion.id.name == 'vaadin-client-compiled'
                    }.empty
                }
            }
         """.stripIndent()

        def result = runWithArguments('hasWidgetset')
        assertTrue result, result.contains( 'Has client dependency true')
        assertTrue result, result.contains( 'Has client-compiled dependency true')
    }

    @Test void 'Client dependencies added when widgetset present'() {

        buildFile << """
            vaadinCompile {
               widgetset 'com.example.TestWidgetset'
            }

            task testClientDependencies {
                doLast {
                    def confs = project.configurations
                    def client = confs.getByName('vaadin-client').resolvedConfiguration
                    def artifacts = client.resolvedArtifacts
                    println 'Has client dependency ' + !artifacts.empty
                }
            }    
        """.stripIndent()

        def result = runWithArguments('testClientDependencies')
        assertTrue result, result.contains( 'Has client dependency true')
    }

    @Test void 'Client dependencies added when widgetset is automatically detected'() {
        buildFile << """
            task testClientDependencies {
                doLast {
                    def confs = project.configurations
                    def client = confs.getByName('vaadin-server').resolvedConfiguration
                    def artifacts = client.resolvedArtifacts
                    println 'Has client dependency ' + !artifacts.empty
                }
            }
        """.stripIndent()

        runWithArguments(CreateProjectTask.NAME, '--widgetset=com.example.MyWidgetset')

        def result = runWithArguments('testClientDependencies')
        assertTrue result, result.contains( 'Has client dependency true')
    }

    @Test void 'Vaadin version is resolved'() {

        buildFile << """
            vaadin {
                version '7.3.0'               
            }
            
            vaadinCompile {
                widgetset 'com.example.TestWidgetset'
            }

            task verifyVaadinVersion {
                doLast {
                    def server = project.configurations.getByName('vaadin-server').resolvedConfiguration
                    println 'server:'
                    server.resolvedArtifacts.each {              
                        if ( it.moduleVersion.id.group.equals('com.vaadin') ) {
                            println 'Vaadin Server ' + it.moduleVersion.id.version
                        }
                    }
                    def client = project.configurations.getByName('vaadin-client').resolvedConfiguration
                    println 'client:'
                    client.resolvedArtifacts.each {
                        if ( it.moduleVersion.id.group.equals('com.vaadin') ) {
                            println 'Vaadin Client ' + it.moduleVersion.id.version
                        }
                    }
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyVaadinVersion')
        assertTrue result, result.contains( 'Vaadin Server 7.3.0')
        assertTrue result, result.contains( 'Vaadin Client 7.3.0')
    }

    @Test void 'Project has Testbench dependencies'() {

        buildFile << """
            vaadinTestbench {
                enabled true
            }

            task verifyTestbenchPresent {
                doLast {
                    def confs = project.configurations
                    println 'Testbench configuration ' + confs.hasProperty('vaadin-testbench')
    
                    def testbench = confs.getByName('vaadin-testbench')
                    println 'Testbench artifacts ' + !testbench.empty
                }
            }
        """.stripIndent()

        def result = runWithArguments('verifyTestbenchPresent')
        assertTrue result, result.contains( 'Testbench configuration true')
        assertTrue result, result.contains( 'Testbench artifacts true')
    }

    @Test void 'Vaadin version blacklist'() {

        buildFile << """
             dependencies {
                compile 'com.vaadin:vaadin-sass-compiler:+'
                compile 'com.vaadin:vaadin-client-compiler-deps:+'
                compile 'com.vaadin:vaadin-cdi:+'
                compile 'com.vaadin:vaadin-spring:+'
                compile 'com.vaadin:vaadin-spring-boot:+'
            }

            task evaluateVersionBlacklist {
                doLast {
                    project.configurations.compile.dependencies.each {
                        if ( it.version.equals(project.vaadin.version) ) {
                            println 'Version blacklist failed for ' + it
                        }
                    }
                }
            }
        """.stripIndent()

        def result = runWithArguments('evaluateVersionBlacklist')
        assertFalse result, result.contains( 'Version blacklist failed for')
    }

    @Test void 'Maven Central and Local are included'() {

        buildFile << """
            task testMavenCentralLocal {
                doLast {
                    def repos = project.repositories
                    if ( repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) ) {
                        println 'Has Maven Central'
                    }
                    if ( repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME) ) {
                        println 'Has Maven Local'
                    }
                }
            }
        """.stripIndent()

        def result = runWithArguments('testMavenCentralLocal')
        assertTrue result, result.contains( 'Has Maven Central')
        assertTrue result, result.contains( 'Has Maven Local')
    }

    @Test void 'Dependency without version'() {
        buildFile << """
            String lib = 'libs/qrcode-2.1.jar'
            dependencies {
                 compile files(lib)
            }
            task downloadFile(type: de.undercouch.gradle.tasks.download.Download) {
                src 'http://vaadin.com/nexus/content/repositories/vaadin-addons/' +
                    'org/vaadin/addons/qrcode/2.1/qrcode-2.1.jar'
                dest lib
            }
        """.stripIndent()

        runWithArguments(CreateProjectTask.NAME, 'downloadFile')
        runWithArguments('vaadinCompile')
    }
}
