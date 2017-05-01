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
package com.devsoap.plugin.configuration

import com.devsoap.plugin.configuration.PluginConfiguration
import com.devsoap.plugin.tasks.CompileWidgetsetTask

/**
 * Created by john on 3/7/16.
 */
@PluginConfiguration
@PluginConfigurationName(CompileWidgetsetTask.NAME)
class CompileWidgetsetConfiguration {

    /**
     * Compilation style
     */
    String style = 'OBF'

    /**
     * Should the compilation result be optimized
     */
    int optimize = 0

    /**
     * Should logging be enabled
     */
    boolean logging = true

    /**
     * The log level. Possible levels NONE,DEBUG,TRACE,INFO
     */
    String logLevel = 'INFO'

    /**
     * Amount of local workers used when compiling. By default the amount of processors.
     */
    int localWorkers = Runtime.getRuntime().availableProcessors()

    /**
     * Should draft compile be used
     */
    boolean draftCompile = true

    /**
     * Should strict compiling be used
     */
    boolean strict = true

    /**
     * What user agents (browsers should be used. By defining null all user agents are used.
     */
    String userAgent = null

    /**
     * Extra jvm arguments passed the JVM running the compiler
     */
    String[] jvmArgs = null

    /**
     * Extra arguments passed to the compiler
     */
    String[] extraArgs = null

    /**
     * Source paths where the compiler will look for source files
     */
    String[] sourcePaths = ['client', 'shared']

    /**
     * Should the compiler permutations be collapsed to save time
     */
    boolean collapsePermutations = true

    /**
     * Extra module inherits
     */
    String[] extraInherits

    /**
     * Should GWT be placed first in the classpath when compiling the widgetset.
     */
    boolean gwtSdkFirstInClasspath = true

    /**
     * (Optional) root directory, for generated files; default is the web-app dir from the WAR plugin
     */
    String outputDirectory = null

    /**
     * Use the widgetset CDN located at cdn.virit.in
     */
    boolean widgetsetCDN = false

    /**
     * Proxy settings for widgetset cdn compilation
     */
    final WidgetsetCDNConfiguration widgetsetCDNConfig = new WidgetsetCDNConfiguration()

    /**
     * Should the Vaadin client side profiler be used
     */
    boolean profiler = false

    /**
     * Should the plugin manage the widgetset (gwt.xml file)
     */
    boolean manageWidgetset = true

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    String widgetset = null

    /**
     * The widgetset generator to use
     */
    String widgetsetGenerator = null
}
