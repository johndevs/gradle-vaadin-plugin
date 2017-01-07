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
package fi.jasoft.plugin

import fi.jasoft.plugin.configuration.VaadinPluginGroovyExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.GroovyPlugin

/**
 * Vaadin Plugin for Groovy projects
 */
class GradleVaadinGroovyPlugin extends GradleVaadinPlugin {

    static final String NAME = 'vaadinGroovy'

    @Override
    static String getPluginId() {
        'fi.jasoft.plugin.vaadin.groovy'
    }

    @Override
    void apply(Project project) {
        super.apply(project)

        // Plugins
        project.plugins.apply(GroovyPlugin)

        // Extensions
        Util.findOrCreateExtension(project, NAME, VaadinPluginGroovyExtension)

        // Dependencies
        if ( project.vaadin.manageDependencies ) {
            ConfigurationContainer configurations = project.configurations
            Configuration compileConfiguration = configurations.findByName('compile')
            DependencyHandler projectDependencies = project.dependencies
            Dependency groovy = projectDependencies
                    .create("org.codehaus.groovy:groovy-all:${project.vaadinGroovy.groovyVersion}")
            compileConfiguration.dependencies.add(groovy)
        }
    }
}
