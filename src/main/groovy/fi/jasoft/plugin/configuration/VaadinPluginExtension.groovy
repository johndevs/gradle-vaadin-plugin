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
@PluginConfiguration
class VaadinPluginExtension {

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    String widgetset

    /**
     * The widgetset generator to use
     */
    String widgetsetGenerator = null

    /*
     * Use the widgetset CDN located at cdn.virit.in
     */
    boolean widgetsetCDN = false

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String version = "7.3.+"

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    boolean debug = true

    /**
     * Should the Vaadin client side profiler be used
     */
    boolean profiler = false

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
    final JRebelConfiguration jrebel = new JRebelConfiguration()

    /**
     * The configuration for Development mode
     */
    final DevelopmentModeConfiguration devmode = new DevelopmentModeConfiguration()

    /**
     * The configuration for the plugin itself
     */
    final VaadinPluginConfiguration plugin = new VaadinPluginConfiguration()

    /**
     * Configuration options for addons
     */
    final AddonConfiguration addon = new AddonConfiguration()

    /**
     * Configuration options for GWT
     */
    final GWTConfiguration gwt = new GWTConfiguration()

    /**
     * Configuration options for TestBench
     */
    final TestBenchConfiguration testbench = new TestBenchConfiguration()

    /**
     * The directory for the main source set. By default src/main/java .
     */
    SourceDirectorySet mainSourceSet = null

    /**
     * THe directory for the main test source set. By default src/test/java.
     */
    SourceDirectorySet mainTestSourceSet = null

    /**
     * Should server push be enabled.
     */
    boolean push = false
}
