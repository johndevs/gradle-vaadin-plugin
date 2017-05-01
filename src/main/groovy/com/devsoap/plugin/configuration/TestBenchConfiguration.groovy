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

import com.devsoap.plugin.MessageLogger
import com.devsoap.plugin.configuration.PluginConfiguration
import org.gradle.api.Project

/**
 * Configuration options for Testbench
 */
@PluginConfiguration
@PluginConfigurationName('vaadinTestbench')
class TestBenchConfiguration {

    /**
     * Should Testbench be enabled when running tests
     */
    boolean enabled = false

    /**
     * What version of testbench should be used
     */
    String version = "5.0.+"

    /**
     * Should the application be run before tests are run
     */
    boolean runApplication = true

    @Deprecated
    transient Project project

    @Deprecated
    TestBenchConfiguration(Project project) {
        this.project = project
    }

    @Deprecated
    TestBenchHubConfiguration getHub() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.testbench.hub',
                'This property has been replaced by vaadinTestbenchHub')
        project.vaadinTestbenchHub
    }

    @Deprecated
    TestBenchNodeConfiguration getNode() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.testbench.node',
                'This property has been replaced by vaadinTestbenchNode')
        project.vaadinTestbenchNode
    }
}
