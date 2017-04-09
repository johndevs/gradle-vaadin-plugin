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
package fi.jasoft.plugin.configuration

import fi.jasoft.plugin.tasks.RunTask

/**
 * Configuration class for configuring the Application Server
 */
@PluginConfiguration
@PluginConfigurationName(RunTask.NAME)
class ApplicationServerConfiguration {

    /**
     * Application server to use.
     * <p>
     * Available options are
     * <ul>
     *     <li>payara - Webserver with EJB/CDI support</li>
     *     <li>jetty - Plain J2EE web server</li>
     * </ul>
     * Default server is payara.
     */
    String server = 'payara'

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    Boolean debug = true

    /**
     * The port the debugger listens to
     */
    Integer debugPort = 8000

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    String[] jvmArgs = null

    /**
     * Should the server restart after every change.
     */
    boolean serverRestart = true

    /**
     * The port the vaadin application should run on
     */
    Integer serverPort = 8080

    /**
     * Should theme be recompiled when SCSS file is changes.
     */
    boolean themeAutoRecompile = true

    /**
     * Should the application be opened in a browser when it has been launched
     */
    boolean openInBrowser = true

    /**
     * The directory where compiled application classes are found
     */
    String classesDir = null
}
