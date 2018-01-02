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

import org.gradle.api.Project
import org.gradle.api.provider.PropertyState

/**
 * Configuration options for Vaadin Testbench
 *
 * @author John Ahlroos
 * @since 1.2
 */
class TestBenchExtension {

    static final String NAME = 'vaadinTestbench'

    private final PropertyState<Boolean> enabled
    private final PropertyState<String> version
    private final PropertyState<Boolean> runApplication

    TestBenchExtension(Project project) {
        enabled = project.property(Boolean)
        version = project.property(String)
        runApplication = project.property(Boolean)

        enabled.set(false)
        version.set('5.0.+')
        runApplication.set(true)
    }

    /**
     * Should Testbench be enabled when running tests
     */
    Boolean getEnabled() {
        enabled.get()
    }

    /**
     * Should Testbench be enabled when running tests
     */
    void setEnabled(Boolean enabled) {
        this.enabled.set(enabled)
    }

    /**
     * What version of testbench should be used
     */
    String getVersion() {
        version.get()
    }

    /**
     * What version of testbench should be used
     */
    void setVersion(String version) {
        this.version.set(version)
    }

    /**
     * Should the application be run before tests are run
     */
    Boolean getRunApplication() {
        runApplication.get()
    }

    /**
     * Should the application be run before tests are run
     */
    void setRunApplication(Boolean runApplication) {
        this.runApplication.set(runApplication)
    }
}
