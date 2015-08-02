package fi.jasoft.plugin.integration

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Created by john on 1/6/15.
 */
class ProjectDependenciesTest implements IntegrationTest {

    @Test void 'Project has Vaadin extension'(){

        buildFile << """
            import fi.jasoft.plugin.configuration.VaadinPluginExtension
            task testProperties << {
                println 'Has Vaadin property ' + project.hasProperty('vaadin')
                println 'Has Vaadin extension ' + (project.extensions.getByName('vaadin') != null)
                println 'Has Vaadin type ' + (project.extensions.getByType(VaadinPluginExtension) != null)
            }
        """.stripIndent()

        def result = getResultWithArguments('testProperties').standardOutput
        assertTrue result, result.contains( 'Has Vaadin property true')
        assertTrue result, result.contains( 'Has Vaadin extension true')
        assertTrue result, result.contains( 'Has Vaadin type true')
    }

    @Test void 'Project has Vaadin configurations'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*

            task testConfigurations << {
                def confs = project.configurations

                println 'Server configuration ' + confs.hasProperty(SERVER.caption)
                println 'Client configuration ' + confs.hasProperty(CLIENT.caption)
                println 'Javadoc configuration ' + confs.hasProperty(JAVADOC.caption)

                println 'Testbench configuration ' + confs.hasProperty(TESTBENCH.caption)
                println 'Push configuration ' + confs.hasProperty(PUSH.caption)
                println 'Groovy configuration ' + confs.hasProperty(GROOVY.caption)
            }
        """.stripIndent()

        def result = getResultWithArguments('testConfigurations').standardOutput
        assertTrue result, result.contains( 'Server configuration true')
        assertTrue result, result.contains( 'Client configuration true')
        assertTrue result, result.contains( 'Javadoc configuration true')

        assertTrue result, result.contains( 'Testbench configuration false')
        assertTrue result, result.contains( 'Push configuration false')
        assertTrue result, result.contains( 'Groovy configuration false')
    }

    @Test void 'Project has Vaadin repositories'() {

        buildFile << """
            import fi.jasoft.plugin.DependencyListener
            task testRepositories << {
                DependencyListener.Repositories.each {
                    if(!project.repositories.hasProperty(it.caption)){
                        println 'Repository missing '+it.caption
                    }
                }
            }
        """.stripIndent()

        def result = getResultWithArguments('testRepositories').standardOutput
        assertFalse result, result.contains( 'Repository missing')
    }

    @Test void 'Project has Jetty dependency'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*
            task hasJettyConfiguration << {
                def confs = project.configurations
                println 'Jetty 9 '+ confs.hasProperty(JETTY9.caption)
                println 'Jetty 8 '+ confs.hasProperty(JETTY8.caption)
            }
        """.stripIndent()

        def result = getResultWithArguments('hasJettyConfiguration').standardOutput
        assertTrue result, result.contains( 'Jetty 9 true')
        assertTrue result, result.contains( 'Jetty 8 false')
    }

    @Test void 'Project has pre-compiled widgetset'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*
            task hasWidgetset << {
                def confs = project.configurations
                def server = confs.getByName(SERVER.caption)
                println 'Has client dependency ' + !confs.getByName(CLIENT.caption).dependencies.empty
                println 'Has client-compiled dependency ' + !server.dependencies.findAll {it.name == 'vaadin-client-compiled'}.empty
            }
         """.stripIndent()

        def result = getResultWithArguments('hasWidgetset').standardOutput
        assertTrue result, result.contains( 'Has client dependency false')
        assertTrue result, result.contains( 'Has client-compiled dependency true')
    }

    @Test void 'Client dependencies added when widgetset present'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*

            vaadin {
                widgetset 'com.example.TestWidgetset'
            }

            task testClientDependencies << {
                def confs = project.configurations
                def server = confs.getByName(SERVER.caption)
                println 'Has client dependency ' + !confs.getByName(CLIENT.caption).dependencies.empty
                println 'Has client-compiled dependency ' +  !server.dependencies.findAll {it.name == 'vaadin-client-compiled'}.empty
            }
        """.stripIndent()

        def result = getResultWithArguments('testClientDependencies').standardOutput
        assertTrue result, result.contains( 'Has client dependency true')
        assertTrue result, result.contains( 'Has client-compiled dependency false')
    }

    @Test void 'Vaadin version is resolved'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*

            vaadin {
                version '7.3.0'
                widgetset 'com.example.TestWidgetset'
            }

            task verifyVaadinVersion << {
                def server = project.configurations.getByName(SERVER.caption)
                server.dependencies.each {
                    if(it.group.equals('com.vaadin')){
                        println 'Vaadin Server ' + it.version
                    }
                }
                def client = project.configurations.getByName(CLIENT.caption)
                client.dependencies.each {
                    if(it.group.equals('com.vaadin')){
                        println 'Vaadin Client ' + it.version
                    }
                }
            }
        """.stripIndent()

        def result = getResultWithArguments('verifyVaadinVersion').standardOutput
        assertTrue result, result.contains( 'Vaadin Server 7.3.0')
        assertTrue result, result.contains( 'Vaadin Client 7.3.0')
    }

    @Test void 'Project has Testbench dependencies'() {

        buildFile << """
            import static fi.jasoft.plugin.DependencyListener.Configuration.*

            vaadin {
                testbench {
                    enabled true
                }
            }

            task verifyTestbenchPresent << {
                def confs = project.configurations
                println 'Testbench configuration ' + confs.hasProperty(TESTBENCH.caption)

                def testbench = confs.getByName(TESTBENCH.caption)
                println 'Testbench artifacts ' + !testbench.empty
            }
        """.stripIndent()

        def result = getResultWithArguments('verifyTestbenchPresent').standardOutput
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

            task evaluateVersionBlacklist << {
                project.configurations.compile.dependencies.each {
                    if(it.version.equals(project.vaadin.version)) {
                        println 'Version blacklist failed for ' + it
                    }
                }
            }

        """.stripIndent()

        def result = getResultWithArguments('evaluateVersionBlacklist').standardOutput
        assertFalse result, result.contains( 'Version blacklist failed for')
    }

    @Test void 'Maven Central/Local are included'() {

        buildFile << """
            task testMavenCentralLocal << {
                def repos = project.repositories
                if(repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME)){
                    println 'Has Maven Central'
                }
                if(repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME)){
                    println 'Has Maven Local'
                }
            }
        """.stripIndent()

        def result = getResultWithArguments('testMavenCentralLocal').standardOutput
        assertTrue result, result.contains( 'Has Maven Central')
        assertTrue result, result.contains( 'Has Maven Local')
    }

}
