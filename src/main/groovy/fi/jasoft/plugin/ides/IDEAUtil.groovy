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
 * Created by john on 5.1.2016.
 */
class IDEAUtil {

    static void configureIDEAModule(Project project) {
        project.afterEvaluate { Project p ->
            if(p.hasProperty('idea')){
                def module = p.idea.module

                // Module name is project name
                module.name = p.name

                module.inheritOutputDirs = false
                module.outputDir = project.sourceSets.main.output.classesDir
                module.testOutputDir = project.sourceSets.test.output.classesDir

                // Download sources and javadoc
                module.downloadJavadoc = true
                module.downloadSources = true
            }
        }
    }

    static void addConfigurationToProject(Project project, String conf, boolean test=false){
        project.afterEvaluate { Project p ->
            if(p.hasProperty('idea')){
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
