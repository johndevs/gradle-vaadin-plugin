/*
 * Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.testbench

import com.devsoap.plugin.Util
import com.devsoap.plugin.extensions.TestBenchHubExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState

/**
 * Represents a Testbench Hub
 *
 * @author John Ahlroos
 * @since 1.1
 */
class TestbenchHub {

    private final Project project
    private Process process

    private final PropertyState<String> host
    private final PropertyState<Integer> port

    TestbenchHub(Project project) {
        this.project = project

        TestBenchHubExtension extension = project.extensions.getByType(TestBenchHubExtension)

        host = project.property(String)
        host.set(extension.hostProvider)

        port = project.property(Integer)
        port.set(extension.portProvider)
    }

    /**
     * Starts the Testbench hub
     */
    void start() {

        File logDir = project.file('build/testbench/')
        logDir.mkdirs()

        FileCollection cp = project.configurations['vaadin-testbench'] + Util.getWarClasspath(project)

        List processList = [Util.getJavaBinary(project)]

        processList.add('-cp')
        processList.add(cp.getAsPath())

        processList.add('org.openqa.grid.selenium.GridLauncher')
        processList.add('-role')
        processList.add('hub')

        processList.add('-host')
        processList.add(host.get())

        processList.add('-port')
        processList.add(port.get().toString())

        // Execute server
        process = processList.execute([], project.buildDir)

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

    /**
     * Terminates the testbench hub
     */
    void terminate() {
        process.in.close()
        process.out.close()
        process.err.close()
        process.destroy()
        process = null

        project.logger.lifecycle("Testbench hub terminated")
    }

    /**
     * Get the hostname or IP address of the hub
     */
    String getHost() {
        host.get()
    }

    /**
     * Set the hostname or IP address of the hub
     */
    void setHost(String host) {
        this.host.set(host)
    }

    /**
     * Get the port of the hub
     */
    Integer getPort() {
        port.get()
    }

    /**
     * Set the port of the hub
     */
    void setPort(Integer port) {
        this.port.set(port)
    }
}
