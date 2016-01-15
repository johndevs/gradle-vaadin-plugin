package fi.jasoft.plugin.integration

import junit.framework.Assert
import org.apache.commons.io.FilenameUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths

/**
 * Created by john on 7/29/15.
 */
trait IntegrationTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
        buildFile = projectDir.newFile("build.gradle")
        def projectVersion = System.getProperty('integrationTestProjectVersion')
        println "Project version set to $projectVersion"

        def libsDir = Paths.get('.', 'build', 'libs').toFile()
        def escapedDir = libsDir.canonicalPath.replace("\\","\\\\")
        println "Repository for built dependencies $escapedDir"


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
        """
    }

    String runWithArguments(String... args) {
        setupRunner().withArguments(args).build().output
    }

    String runFailureExpected() {
        setupRunner().buildAndFail().output
    }

    GradleRunner setupRunner() {
        GradleRunner.create().withProjectDir(projectDir.root)
    }
}