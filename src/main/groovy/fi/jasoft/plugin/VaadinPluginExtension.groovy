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

class VaadinPluginExtension {

    String widgetset

    String widgetsetGenerator = null

    String version = "7+"

    // Only used by the wtp plugin, should be removed
    @Deprecated
    String servletVersion = "3.0"

    boolean debug = true

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

    TestBench testbench = new TestBench()

    SourceDirectorySet mainSourceSet = null

    boolean push = false

    /*
     * Assignment methods
     */

    void widgetset(String widgetset) {
        this.widgetset = widgetset
    }

    void widgetsetGenerator(String widgetsetGenerator) {
        this.widgetsetGenerator = widgetsetGenerator
    }

    void version(String version) {
        this.version = version
    }

    @Deprecated
    void servletVersion(String servletVersion) {
        this.servletVersion = servletVersion
    }

    void debug(boolean debug) {
        this.debug = debug
    }

    void debugPort(int port) {
        this.debugPort = port
    }

    void manageWidgetset(boolean manage) {
        this.manageWidgetset = manage
    }

    void manageDependencies(boolean manage) {
        this.manageDependencies = manage
    }

    void serverPort(int port) {
        this.serverPort = port;
    }

    void jvmArgs(String[] args) {
        this.jvmArgs = args
    }

    void push(boolean push) {
        this.push = push
    }

    void mainSourceSet(SourceDirectorySet set) {
        this.mainSourceSet = set
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

    TestBench testbench(closure) {
        closure.delegate = testbench
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /*
     * Inner classes
     */

    class GWT {
        String style = "OBF"
        int optimize = 0
        boolean logging = true
        String logLevel = "INFO"
        int localWorkers = Runtime.getRuntime().availableProcessors()
        boolean draftCompile = false
        boolean strict = false
        String userAgent = null
        String[] jvmArgs = null
        String version = "2.3.0"
        String extraArgs
        String[] sourcePaths = ['client', 'shared']
        boolean collapsePermutations = false
        String[] extraInherits


        void style(String style) {
            this.style = style
        }

        void optimize(int optimize) {
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

        void extraInherits(String[] inherits) {
            this.extraInherits = inherits
        }

        void sourcePaths(String[] sourcePaths) {
            this.sourcePaths = sourcePaths
        }

        void collapsePermutations(boolean collapse) {
            this.collapsePermutations = collapse
        }

        void logging(boolean logging) {
            this.logging = logging
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
        boolean openInBrowser = true

        void logToConsole(boolean logToConsole) {
            this.logToConsole = logToConsole
        }

        void terminateOnEnter(boolean terminateOnEnter) {
            this.terminateOnEnter = terminateOnEnter
        }

        void openInBrowser(boolean openInBrowser) {
            this.openInBrowser = openInBrowser
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

    class TestBench {

        class TestBenchHub {
            boolean enabled = false
            String host = 'localhost'
            int port = 4444

            void enabled(boolean enabled) {
                this.enabled = enabled
            }

            void host(String host){
                this.host = host
            }

            void port(int port){
                this.port = port
            }
        }

        class TestBenchNode {
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
            void host(String host){
                this.host = host
            }

            /**
             * The port on which the node should be run
             *
             * @param port
             *      The port number for the node
             */
            void port(int port){
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
            void hub(String hub){
                this.hub = hub
            }
        }

        boolean enabled = false
        String version = "3.+"
        boolean runApplication = true

        TestBenchHub hub = new TestBenchHub()
        TestBenchNode node = new TestBenchNode()

        void enabled(boolean enabled) {
            this.enabled = enabled
        }

        void version(String version) {
            this.version = version
        }

        void runApplication(boolean run) {
            this.runApplication = run
        }

        TestBenchHub hub(closure) {
            closure.delegate = hub
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }

        TestBenchNode node(closure) {
            closure.delegate = node
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
    }

}
