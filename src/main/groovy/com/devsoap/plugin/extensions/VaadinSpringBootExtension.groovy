package com.devsoap.plugin.extensions

import org.gradle.api.Project
import org.gradle.api.provider.PropertyState

/**
 * SpringBoot-related options
 *
 * @author Mac Przepióra
 */
class VaadinSpringBootExtension {
    static final NAME = "vaadinSpringBoot"

    private final PropertyState<String> starterVersion

    VaadinSpringBootExtension(Project project) {
        starterVersion = project.property(String)
        starterVersion.set(null)
    }

    /**
     * The Vaadin Spring Boot Starter version to use. By
     * default latest 2 version.
     */
    String getStarterVersion() {
        starterVersion.getOrNull()
    }

    /**
     * The Vaadin Spring Boot Starter version to use. By
     * default latest 2 version.
     */
    String setStarterVersion(String springBootStarterVersion) {
        this.starterVersion.set(springBootStarterVersion)
    }
}
