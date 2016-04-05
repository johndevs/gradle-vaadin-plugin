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

import org.gradle.api.Project

/**
 * Intellij related utility methods
 *
 * @author John Ahlroos
 */
class IDEAUtil {

    private static final String IDEA_PROPERTY = 'idea'

    /**
     * Configures the intellij module.
     *
     * @param project
     *      the project to configure
     */
    static void configureIDEAModule(Project project) {
        project.afterEvaluate { Project p ->
            if(p.hasProperty(IDEA_PROPERTY)){
                def module = p.idea.module

                // Module name is project name
                module.name = p.name

                module.inheritOutputDirs = false

                if (project.vaadinRun.classesDir == null) {
                    module.outputDir = project.sourceSets.main.output.classesDir
                    module.testOutputDir = project.sourceSets.test.output.classesDir
                } else {
                    module.outputDir = project.file(project.vaadinRun.classesDir)
                    module.testOutputDir = project.file(project.vaadinRun.classesDir)
                }

                // Download sources and javadoc
                module.downloadJavadoc = true
                module.downloadSources = true
            }
        }
    }

    /**
     * Adds a dependency configuration to the module scope
     *
     * @param project
     *      the project to add the configuration to
     * @param conf
     *      the configuration name (must exist in project.configurations)
     * @param test
     *      is the configuration a test dependency
     */
    static void addConfigurationToProject(Project project, String conf, boolean test=false){
        project.afterEvaluate { Project p ->
            if(p.hasProperty(IDEA_PROPERTY)){
                def scopes = p.idea.module.scopes
                if(test){
                    scopes.TEST.plus += [p.configurations[conf]]
                } else {
                    scopes.COMPILE.plus += [p.configurations[conf]]
                }
            }
        }
    }

}
