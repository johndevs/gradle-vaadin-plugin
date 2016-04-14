/*
* Copyright 2016 John Ahlroos
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
package fi.jasoft.plugin.integration

import groovy.transform.PackageScope
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths

/**
 * Base class for integration tests
 */
class IntegrationTest {

    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder()

    protected File buildFile

    @Before
    void setup() {
        buildFile = createBuildFile(projectDir.root)
    }

    protected static File createBuildFile(File projectDir) {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.createNewFile()

        def projectVersion = System.getProperty('integrationTestProjectVersion')

        File libsDir = Paths.get('.', 'build', 'libs').toFile()
        String escapedDir = libsDir.canonicalPath.replace("\\","\\\\")

        // Apply plugin to project
        buildFile << """
            buildscript {
                repositories {
                    mavenLocal()
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

            vaadin.logToConsole = true
        """
        buildFile
    }

    protected String runWithArgumentsOnProject(File projectDir, String... args) {
        setupRunner(projectDir).withArguments((args as List) + ['--stacktrace']).build().output
    }

    protected String runWithArguments(String... args) {
        runWithArgumentsOnProject(projectDir.root, args)
    }

    protected String runFailureExpected() {
        setupRunner().buildAndFail().output
    }

    protected GradleRunner setupRunner(File projectDir = this.projectDir.root) {
        GradleRunner.create().withProjectDir(projectDir)
    }
}