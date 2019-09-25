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
package com.devsoap.plugin.extensions

import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Configuration options for the testbench node
 *
 * @author John Ahlroos
 * @since 1.2
 */
class TestBenchNodeExtension {

    static final String NAME = 'vaadinTestbenchNode'

    private final Property<Boolean> enabled
    private final Property<String> host
    private final Property<Integer> port
    private final Property<String> hub
    private final MapProperty<String, String> browsers

    TestBenchNodeExtension(Project project) {

        enabled = project.objects.property(Boolean)
        host = project.objects.property(String)
        port = project.objects.property(Integer)
        hub = project.objects.property(String)
        browsers = project.objects.mapProperty(String, String)

        enabled.set(false)
        host.set('localhost')
        port.set(4445)
        hub.set('http://localhost:4444/grid/register')
        browsers.empty()
    }

    /**
     * Should the node be enabled
     */
    boolean getEnabled() {
        enabled.get()
    }

    /**
     * Should the node be enabled
     */
    void setEnabled(Boolean enabled) {
        this.enabled.set(enabled)
    }

    /**
     * The hostname of the node
     */
    String getHost() {
        host.get()
    }

    /**
     * Get the provider for the host value
     */
    Provider<String> getHostProvider() {
        host
    }

    /**
     * The hostname of the node
     */
    void setHost(String host) {
        this.host.set(host)
    }

    /**
     * The port of the node
     */
    Integer getPort() {
        port.get()
    }

    /**
     * Get the provider for the port value
     */
    Provider<Integer> getPortProvider() {
        port
    }

    /**
     * The port of the node
     */
    void setPort(Integer port) {
        this.port.set(port)
    }

    /**
     * The hub to connect to.
     */
    String getHub() {
        hub.get()
    }

    /**
     * The hub to connect to.
     */
    void setHub(String hub) {
        this.hub.set(hub)
    }

    /**
     * Get the provider for the hub value
     */
    Provider<String> getHubProvider() {
        hub
    }

    /** A list of browser configurations:
     * e.g.
     *
     *   browser = [
     *       [ browserName: 'firefox', version: 3.6, maxInstances: 5, platform: 'LINUX' ],
     *       [ browserName: 'chrome', version: 22, maxInstances: 1, platform: 'WINDOWS' ]
     *   ]
     *
     *   See http://code.google.com/p/selenium/wiki/Grid2 for more information about available browsers and
     *   settings. The browser setting will be stringingified into the -browser parameter for the hub.
     */
    List<Map> getBrowsers() {
        browsers.get()
    }

    /** A list of browser configurations:
     * e.g.
     *
     *   browser = [
     *       [ browserName: 'firefox', version: 3.6, maxInstances: 5, platform: 'LINUX' ],
     *       [ browserName: 'chrome', version: 22, maxInstances: 1, platform: 'WINDOWS' ]
     *   ]
     *
     *   See http://code.google.com/p/selenium/wiki/Grid2 for more information about available browsers and
     *   settings. The browser setting will be stringingified into the -browser parameter for the hub.
     */
    void setBrowsers(List<Map> browsers) {
        this.browsers.set(browsers)
    }

    /**
     * Get the provider for the browsers value
     */
    Provider<List<Map>> getBrowsersProvider() {
        browsers
    }
}
