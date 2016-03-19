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
import org.gradle.plugins.ide.eclipse.model.EclipseWtp

/**
 * Eclipse related utility methods
 *
 * @author John Ahlroos
 */
class EclipseUtil {

    public static final String ECLIPSE_PROPERTY = 'eclipse'

    /**
     * Configures the eclipse plugin
     *
     * @param project
     *      the project to configure. Must use the eclipse plugin.
     */
    static configureEclipsePlugin(Project project) {
        project.beforeEvaluate { Project p ->
            def plugins = p.plugins
            if (plugins.findPlugin(ECLIPSE_PROPERTY) && !plugins.findPlugin('eclipse-wtp')) {
                throw new BuildFailureException("You are using the eclipse plugin which does not support all " +
                        "features of the Vaadin plugin. Please use the eclipse-wtp plugin instead.")
            }
        }

        project.afterEvaluate { Project p ->
            if(p.hasProperty(ECLIPSE_PROPERTY)){
                def cp = p.eclipse.classpath
                def wtp = p.eclipse.wtp as EclipseWtp

                // Always download sources
                cp.downloadSources = true

                // Set Eclipse's class output dir
                if (p.vaadin.plugin.eclipseOutputDir == null) {
                    cp.defaultOutputDir = p.sourceSets.main.output.classesDir
                } else {
                    cp.defaultOutputDir = p.file(project.vaadin.plugin.eclipseOutputDir)
                }

                // Configure natures
                def natures = p.eclipse.project.natures
                natures.add(0, 'org.springsource.ide.eclipse.gradle.core.nature')

                // Configure facets
                def facet = wtp.facet
                facet.facets = []
                facet.facet(name: 'jst.web', version: '3.0')
                facet.facet(name: 'jst.java', version: p.sourceCompatibility)
                facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '7.0')
                facet.facet(name: 'java', version: p.sourceCompatibility)
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
            if(p.hasProperty(ECLIPSE_PROPERTY)){
                def cp = p.eclipse.classpath
                cp.plusConfigurations += [p.configurations[conf]]

                def wtp = p.eclipse.wtp as EclipseWtp
                wtp.component.plusConfigurations += [p.configurations[conf]]
            }
        }
    }
}
