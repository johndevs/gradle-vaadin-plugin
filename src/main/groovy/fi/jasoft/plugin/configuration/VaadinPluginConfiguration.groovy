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
package fi.jasoft.plugin.configuration

import fi.jasoft.plugin.MessageLogger
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * General configuration options for the plugin itself
 */
@PluginConfiguration
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
     *
     * @deprecated
     */
    @Deprecated
    void jettyAutoRefresh(boolean refresh){
        isJettyAutoRefresh()
        serverRestart = refresh
    }
    @Deprecated
    void setJettyAutoRefresh(boolean refresh){
        isJettyAutoRefresh()
        serverRestart = refresh
    }
    @Deprecated
    boolean isJettyAutoRefresh(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.plugin.jettyAutoRefresh',
                'This property has been replaced by vaadin.plugin.serverRestart.')
        serverRestart
    }

    /**
     * Should the server restart after every change.
     */
    boolean serverRestart = true

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
     * Should theme be recompiled when SCSS file is changes.
     */
    boolean themeAutoRecompile = true

    /**
     * Custom directory where themes can be found
     */
    String themesDirectory = null

    /**
     * Theme compiler to use
     * <p>
     *     Available options are
     *     <ul>
     *         <li>vaadin - Vaadin's SASS Compiler</li>
     *         <li>compass - Compass's SASS Compiler</li>
     *     </ul>
     */
    String themeCompiler = 'vaadin'

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    boolean useClassPathJar = Os.isFamily(Os.FAMILY_WINDOWS)
}
