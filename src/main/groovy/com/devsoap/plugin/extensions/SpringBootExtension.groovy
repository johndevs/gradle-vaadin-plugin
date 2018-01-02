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

import javax.validation.constraints.NotNull

/**
 * SpringBoot-related options
 *
 * @author Mac Przepióra
 * @since 1.2
 */
class SpringBootExtension {

    static final NAME = "vaadinSpringBoot"

    private final PropertyState<String> starterVersion

    SpringBootExtension(Project project) {
        starterVersion = project.property(String)
        starterVersion.set('2.+')
    }

    /**
     * The Vaadin Spring Boot Starter version to use. By
     * default latest 2.x version.
     */
    String getStarterVersion() {
        starterVersion.get()
    }

    /**
     * The Vaadin Spring Boot Starter version to use. By
     * default latest 2.x version.
     */
    String setStarterVersion(@NotNull String springBootStarterVersion) {
        this.starterVersion.set(springBootStarterVersion)
    }
}
