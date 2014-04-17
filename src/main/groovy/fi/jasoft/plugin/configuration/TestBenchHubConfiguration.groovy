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
package fi.jasoft.plugin.configuration

/**
 * Configuration options for the testbench hub
 */
class TestBenchHubConfiguration {

    /**
     * Should the testbench hub be enabled when tests are run
     */
    boolean enabled = false

    /**
     * The host the hub should be run on
     */
    String host = 'localhost'

    /**
     * The port the hub should be run on
     */
    int port = 4444

    /**
     * @see TestBenchHubConfiguration#enabled
     *
     * @param enabled
     */
    void enabled(boolean enabled) {
        this.enabled = enabled
    }

    /**
     * @see TestBenchHubConfiguration#host
     *
     * @param host
     */
    void host(String host) {
        this.host = host
    }

    /**
     * @see TestBenchHubConfiguration#port
     *
     * @param port
     */
    void port(int port) {
        this.port = port
    }
}
