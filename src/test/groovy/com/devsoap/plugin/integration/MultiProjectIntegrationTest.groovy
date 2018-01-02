/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.integration
/**
 * Base for integration tests which has modules with vaadin projects
 */
class MultiProjectIntegrationTest extends IntegrationTest {

    @Override
    void setup() {
        startTime = System.currentTimeMillis()
        println "Running test in $projectDir.root"

        // Create root build file without vaadin plugin
        buildFile = makeBuildFile(projectDir.root, false)
        settingsFile = projectDir.newFile("settings.gradle")

        def escapedDir = getPluginDir()

        // Apply plugin to subprojects instead
        buildFile << """
            allprojects {
                apply plugin: 'java'
            }

            subprojects {
                apply plugin:com.devsoap.plugin.GradleVaadinPlugin
                repositories {
                    flatDir dirs:file('$escapedDir')
                }
                vaadin.logToConsole = true
            }
        """
    }

    protected File makeProject(String name) {
        File projectDir = projectDir.newFolder(name)
        settingsFile << "include '$name'\n"
        projectDir
    }
}
