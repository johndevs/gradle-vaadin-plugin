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

import org.apache.maven.BuildFailureException
import org.gradle.api.Project
import org.gradle.api.plugins.PluginContainer
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.gradle.plugins.ide.eclipse.model.EclipseWtp
import org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet

/**
 * Eclipse related utility methods
 *
 * @author John Ahlroos
 */
class EclipseUtil {

    public static final String ECLIPSE_PROPERTY = 'eclipse'
    public static final String ECLIPSE_WTP_PLUGIN = 'eclipse-wtp'

    /**
     * Configures the eclipse plugin
     *
     * @param project
     *      the project to configure. Must use the eclipse plugin.
     */
    static configureEclipsePlugin(Project project) {
        project.afterEvaluate { Project p ->
            if(p.hasProperty(ECLIPSE_PROPERTY)){
                EclipseModel eclipse = p.eclipse as EclipseModel
                eclipse.project.comment = 'Project created with the Gradle Vaadin Plugin'

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
                //natures.add(2, 'com.vaadin.integration.eclipse.widgetsetNature')

                // Configure build commands
                eclipse.project.buildCommand('org.eclipse.buildship.core.gradleprojectbuilder')
                //eclipse.project.buildCommand('com.vaadin.integration.eclipse.addonStylesBuilder')
                //eclipse.project.buildCommand('com.vaadin.integration.eclipse.widgetsetBuilder')

                // Configure facets
                PluginContainer plugins = p.plugins
                if (plugins.findPlugin(ECLIPSE_WTP_PLUGIN)) {
                    EclipseWtp wtp = eclipse.wtp
                    EclipseWtpFacet facet = wtp.facet
                    facet.facets = []
                    facet.facet(name: 'jst.web', version: '3.0')
                    facet.facet(name: 'jst.java', version: p.sourceCompatibility)
                    //facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '7.0')
                    facet.facet(name: 'java', version: p.sourceCompatibility)
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
}
