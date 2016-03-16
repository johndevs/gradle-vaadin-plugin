package fi.jasoft.plugin.integration

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths

/**
 * Created by john on 7/29/15.
 */
abstract class IntegrationTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
        buildFile = projectDir.newFile("build.gradle")
        def projectVersion = System.getProperty('integrationTestProjectVersion')

        def libsDir = Paths.get('.', 'build', 'libs').toFile()
        def escapedDir = libsDir.canonicalPath.replace("\\","\\\\")

        // Apply plugin to project
        buildFile << """
            buildscript {
                repositories {
                    mavenCentral()
                    flatDir dirs: file('$escapedDir')
                }

                dependencies {
                    classpath group: 'org.codehaus.groovy.modules.http-builder', name: 'http-builder', version: '0.7.1'
                    classpath group: 'fi.jasoft.plugin', name: 'gradle-vaadin-plugin', version: '$projectVersion'
                }
            }

            repositories {
                flatDir dirs: file('$escapedDir')
            }

            apply plugin: fi.jasoft.plugin.GradleVaadinPlugin

            vaadin.plugin.logToConsole = true
        """
    }

    String runWithArguments(String... args) {
        setupRunner().withArguments((args as List) + ['--stacktrace']).build().output
    }

    String runFailureExpected() {
        setupRunner().buildAndFail().output
    }

    GradleRunner setupRunner() {
        GradleRunner.create().withProjectDir(projectDir.root)
    }
}