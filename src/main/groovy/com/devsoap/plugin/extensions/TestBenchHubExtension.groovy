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
package com.devsoap.plugin.extensions

import org.gradle.api.Project
import org.gradle.api.provider.PropertyState

/**
 * Configuration options for the testbench hub
 */
class TestBenchHubExtension {

    static final String NAME = 'vaadinTestbenchHub'

    final PropertyState<Boolean> enabled
    final PropertyState<String> host
    final PropertyState<Integer> port

    TestBenchHubExtension(Project project) {
        enabled = project.property(Boolean)
        host = project.property(String)
        port = project.property(Integer)

        enabled.set(false)
        host.set('localhost')
        port.set(4444)
    }

    /**
     * Should the testbench hub be enabled when tests are run
     */
    Boolean getEnabled() {
        enabled.get()
    }

    /**
     * Should the testbench hub be enabled when tests are run
     */
    void setEnabled(Boolean enabled) {
        this.enabled.set(enabled)
    }

    /**
     * The host the hub should be run on
     */
    String getHost() {
        host.get()
    }

    /**
     * The host the hub should be run on
     */
    void setHost(String host) {
        this.host.set(host)
    }

    /**
     * The port the hub should be run on
     */
    Integer getPort() {
        port.get()
    }

    /**
     * The port the hub should be run on
     */
    void setPort(Integer port) {
        this.port.set(port)
    }
}
