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
package fi.jasoft.plugin.testbench

import fi.jasoft.plugin.Util
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * Represents a Testbench Node
 *
 * @author John Ahlroos
 */
class TestbenchNode {

    private final Project project

    private process;

    TestbenchNode(Project project) {
        this.project = project
    }

    public start() {

        def host = project.vaadin.testbench.node.host
        def port = project.vaadin.testbench.node.port
        def hub = project.vaadin.testbench.node.hub
        def browsers = project.vaadin.testbench.node.browsers

        File logDir = project.file('build/testbench/')
        logDir.mkdirs()

        FileCollection cp = project.configurations['vaadin-testbench'] + Util.getClassPath(project)

        process = [Util.getJavaBinary(project)]

        process.add('-cp')
        process.add(cp.getAsPath())

        process.add('org.openqa.grid.selenium.GridLauncher')

        process.add('-role')
        process.add('node')

        process.add('-hub')
        process.add(hub)

        process.add('-host')
        process.add(host)

        process.add('-port')
        process.add(port)

        for (browser in browsers) {
            process.add('-browser')
            process.add(browser.inject([]) { result, entry ->
                result << "${entry.key}=${entry.value}"
            }.join(','))
        }

        // Execute server
        process = process.execute()

        if (project.vaadin.plugin.logToConsole) {
            process.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/testbench-node.log')
            process.consumeProcessOutputStream(new FileOutputStream(log))
        }

        // Wait for node to start and connect to hub
        sleep(10000)

        project.logger.lifecycle("Testbench Node started on http://$host:$port/wd/hub")

    }

    public terminate() {
        process.in.close()
        process.out.close()
        process.err.close()
        process.destroy()
        process = null;

        project.logger.lifecycle("Testbench node terminated.")
    }
}
