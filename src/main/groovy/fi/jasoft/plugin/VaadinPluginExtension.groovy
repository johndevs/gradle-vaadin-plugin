/*
* Copyright 2013 John Ahlroos
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
package fi.jasoft.plugin

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet;

class VaadinPluginExtension{

	String widgetset

	String widgetsetGenerator = null

	String version = "7+"

	String servletVersion = "2.5"

	int debugPort = 8000

	boolean manageWidgetset = true

	boolean manageDependencies = true

	int serverPort = 8080

	String[] jvmArgs = null

    JRebel jrebel = new JRebel()

    DevMode devmode = new DevMode()

    VaadinPluginConfiguration plugin = new VaadinPluginConfiguration()

    Addon addon = new Addon()

    GWT gwt = new GWT()

    SourceDirectorySet mainSourceSet = null

    /*
     * Assignment methods
     */
    void widgetset(String widgetset){
        this.widgetset = widgetset
    }

    void widgetsetGenerator(String widgetsetGenerator){
        this.widgetsetGenerator = widgetsetGenerator
    }

    void version(String version){
        this.version = version
    }

    @Deprecated
    void servletVersion(String servletVersion) {
        this.servletVersion = servletVersion
    }

    void debugPort(int port){
        this.debugPort = port
    }

    void manageWidgetset(boolean manage){
        this.manageWidgetset = manage
    }

    void manageDependencies(boolean manage){
        this.manageDependencies = manage
    }

    void serverPort(int port){
        this.serverPort = port;
    }

    void jvmArgs(String[] args){
        this.jvmArgs = args
    }

    /*
     * Closures
     */

    Addon addon(closure) {
        closure.delegate = addon
        closure()
    }

    GWT gwt(closure) {
        closure.delegate = gwt
        closure()
    }

    VaadinPluginConfiguration plugin(closure) {
        closure.delegate = plugin
        closure()
    }

    DevMode devmode(closure) {
        closure.delegate = devmode
        closure()
    }

    JRebel jrebel(closure) {
        closure.delegate = jrebel
        closure()
    }


    /*
     * Inner classes
     */

	class GWT{
		String style = "OBF"
		String optimize = 0
		String logLevel = "INFO"
		int localWorkers = Runtime.getRuntime().availableProcessors()
		boolean draftCompile = false
		boolean strict = false
		String userAgent = "ie8,ie9,gecko1_8,safari,opera"
		String[] jvmArgs = null
		String version = "2.3.0"
		String extraArgs
        String[] sourcePaths = ['client', 'shared']
        boolean collapsePermutations = false

        void style(String style) {
            this.style = style
        }

        void optimize(String optimize) {
            this.optimize = optimize
        }

        void logLevel(String logLevel) {
            this.logLevel = logLevel
        }

        void localWorkers(int localWorkers) {
            this.localWorkers = localWorkers
        }

        void draftCompile(boolean draftCompile) {
            this.draftCompile = draftCompile
        }

        void strict(boolean strict) {
            this.strict = strict
        }

        void userAgent(String userAgent) {
            this.userAgent = userAgent
        }

        void jvmArgs(String[] jvmArgs) {
            this.jvmArgs = jvmArgs
        }

        void version(String version) {
            this.version = version
        }

        void extraArgs(String extraArgs) {
            this.extraArgs = extraArgs
        }

        void sourcePaths(String[] sourcePaths) {
            this.sourcePaths = sourcePaths
        }

        void collapsePermutations(boolean collapse) {
            this.collapsePermutations = collapse
        }
    }

	class DevMode {
		boolean noserver = false
		boolean superDevMode = false
		String bindAddress = '127.0.0.1'
		int codeServerPort = 9997

        void noserver(boolean noserver) {
            this.noserver = noserver
        }

        void superDevMode(boolean superDevMode) {
            this.superDevMode = superDevMode
        }

        void bindAddress(String bindAddress) {
            this.bindAddress = bindAddress
        }

        void codeServerPort(int codeServerPort) {
            this.codeServerPort = codeServerPort
        }
    }

    class VaadinPluginConfiguration {

        boolean terminateOnEnter = true
        boolean logToConsole = false

        void logToConsole (boolean logToConsole) {
            this.logToConsole = logToConsole
        }

        void terminateOnEnter(boolean terminateOnEnter) {
            this.terminateOnEnter = terminateOnEnter
        }
    }

    class Addon {
        String author = ''
        String license = ''
        String title = ''

        void author(String author) {
            this.author = author
        }

        void license(String license) {
            this.license = license
        }

        void title(String title) {
            this.title = title
        }
    }

    class JRebel {
        boolean enabled = false
        String location

        void enabled(boolean enabled) {
            this.enabled = enabled
        }

        void location(String location) {
            this.location = location
        }
    }

}
