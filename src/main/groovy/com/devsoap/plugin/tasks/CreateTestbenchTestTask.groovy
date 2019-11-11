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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a Vaadin Testbench test
 *
 * @author John Ahlroos
 */
class CreateTestbenchTestTask extends DefaultTask {

    static final String NAME = 'vaadinCreateTestbenchTest'

    /**
     * The test class name
     */
    @Input
    @Option(option = 'name', description = 'Test name')
    String testName = 'MyTest'

    /**
     * The test package
     */
    @Input
    @Option(option = 'package', description = 'Test package')
    String testPackage = 'com.example.tests'

    CreateTestbenchTestTask() {
        description = "Creates a new Testbench test"
    }

    /**
     * Creates the test
     */
    @TaskAction
    void run() {
        if ( !project.vaadinTestbench.enabled ) {
            throw new GradleException('Please enable Testbench by setting vaadinTestbench.enabled=true before ' +
                    'creating a test')
        }
        makeTestClass()
    }

    private makeTestClass() {
        File javaDir = Util.getMainTestSourceSet(project).srcDirs.first()
        RunTask runTask = project.tasks.getByName(RunTask.NAME)
        File packageDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(testPackage))
        packageDir.mkdirs()

        def substitutions = [:]
        substitutions['packageName'] = testPackage
        substitutions['testName'] = testName
        substitutions['appUrl'] = "http://localhost:${runTask.serverPort}"

        if ( Util.getProjectType(project) == ProjectType.GROOVY ) {
            TemplateUtil.writeTemplate("MyTest.groovy", packageDir, testName + ".groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate("MyTest.java", packageDir, testName + ".java", substitutions)
        }
    }
}
