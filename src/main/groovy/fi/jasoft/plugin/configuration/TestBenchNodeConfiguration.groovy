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
 * Configuration options for the testbench node
 */
class TestBenchNodeConfiguration {

    /**
     * Should the node be enabled
     */
    boolean enabled = false

    /**
     * The hostname of the node
     */
    String host = 'localhost'

    /**
     * The port of the node
     */
    int port = 4445

    /**
     * The hub to connect to.
     */
    String hub = 'http://localhost:4444/grid/register'

    /* A list of browser configurations:
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
    List<Map> browsers = []

    /**
     * Should the node be started when launching the tests
     *
     * @param enabled
     *      <code>true</code> if it should.
     */
    void enabled(boolean enabled) {
        this.enabled = enabled
    }

    /**
     * The host name or ip address where the node should be run
     *
     * @param host
     *      Host name or ip address for the node
     */
    void host(String host) {
        this.host = host
    }

    /**
     * The port on which the node should be run
     *
     * @param port
     *      The port number for the node
     */
    void port(int port) {
        this.port = port
    }

    /* A list of browser configurations:
     * <p>
     * See http://code.google.com/p/selenium/wiki/Grid2 for more information about available browsers and
     * settings. The browser setting will be converted into the -browser parameter for the node.
     *
     * @param browser e.g.
     *
     *   browser = [
     *       [ browserName: 'firefox', version: 3.6, maxInstances: 5, platform: 'LINUX' ],
     *       [ browserName: 'chrome', version: 22, maxInstances: 1, platform: 'WINDOWS' ]
     *   ]
     *
     */

    void browsers(List<Map> browsers) {
        this.browsers = browsers
    }

    /**
     * The url for where the hub is running
     *
     * @param hub
     *      URL in the for http://localhost:4444/grid/register
     */
    void hub(String hub) {
        this.hub = hub
    }
}
