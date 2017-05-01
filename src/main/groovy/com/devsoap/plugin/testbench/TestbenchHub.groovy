/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.testbench

import com.devsoap.plugin.Util
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * Represents a Testbench Hub
 *
 * @author John Ahlroos
 */
class TestbenchHub {

    private final Project project

    private process

    TestbenchHub(Project project) {
        this.project = project
    }

    public start() {

        def host = project.vaadinTestbenchHub.host
        def port = project.vaadinTestbenchHub.port

        File logDir = project.file('build/testbench/')
        logDir.mkdirs()

        FileCollection cp = project.configurations['vaadin-testbench'] + Util.getClassPath(project)

        process = [Util.getJavaBinary(project)]

        process.add('-cp')
        process.add(cp.getAsPath())

        process.add('org.openqa.grid.selenium.GridLauncher')
        process.add('-role')
        process.add('hub')

        process.add('-host')
        process.add(host)

        process.add('-port')
        process.add(port)

        // Execute server
        process = process.execute()

        if ( project.vaadin.logToConsole ) {
            process.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/testbench-hub.log')
            process.consumeProcessOutputStream(new FileOutputStream(log))
        }

        // Wait for hub to start
        sleep(3000)

        project.logger.lifecycle("Testbench Hub started on http://$host:$port")
    }

    public terminate() {
        process.in.close()
        process.out.close()
        process.err.close()
        process.destroy()
        process = null

        project.logger.lifecycle("Testbench hub terminated")
    }

}
