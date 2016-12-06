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
package fi.jasoft.plugin.ides

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.EclipseWtp
import org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet
import org.gradle.plugins.ide.eclipse.model.Facet

/**
 * Eclipse related utility methods
 *
 * @author John Ahlroos
 */
class EclipseUtil {

    static final String ECLIPSE_PROPERTY = 'eclipse'
    static final String ECLIPSE_WTP_PLUGIN = 'eclipse-wtp'

    private static final String JAVA_1_8 = '1.8'

    /**
     * Configures the eclipse plugin
     *
     * @param project
     *      the project to configure. Must use the eclipse plugin.
     */
    static configureEclipsePlugin(Project project) {
        project.afterEvaluate { Project p ->
            if(p.hasProperty(ECLIPSE_PROPERTY)) {
                EclipseModel eclipse = p.eclipse as EclipseModel
                eclipse.project.comment = 'Vaadin Project'

                // Always download sources
                def cp = eclipse.classpath
                cp.downloadSources = true

                // Set Eclipse's class output dir
                if (p.vaadinRun.classesDir == null) {
                    cp.defaultOutputDir = p.sourceSets.main.output.classesDir
                } else {
                    cp.defaultOutputDir = p.file(p.vaadinRun.classesDir)
                }

                // Configure natures
                def natures = eclipse.project.natures
                natures.add(0, 'org.springsource.ide.eclipse.gradle.core.nature')
                natures.add(1, 'org.eclipse.buildship.core.gradleprojectnature')

                // Configure build commands
                eclipse.project.buildCommand('org.eclipse.buildship.core.gradleprojectbuilder')

                // Configure facets
                PluginContainer plugins = p.plugins
                if (plugins.findPlugin(ECLIPSE_WTP_PLUGIN)) {
                    EclipseWtp wtp = eclipse.wtp
                    setOrAddFacet(p, wtp, 'jst.web', '3.0')
                    setOrAddFacet(p, wtp, 'jst.java', JAVA_1_8)
                    setOrAddFacet(p, wtp, 'java', JAVA_1_8)
                }
            }
        }
    }

    /**
     * Adds a dependency configuration to the eclipse project classpath
     *
     * @param project
     *      the project to add the configuration to
     * @param conf
     *      the configuration name (must exist in project.configurations)
     */
    static void addConfigurationToProject(Project project, String conf){
        project.afterEvaluate { Project p ->
            PluginContainer plugins = p.plugins
            if(p.hasProperty(ECLIPSE_PROPERTY)){
                EclipseModel eclipse = p.eclipse as EclipseModel
                EclipseClasspath cp = eclipse.classpath
                cp.plusConfigurations += [p.configurations[conf]]
            }

            if(plugins.findPlugin(ECLIPSE_WTP_PLUGIN)) {
                EclipseModel eclipse = p.eclipse as EclipseModel
                EclipseWtp wtp = eclipse.wtp as EclipseWtp
                wtp.component.plusConfigurations += [p.configurations[conf]]
            }
        }
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
    static void setOrAddFacet(Project project, EclipseWtp wtp, String name, String version) {
        EclipseWtpFacet facetContainer = wtp.facet
        List<Facet> facets = facetContainer.facets
        Facet facet = facets.find { it.name == name && it.type == Facet.FacetType.installed }
        if(facet) {
            facet.version = version
            project.logger.info("Updated facet $facet.name to version $facet.version")
        } else {
            facets.add(new Facet(name, version))
        }
        facetContainer.facets = facets
    }
}
