package fi.jasoft.plugin.integration

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

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

        // Apply plugin to project
        buildFile << """
            buildscript {
                repositories {
                    mavenCentral()
                    flatDir dirs: '${new File('.').absolutePath + '/build/libs'}'
                }

                dependencies {
                    classpath group: 'org.codehaus.groovy.modules.http-builder', name: 'http-builder', version: '0.7.1'
                    classpath group: 'fi.jasoft.plugin', name: 'gradle-vaadin-plugin', version: 'SNAPSHOT-'+ new Date().format('yyyyMMdd')
                }
            }

            apply plugin: fi.jasoft.plugin.GradleVaadinPlugin
        """
    }

    String runWithArguments(String... args) {
        GradleRunner.create().withProjectDir(projectDir.root).withArguments(args).build().output
    }

    String runFailureExpected() {
        GradleRunner.create().withProjectDir(projectDir.root).buildAndFail().output
    }
}