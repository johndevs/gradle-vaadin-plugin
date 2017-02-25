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

import fi.jasoft.plugin.MessageLogger
import groovy.transform.PackageScope
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet

/**
 * Plugin configuration extension
 */
@PluginConfiguration
class VaadinPluginExtension {

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String version = null

    /**
     * Should the plugin manage the vaadin dependencies
     */
    boolean manageDependencies = true

    /**
     * Should the plugin manage repositories
     */
    boolean manageRepositories = true

    /**
     * Configuration options for addons
     */
    final AddonConfiguration addon = new AddonConfiguration()

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

    /**
     * Should all logs output by the task be redirected to the console (if false output is redirected to file)
     */
    boolean logToConsole = false

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    boolean useClassPathJar = Os.isFamily(Os.FAMILY_WINDOWS)

    /**
     * Configuration options for GWT
     */
    @Deprecated
    final GWTConfiguration gwt

    /**
     * The configuration for Development mode
     */
    @Deprecated
    final DevelopmentModeConfiguration devmode

    /**
     * The configuration for the plugin itself
     */
    @Deprecated
    final VaadinPluginConfiguration plugin

    @PackageScope
    @Deprecated
    final Project project

    @Deprecated
    VaadinPluginExtension(Project project) {
        this.project = project
        plugin = new VaadinPluginConfiguration(project)
        devmode = new DevelopmentModeConfiguration(project)
        gwt = new GWTConfiguration(project)
    }

    /**
     * Should application be run in debug mode. When running in production set this to true
     */
    @Deprecated
    void debug(boolean debug) {
        project.vaadinRun.debug = debug
        isDebug()
    }
    @Deprecated
    void setDebug(boolean debug) {
        project.vaadinRun.debug = debug
        isDebug()
    }
    @Deprecated
    void isDebug() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debug',
                'This property has been replaced by vaadinRun.debug.')
        project.vaadinRun.debug
    }

    /**
     * The port the debugger listens to
     */
    @Deprecated
    void debugPort(int port) {
        project.vaadinRun.debugPort = port
        isDebugPort()
    }
    @Deprecated
    void setDebugPort(int port) {
        project.vaadinRun.debugPort = port
        isDebugPort()
    }
    @Deprecated
    boolean isDebugPort() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.debugPort',
                'This property has been replaced by vaadinRun.debugPort.')
        project.vaadinRun.debugPort
    }

    /**
     * The port the vaadin application should run on
     */
    @Deprecated
    void serverPort(int port) {
        project.vaadinRun.serverPort = port
        getServerPort()
    }
    @Deprecated
    void setServerPort(int port) {
        project.vaadinRun.serverPort = port
        getServerPort()
    }
    @Deprecated
    int getServerPort() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.serverPort',
                'This property has been replaced by vaadinRun.serverPort.')
        project.vaadinRun.serverPort
    }

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    @Deprecated
    void jvmArgs(String[] args) {
        project.vaadinRun.jvmArgs = args
        getJvmArgs()
    }
    @Deprecated
    void setJvmArgs(String[] args) {
        project.vaadinRun.jvmArgs = args
        getJvmArgs()
    }
    @Deprecated
    String[] getJvmArgs() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.jvmArgs',
                'This property has been replaced by vaadinRun.jvmArgs.')
        project.vaadinRun.jvmArgs
    }

    /**
     * Extra jvm args passed to the JVM running the Vaadin application
     */
    @Deprecated
    void widgetsetCDN(Boolean enabled) {
        project.vaadinCompile.widgetsetCDN = enabled
        getWidgetsetCDN()
    }
    @Deprecated
    void setWidgetsetCDN(Boolean enabled) {
        project.vaadinCompile.widgetsetCDN = enabled
        getWidgetsetCDN()
    }
    @Deprecated
    Boolean getWidgetsetCDN() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.widgetsetCDN',
                'This property has been replaced by vaadinCompile.widgetsetCDN.')
        project.vaadinCompile.widgetsetCDN
    }

    /**
     * Should the Vaadin client side profiler be used
     */
    @Deprecated
    void profiler(Boolean enabled) {
        project.vaadinCompile.profiler = enabled
        isProfiler()
    }
    @Deprecated
    void setProfiler(Boolean enabled) {
        project.vaadinCompile.profiler = enabled
        isProfiler()
    }
    @Deprecated
    Boolean isProfiler() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.profiler',
                'This property has been replaced by vaadinCompile.profiler.')
        project.vaadinCompile.profiler
    }


    /**
     * Should the plugin manage the widgetset (gwt.xml file)
     */
    @Deprecated
    void manageWidgetset(Boolean enabled) {
        project.vaadinCompile.manageWidgetset = enabled
        isManageWidgetset()
    }
    @Deprecated
    void setManageWidgetset(Boolean enabled) {
        project.vaadinCompile.manageWidgetset = enabled
        isManageWidgetset()
    }
    @Deprecated
    Boolean isManageWidgetset() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.manageWidgetset',
                'This property has been replaced by vaadinCompile.manageWidgetset.')
        project.vaadinCompile.manageWidgetset
    }

    /**
     * The widgetset to use for the project. Leave emptu for a pure server side project
     */
    @Deprecated
    void widgetset(String widgetset) {
        project.vaadinCompile.widgetset = widgetset
        getWidgetset()
    }
    @Deprecated
    void setWidgetset(String widgetset) {
        project.vaadinCompile.widgetset = widgetset
        getWidgetset()
    }
    @Deprecated
    String getWidgetset() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.widgetset',
                'This property has been replaced by automatic widgetset detection. ' +
                        'You should be able to remove this property definition completely. ' +
                        'If you do need to set it manually you can set vaadinCompile.widgetset.')
        project.vaadinCompile.widgetset
    }

    /**
     * The widgetset generator to use
     */
    @Deprecated
    void widgetsetGenerator(String widgetsetGenerator) {
        project.vaadinCompile.widgetsetGenerator = widgetsetGenerator
        getWidgetsetGenerator()
    }
    @Deprecated
    void setWidgetsetGenerator(String widgetsetGenerator) {
        project.vaadinCompile.widgetsetGenerator = widgetsetGenerator
        getWidgetsetGenerator()
    }
    @Deprecated
    String getWidgetsetGenerator() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.widgetsetGenerator',
                'This property has been replaced by vaadinCompile.widgetsetGenerator.')
        project.vaadinCompile.widgetsetGenerator
    }

    /**
     * Configuration options for TestBench
     */
    @Deprecated
    TestBenchConfiguration getTestbench() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.testbench',
                'This property has been replaced by vaadinTestbench')
        project.vaadinTestbench
    }
}
