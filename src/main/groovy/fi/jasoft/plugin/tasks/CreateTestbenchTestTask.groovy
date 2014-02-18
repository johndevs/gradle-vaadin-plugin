package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/*
* Copyright 2014 John Ahlroos
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
class CreateTestbenchTestTask extends DefaultTask {

    public static final String NAME = 'createTestbenchTest'

    public CreateTestbenchTestTask(){
        description = "Creates a new Testbench test"
    }

    @TaskAction
    public run(){

        String testName = Util.readLine('\nTest Name (MyTest): ')
        if (testName == '') {
            testName = 'MyTest'
        }

        String testPackage = Util.readLine('\nTest package (com.example.tests): ')
        if (testPackage == '') {
            testPackage = 'com.example.tests'
        }

        File javaDir = Util.getTestSourceSet(project).srcDirs.iterator().next()
        File packageDir = new File(javaDir.canonicalPath+"/"+testPackage.replaceAll(/\./, '/'))

        packageDir.mkdirs()

        def substitutions = [:]
        substitutions['%PACKAGE%'] = testPackage
        substitutions['%TEST_NAME%'] = testName
        substitutions['%APPLICATION_URL%'] = "http://localhost:${project.vaadin.serverPort}"

        TemplateUtil.writeTemplate("MyTest.java",packageDir,testName+".java",substitutions)
    }
}
