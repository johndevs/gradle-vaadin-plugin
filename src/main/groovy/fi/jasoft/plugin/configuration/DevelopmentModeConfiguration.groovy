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
 * Configuration for Development Mode
 */
class DevelopmentModeConfiguration {

    /**
     * Should the internal Jetty server be used.
     */
    boolean noserver = false

    /**
     * Should super devmode be available
     */
    boolean superDevMode = false

    /**
     * To what host or ip should development mode bind itself to. By default localhost.
     */
    String bindAddress = '127.0.0.1'

    /**
     * To what port should development mode bind itself to.
     */
    int codeServerPort = 9997

    /**
     * @see DevelopmentModeConfiguration#noserver
     *
     * @param noserver
     */
    void noserver(boolean noserver) {
        this.noserver = noserver
    }

    /**
     * @see DevelopmentModeConfiguration#superDevMode
     *
     * @param superDevMode
     */
    void superDevMode(boolean superDevMode) {
        this.superDevMode = superDevMode
    }

    /**
     * @see DevelopmentModeConfiguration#bindAddress
     *
     * @param bindAddress
     */
    void bindAddress(String bindAddress) {
        this.bindAddress = bindAddress
    }

    /**
     * @see DevelopmentModeConfiguration#codeServerPort
     *
     * @param codeServerPort
     */
    void codeServerPort(int codeServerPort) {
        this.codeServerPort = codeServerPort
    }
}
