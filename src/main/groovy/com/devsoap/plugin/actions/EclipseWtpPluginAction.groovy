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
package com.devsoap.plugin.actions

import com.devsoap.plugin.GradleVaadinPlugin
import org.gradle.api.Project
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.EclipseWtp
import org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet
import org.gradle.plugins.ide.eclipse.model.Facet

/**
 * Actions applied when the eclipse-wtp plugin is applied
 *
 * @author John Ahlroos
 * @since 1.2
 */
class EclipseWtpPluginAction extends PluginAction {

    private static final String JAVA_1_8 = '1.8'

    @Override
    String getPluginId() {
        'eclipse-wtp'
    }

    @Override
    protected void execute(Project project) {
        super.execute(project)

        EclipseModel eclipse = project.extensions.getByType(EclipseModel)
        EclipseWtp wtp = eclipse.wtp

        // Add facets
        setOrAddFacet(project, wtp, 'jst.web', '3.0')
        setOrAddFacet(project, wtp, 'jst.java', JAVA_1_8)
        setOrAddFacet(project, wtp, 'java', JAVA_1_8)

        // Add configurations to wtp classpath
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SERVER)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_PUSH)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_TESTBENCH)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_THEME)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT)
        addConfigurationToProject(project, GradleVaadinPlugin.CONFIGURATION_CLIENT_COMPILE)
    }

    /**
     * Adds a facet or updates an existing facet
     *
     * @param facets
     *      the facets
     * @param name
     *      the name of the facet
     * @param version
     *      the version of the facet
     */
    private static void setOrAddFacet(Project project, EclipseWtp wtp, String name, String version) {
        EclipseWtpFacet facetContainer = wtp.facet
        List<Facet> facets = facetContainer.facets
        Facet facet = facets.find { it.name == name && it.type == Facet.FacetType.installed }
        if ( facet ) {
            facet.version = version
            project.logger.info("Updated facet $facet.name to version $facet.version")
        } else {
            facets.add(new Facet(name, version))
        }
        facetContainer.facets = facets
    }


    /**
     * Adds a dependency configuration to the eclipse project classpath
     *
     * @param project
     *      the project to add the configuration to
     * @param conf
     *      the configuration name (must exist in project.configurations)
     * @param deploy
     *      also add to wtp deployment assembly
     */
    private static void addConfigurationToProject(Project project, String conf) {
        EclipseModel eclipse = project.extensions.getByType(EclipseModel)
        EclipseWtp wtp = eclipse.wtp as EclipseWtp
        wtp.component.plusConfigurations += [project.configurations[conf]]
    }
}
