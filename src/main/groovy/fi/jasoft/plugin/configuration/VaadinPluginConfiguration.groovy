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
 * General configuration options for the plugin itself
 */
class VaadinPluginConfiguration {

    /**
     * Should all logs output by the task be redirected to the console (if false output is redirected to file)
     */
    boolean logToConsole = false

    /**
     * Should the application be opened in a browser when it has been launched
     */
    boolean openInBrowser = true

    /**
     * The directory where Eclipse will output its compiled classes.
     */
    String eclipseOutputDir = null

    /**
     * Should jetty restart when a class is changed in the build directory.
     */
    boolean jettyAutoRefresh = true

    /**
     * @see VaadinPluginConfiguration#logToConsole
     *
     * @param logToConsole
     */
    void logToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole
    }

    /**
     * @see VaadinPluginConfiguration#openInBrowser
     *
     * @param openInBrowser
     */
    void openInBrowser(boolean openInBrowser) {
        this.openInBrowser = openInBrowser
    }

    /**
     * @see VaadinPluginConfiguration#eclipseOutputDir
     *
     * @param eclipseOutputDir
     */
    void eclipseOutputDir(String eclipseOutputDir) {
        this.eclipseOutputDir = eclipseOutputDir
    }

    /**
     * @see VaadinPluginConfiguration#jettyAutoRefresh
     *
     * @param jettyAutoRefresh
     */
    void jettyAutoRefresh(boolean jettyAutoRefresh) {
        this.jettyAutoRefresh = jettyAutoRefresh
    }

}
