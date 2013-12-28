package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/*
* Copyright 2013 John Ahlroos
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
class TestbenchNode {

    private final Project project

    private process;

    TestbenchNode(Project project){
        this.project = project
    }

    public start(){

        File logDir = new File('build/testbench/')
        logDir.mkdirs()

        FileCollection cp = project.configurations['vaadin-testbench'] + Util.getClassPath(project)

        process = ['java']

        process.add('-cp')
        process.add(cp.getAsPath())

        process.add('org.openqa.grid.selenium.GridLauncher')

        process.add('-role')
        process.add('node')

        process.add('-hub')
        process.add('http://localhost:4444/grid/register')

        process.add('-port')
        process.add('4445')

        // Execute server
        process = process.execute()

        if (project.vaadin.plugin.logToConsole) {
            process.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/testbench-node.log')
            process.consumeProcessOutputStream(new FileOutputStream(log))
        }

        project.logger.lifecycle("Testbench node started.")
    }

    public terminate() {
        process.in.close()
        process.out.close()
        process.err.close()
        process.destroy()
        process = null;

        project.logger.lifecycle("Testbench node terminated")
    }
}
