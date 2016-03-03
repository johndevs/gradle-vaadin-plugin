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
import groovy.transform.PackageScope
import org.gradle.api.GradleException
import org.gradle.api.Project
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

    /**
     * Use the widgetset CDN located at cdn.virit.in
     */
    boolean widgetsetCDN = false

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String version = null

    /**
     * Should the Vaadin client side profiler be used
     */
    boolean profiler = false

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
    final VaadinPluginConfiguration plugin

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

    @Deprecated
    final Project project

    @Deprecated
    VaadinPluginExtension(Project project){
        this.project = project
        plugin = new VaadinPluginConfiguration(project)
    }

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    @Deprecated
    void debug(boolean debug){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debug',
                'This property has been replaced by vaadinRun.configuration.debug.')
        project.vaadinRun.configuration.debug = debug
    }
    @Deprecated
    void setDebug(boolean debug){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debug',
                'This property has been replaced by vaadinRun.configuration.debug.')
        project.vaadinRun.configuration.debug = debug
    }
    @Deprecated
    void isDebug(boolean debug){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debug',
                'This property has been replaced by vaadinRun.configuration.debug.')
        project.vaadinRun.configuration.debug
    }

    /**
     * The port the debugger listens to
     */
    @Deprecated
    void debugPort(int port){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debugPort',
                'This property has been replaced by vaadinRun.configuration.debugPort.')
        project.vaadinRun.configuration.debugPort = port
    }
    @Deprecated
    void setDebugPort(int port){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debugPort',
                'This property has been replaced by vaadinRun.configuration.debugPort.')
        project.vaadinRun.configuration.debugPort = port
    }
    @Deprecated
    boolean isDebugPort(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debugPort',
                'This property has been replaced by vaadinRun.configuration.debugPort.')
        project.vaadinRun.configuration.debugPort
    }

    /**
     * The port the vaadin application should run on
     */
    @Deprecated
    void serverPort(int port){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.serverPort',
                'This property has been replaced by vaadinRun.configuration.serverPort.')
        project.vaadinRun.configuration.serverPort = port
    }
    @Deprecated
    void setServerPort(int port){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.serverPort',
                'This property has been replaced by vaadinRun.configuration.serverPort.')
        project.vaadinRun.configuration.serverPort = port
    }
    @Deprecated
    int getServerPort(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.serverPort',
                'This property has been replaced by vaadinRun.configuration.serverPort.')
        project.vaadinRun.configuration.serverPort
    }

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    @Deprecated
    void jvmArgs(String[] args){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.jvmArgs',
                'This property has been replaced by vaadinRun.configuration.jvmArgs.')
        project.vaadinRun.configuration.jvmArgs = args
    }
    @Deprecated
    void setJvmArgs(String[] args){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.jvmArgs',
                'This property has been replaced by vaadinRun.configuration.jvmArgs.')
        project.vaadinRun.configuration.jvmArgs = args
    }
    @Deprecated
    String[] getJvmArgs(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.jvmArgs',
                'This property has been replaced by vaadinRun.configuration.jvmArgs.')
        project.vaadinRun.configuration.jvmArgs
    }
}
