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

import org.gradle.api.file.SourceDirectorySet

/**
 * Plugin configuration extension
 */
class VaadinPluginExtension {

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    String widgetset

    /**
     * The widgetset generator to use
     */
    String widgetsetGenerator = null

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String version = "7.1.+"

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    boolean debug = true

    /**
     * Should the Vaadin client side profiler be used
     */
    boolean profiler = true

    /**
     * The port the debugger listens to
     */
    int debugPort = 8000

    /**
     * Should the plugin manage the widgetset (gwt.xml file)
     */
    boolean manageWidgetset = true

    /**
     * Should the plugin manage the vaadin dependencies
     */
    boolean manageDependencies = true

    /**
     * Should the plugin manage repositories
     */
    boolean manageRepositories = true

    /**
     * The port the vaadin application should run on
     */
    int serverPort = 8080

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    String[] jvmArgs = null

    /**
     * The configuration for JRebel
     */
    JRebelConfiguration jrebel = new JRebelConfiguration()

    /**
     * The configuration for Development mode
     */
    DevelopmentModeConfiguration devmode = new DevelopmentModeConfiguration()

    /**
     * The configuration for the plugin itself
     */
    VaadinPluginConfiguration plugin = new VaadinPluginConfiguration()

    /**
     * Configuration options for addons
     */
    AddonConfiguration addon = new AddonConfiguration()

    /**
     * Configuration options for GWT
     */
    GWTConfiguration gwt = new GWTConfiguration()

    /**
     * Configuration options for TestBench
     */
    TestBenchConfiguration testbench = new TestBenchConfiguration()

    /**
     * The directory for the main source set. By default src/main/java .
     */
    SourceDirectorySet mainSourceSet = null

    /**
     * Should server push be enabled.
     */
    boolean push = false

    /**
     * @see VaadinPluginExtension#widgetset
     *
     * @param widgetset
     */
    void widgetset(String widgetset) {
        this.widgetset = widgetset
    }

    /**
     * @see VaadinPluginExtension#widgetsetGenerator
     *
     * @param widgetsetGenerator
     */
    void widgetsetGenerator(String widgetsetGenerator) {
        this.widgetsetGenerator = widgetsetGenerator
    }

    /**
     * @see VaadinPluginExtension#version
     *
     * @param version
     */
    void version(String version) {
        this.version = version
    }

    /**
     * @see VaadinPluginExtension#debug
     *
     * @param debug
     */
    void debug(boolean debug) {
        this.debug = debug
    }

    /**
     * @see VaadinPluginExtension#profiler
     *
     * @param profilerEnabled
     */
    void profiler(boolean profilerEnabled) {
        this.profiler = profilerEnabled
    }

    /**
     * @see VaadinPluginExtension#debugPort
     *
     * @param port
     */
    void debugPort(int port) {
        this.debugPort = port
    }

    /**
     * @see VaadinPluginExtension#manageWidgetset
     *
     * @param manage
     */
    void manageWidgetset(boolean manage) {
        this.manageWidgetset = manage
    }

    /**
     * @see VaadinPluginExtension#manageDependencies
     *
     * @param manage
     */
    void manageDependencies(boolean manage) {
        this.manageDependencies = manage
    }

    /**
     * @see VaadinPluginExtension#manageRepositories
     *
     * @param manage
     */
    void manageRepositories(boolean manage) {
        this.manageRepositories = manage
    }

    /**
     * @see VaadinPluginExtension#serverPort
     *
     * @param port
     */
    void serverPort(int port) {
        this.serverPort = port;
    }

    /**
     * @see VaadinPluginExtension#jvmArgs
     *
     * @param args
     */
    void jvmArgs(String[] args) {
        this.jvmArgs = args
    }

    /**
     * @see VaadinPluginExtension#push
     *
     * @param push
     */
    void push(boolean push) {
        this.push = push
    }

    /**
     * @see VaadinPluginExtension#mainSourceSet
     *
     * @param set
     */
    void mainSourceSet(SourceDirectorySet set) {
        this.mainSourceSet = set
    }

    /**
     * @see VaadinPluginExtension#addon
     *
     * @param closure
     * @return
     */
    AddonConfiguration addon(closure) {
        closure.delegate = addon
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see VaadinPluginExtension#gwt
     *
     * @param closure
     * @return
     */
    GWTConfiguration gwt(closure) {
        closure.delegate = gwt
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see VaadinPluginExtension#plugin
     *
     * @param closure
     * @return
     */
    VaadinPluginConfiguration plugin(closure) {
        closure.delegate = plugin
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see VaadinPluginExtension#devmode
     *
     * @param closure
     * @return
     */
    DevelopmentModeConfiguration devmode(closure) {
        closure.delegate = devmode
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see VaadinPluginExtension#jrebel
     *
     * @param closure
     * @return
     */
    JRebelConfiguration jrebel(closure) {
        closure.delegate = jrebel
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    /**
     * @see VaadinPluginExtension#testbench
     *
     * @param closure
     * @return
     */
    TestBenchConfiguration testbench(closure) {
        closure.delegate = testbench
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
}
