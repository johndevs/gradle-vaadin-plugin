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
package com.devsoap.plugin.testbench

import com.devsoap.plugin.Util
import com.devsoap.plugin.extensions.TestBenchNodeExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/**
 * Represents a Testbench Node
 *
 * @author John Ahlroos
 * @since 1.1
 */
class TestbenchNode {

    private final Project project

    private Process process

    private final Property<String> host
    private final Property<Integer> port
    private final Property<String> hub
    private final MapProperty<String, String> browsers

    TestbenchNode(Project project) {
        this.project = project

        TestBenchNodeExtension extension = project.extensions.getByType(TestBenchNodeExtension)

        host = project.objects.property(String)
        host.set(extension.hostProvider)

        port = project.objects.property(Integer)
        port.set(extension.portProvider)

        hub = project.objects.property(String)
        hub.set(extension.hubProvider)

        browsers = project.objects.mapProperty(String, String)
        browsers.set(extension.browsersProvider)
    }

    /**
     * Starts the Testbench Node
     */
    void start() {
        TestBenchNodeExtension nodeExtension = project.extensions.getByType(TestBenchNodeExtension)
        String host = nodeExtension.host
        Integer port = nodeExtension.port
        String hub = nodeExtension.hub
        List<Map> browsers = nodeExtension.browsers

        File logDir = project.file('build/testbench/')
        logDir.mkdirs()

        FileCollection cp = project.configurations['vaadin-testbench'] + Util.getWarClasspath(project)

        List processList = [Util.getJavaBinary(project)]

        processList.add('-cp')
        processList.add(cp.getAsPath())

        processList.add('org.openqa.grid.selenium.GridLauncher')

        processList.add('-role')
        processList.add('node')

        processList.add('-hub')
        processList.add(hub)

        processList.add('-host')
        processList.add(host)

        processList.add('-port')
        processList.add(port.toString())

        for ( browser in browsers ) {
            processList.add('-browser')
            processList.add(browser.inject([]) { result, entry ->
                result << "${entry.key}=${entry.value}"
            }.join(','))
        }

        // Execute server
        process = process.execute([], project.buildDir)

        if ( project.vaadin.logToConsole ) {
            process.consumeProcessOutput(System.out, System.out)
        } else {
            File log = new File(logDir.canonicalPath + '/testbench-node.log')
            process.consumeProcessOutputStream(new FileOutputStream(log))
        }

        // Wait for node to start and connect to hub
        sleep(10000)

        project.logger.lifecycle("Testbench Node started on http://$host:$port/wd/hub")

    }

    /**
     * Terminates the Testbench node
     */
    void terminate() {
        process.in.close()
        process.out.close()
        process.err.close()
        process.destroy()
        process = null

        project.logger.lifecycle("Testbench node terminated.")
    }

    /**
     * Get the hostname or IP address
     */
    String getHost() {
        host.get()
    }

    /**
     * Set the hostname or IP address
     */
    void setHost(String host) {
        this.host.set(host)
    }

    /**
     * Get the port
     */
    Integer getPort() {
        port.get()
    }

    /**
     * Set the port
     */
    void setPort(Integer port) {
        this.port.set(port)
    }

    /**
     * Get the hub IP address
     */
    String getHub() {
        hub.get()
    }

    /**
     * Set the hub IP address
     */
    void setHub(String hub) {
        this.hub.set(hub)
    }

    /**
     * Get the enabled browsers map. See TestBenchNodeExtension for details.
     */
    List<Map> getBrowsers() {
        browsers.get()
    }

    /**
     * Set the enabled browsers map. See TestBenchNodeExtension for details.
     */
    void setBrowsers(List<Map> browsers) {
        this.browsers.set(browsers)
    }
}
