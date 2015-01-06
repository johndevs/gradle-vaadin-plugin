package fi.jasoft.plugin.integration

import fi.jasoft.plugin.DependencyListener
import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.configuration.VaadinPluginExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactRepositoryContainer
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static fi.jasoft.plugin.DependencyListener.Configuration.*
import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertNotNull
import static junit.framework.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Created by john on 1/6/15.
 */
class ProjectDependenciesTest {

    def Project project

    @Before void setup(){
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin
            evaluate()
            project
        }
    }

    @Test void 'Project has Vaadin extension'(){
        assertTrue project.hasProperty('vaadin')
        assertNotNull project.extensions.getByName('vaadin')
        assertNotNull project.extensions.getByType(VaadinPluginExtension)
    }

    @Test void 'Project has Vaadin configurations'() {
        def confs = project.configurations

        assertTrue confs.hasProperty(SERVER.caption) as Boolean
        assertTrue confs.hasProperty(CLIENT.caption) as Boolean
        assertTrue confs.hasProperty(JAVADOC.caption) as Boolean

        assertFalse confs.hasProperty(TESTBENCH.caption) as Boolean
        assertFalse confs.hasProperty(PUSH.caption) as Boolean
        assertFalse confs.hasProperty(GROOVY.caption) as Boolean
    }

    @Test void 'Project has Vaadin repositories'() {
        DependencyListener.Repositories.each {
            assertTrue project.repositories.hasProperty(it.caption) as Boolean
        }
    }

    @Test void 'Project has Jetty dependency'() {
        def confs = project.configurations

        assertTrue confs.hasProperty(JETTY9.caption) as Boolean
        assertFalse confs.hasProperty(JETTY8.caption) as Boolean
    }

    @Test void 'Project has pre-compiled widgetset'() {
        def client = project.configurations.getByName(CLIENT.caption)
        assertTrue client.dependencies.isEmpty()

        def server = project.configurations.getByName(SERVER.caption)
        assertFalse server.dependencies.findAll {it.name == 'vaadin-client-compiled'}.isEmpty()
    }

    @Test void 'Client dependencies added when widgetset present'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin

            vaadin {
                widgetset 'com.example.TestWidgetset'
            }

            evaluate()
            project
        }

        def server = project.configurations.getByName(SERVER.caption)
        assertTrue server.dependencies.findAll {it.name == 'vaadin-client-compiled'}.isEmpty()

        def client = project.configurations.getByName(CLIENT.caption)
        assertFalse client.dependencies.isEmpty()
    }

    @Test void 'Vaadin version is resolved'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin

            vaadin {
                version '7.3.0'
                widgetset 'com.example.TestWidgetset'
            }

            evaluate()
            project
        }

        def server = project.configurations.getByName(SERVER.caption)
        server.dependencies.each {
            if(it.group.equals('com.vaadin')){
                assertEquals '7.3.0', it.version
            }
        }

        def client = project.configurations.getByName(CLIENT.caption)
        client.dependencies.each {
            if(it.group.equals('com.vaadin')){
                assertEquals '7.3.0', it.version
            }
        }
    }

    @Test void 'Project has Testbench dependencies'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin

            vaadin {
                testbench {
                    enabled true
                }
            }

            evaluate()
            project
        }

        def confs = project.configurations
        assertTrue confs.hasProperty(TESTBENCH.caption) as Boolean

        def testbench = confs.getByName(TESTBENCH.caption)
        assertFalse testbench.isEmpty()

    }

    @Test void 'Vaadin version blacklist'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin

            dependencies {
                compile 'com.vaadin:vaadin-sass-compiler:+'
                compile 'com.vaadin:vaadin-client-compiler-deps:+'
                compile 'com.vaadin:vaadin-cdi:+'
            }

            evaluate()
            project
        }

        project.configurations.compile.dependencies.each {
            assertFalse it.version.equals(project.vaadin.version)
        }
    }

    @Test void 'Maven Central/Local are included'() {
        def repos = project.repositories
        assertTrue repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) as Boolean
        assertTrue repos.hasProperty(ArtifactRepositoryContainer.DEFAULT_MAVEN_LOCAL_REPO_NAME) as Boolean
    }

}
