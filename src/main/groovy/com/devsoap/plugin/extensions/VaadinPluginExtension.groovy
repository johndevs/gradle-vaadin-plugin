/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.extensions

import com.devsoap.plugin.Util
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Generic plugin configuration options
 *
 * @author John Ahlroos
 * @since 1.2
 */
class VaadinPluginExtension {

    static final NAME = 'vaadin'

    private static final String VAADIN_VERSION_PROPERTY = 'vaadinVersion'

    private final Property<String> version
    private final Property<Boolean> manageDependencies
    private final Property<Boolean> manageRepositories
    private final Property<SourceDirectorySet> mainSourceSet
    private final Property<SourceDirectorySet> mainTestSourceSet
    private final Property<Boolean> push
    private final Property<Boolean> logToConsole
    private final Property<Boolean> useClassPathJar

    private final Project project

    VaadinPluginExtension(Project project) {
        this.project = project

        version = project.objects.property(String)
        manageDependencies = project.objects.property(Boolean)
        manageRepositories = project.objects.property(Boolean)
        mainSourceSet = project.objects.property(SourceDirectorySet)
        mainTestSourceSet = project.objects.property(SourceDirectorySet)
        push = project.objects.property(Boolean)
        logToConsole = project.objects.property(Boolean)
        useClassPathJar = project.objects.property(Boolean)

        version.set(null)
        manageDependencies.set(true)
        manageRepositories.set(true)
        mainSourceSet.set(null)
        mainTestSourceSet.set(null)
        push.set(false)
        logToConsole.set(false)
        useClassPathJar.set(Os.isFamily(Os.FAMILY_WINDOWS))
    }

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    String getVersion() {
        // Use vaadin.version if set
        if(version.isPresent()){
            return version.get()
        }

        // else see if ext.vaadinVersion is set
        if(project.ext.properties.containsKey(VAADIN_VERSION_PROPERTY)) {
            return project.ext.get(VAADIN_VERSION_PROPERTY).toString()
        }

        // else fallback to default vaadin version
        Util.pluginProperties.getProperty('vaadin.defaultVersion')
    }

    /**
     * The vaadin version to use. By default latest Vaadin 7 version.
     */
    void setVersion(String version) {
        this.version.set(version)
    }

    /**
     * Should the plugin manage the vaadin dependencies
     */
    Boolean getManageDependencies() {
        manageDependencies.get()
    }

    /**
     * Should the plugin manage the vaadin dependencies
     */
    void setManageDependencies(Boolean enabled) {
        manageDependencies.set(enabled)
    }

    /**
     * Should the plugin manage repositories
     */
    Boolean getManageRepositories() {
        manageRepositories.get()
    }

    /**
     * Should the plugin manage repositories
     */
    void setManageRepositories(Boolean enabled) {
        manageRepositories.set(enabled)
    }

    /**
     * The directory for the main source set. By default src/main/java .
     */
    SourceDirectorySet getMainSourceSet() {
        mainSourceSet.getOrNull()
    }

    /**
     * The directory for the main source set. By default src/main/java .
     */
    void setMainSourceSet(SourceDirectorySet set) {
        mainSourceSet.set(set)
    }

    /**
     * The directory for the main test source set. By default src/test/java.
     */
    SourceDirectorySet getMainTestSourceSet() {
        mainTestSourceSet.getOrNull()
    }

    /**
     * The directory for the main test source set. By default src/test/java.
     */
    void setMainTestSourceSet(SourceDirectorySet set) {
        mainTestSourceSet.set(set)
    }

    /**
     * Should server push be enabled.
     */
    Boolean getPush() {
        push.get()
    }

    /**
     * Should server push be enabled.
     */
    void setPush(Boolean enabled) {
        push.set(enabled)
    }

    /**
     * Should all logs output by the task be redirected to the console (if false output is redirected to file)
     */
    Boolean getLogToConsole() {
        logToConsole.get()
    }

    /**
     * Should all logs output by the task be redirected to the console (if false output is redirected to file)
     */
    void setLogToConsole(Boolean log) {
        logToConsole.set(log)
    }

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    Boolean getUseClassPathJar() {
        useClassPathJar.get()
    }

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    Provider<Boolean> getUseClassPathJarProvider() {
        useClassPathJar
    }

    /**
     * Should a classpath Jar be used to shorten the classpath.
     */
    void setUseClassPathJar(Boolean enabled) {
        useClassPathJar.set(enabled)
    }
}
