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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a Vaadin Testbench test
 *
 * @author John Ahlroos
 */
class CreateTestbenchTestTask extends DefaultTask {

    static final String NAME = 'vaadinCreateTestbenchTest'

    @Option(option = 'name', description = 'Test name')
    def testName = 'MyTest'

    @Option(option = 'package', description = 'Test package')
    def testPackage = 'com.example.tests'

    public CreateTestbenchTestTask() {
        description = "Creates a new Testbench test"
    }

    @TaskAction
    def run() {
        if ( !project.vaadinTestbench.enabled ) {
            throw new GradleException('Please enable Testbench by setting vaadinTestbench.enabled=true before ' +
                    'creating a test')
        }
        makeTestClass()
    }

    @PackageScope
    def makeTestClass() {
        def javaDir = Util.getMainTestSourceSet(project).srcDirs.first()
        def packageDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(testPackage))
        packageDir.mkdirs()

        def substitutions = [:]
        substitutions['packageName'] = testPackage
        substitutions['testName'] = testName
        substitutions['appUrl'] = "http://localhost:${project.vaadinRun.serverPort}"

        if ( Util.isGroovyProject(project) ) {
            TemplateUtil.writeTemplate("MyTest.groovy", packageDir, testName + ".groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate("MyTest.java", packageDir, testName + ".java", substitutions)
        }
    }
}
