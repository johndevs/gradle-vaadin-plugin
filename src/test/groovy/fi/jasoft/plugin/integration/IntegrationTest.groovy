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

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.nio.file.Paths
import java.util.logging.Logger

/**
 * Base class for integration tests
 */
class IntegrationTest {

    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder()

    protected File buildFile

    protected File settingsFile

    @Before
    void setup() {
        println "Running test in $projectDir.root"
        buildFile = makeBuildFile(projectDir.root)
        settingsFile = projectDir.newFile("settings.gradle")
    }

    protected String getPluginDir() {
        File libsDir = Paths.get('.', 'build', 'libs').toFile()
        String escapedDir = libsDir.canonicalPath.replace("\\","\\\\")
        escapedDir
    }

    protected File makeBuildFile(File projectDir, boolean applyPluginToFile=true) {
        File buildFile = new File(projectDir, 'build.gradle')
        buildFile.createNewFile()

        def projectVersion = System.getProperty('integrationTestProjectVersion')

        String escapedDir = getPluginDir()

        // Apply plugin to project
        buildFile << """
            buildscript {
                repositories {
                    mavenLocal()
                    mavenCentral()
                    flatDir dirs:file('$escapedDir')
                }

                dependencies {
                    classpath group: 'org.codehaus.groovy.modules.http-builder', name: 'http-builder', version: '0.7.1'
                    classpath group: 'fi.jasoft.plugin', name: 'gradle-vaadin-plugin', version: '$projectVersion'
                }
            }

        """

        if (  applyPluginToFile ) {
            applyRepositories(buildFile)
            applyPlugin(buildFile)
            buildFile << "vaadin.logToConsole = true\n"
        }

        buildFile
    }

    protected void applyRepositories(File buildFile) {
        String escapedDir = getPluginDir()
        buildFile << """
            repositories {
                flatDir dirs:file('$escapedDir')
            }
        """
    }

    protected void applyPlugin(File buildFile) {
        buildFile << "apply plugin:fi.jasoft.plugin.GradleVaadinPlugin\n"
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