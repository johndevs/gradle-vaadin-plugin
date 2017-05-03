/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.Util
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.configurations.DefaultConfiguration
import org.gradle.api.internal.file.CompositeFileCollection
import org.gradle.api.tasks.bundling.Jar

/**
 * Builds a classpath jar used by external java processes.
 *
 * @author John Ahlroos
 */
class BuildClassPathJar extends Jar {

    public static final String NAME = 'vaadinClassPathJar'

    BuildClassPathJar() {
        description = 'Creates a Jar with the project classpath'
        classifier = 'classpath'
        dependsOn 'classes'

        onlyIf {
            project.vaadin.useClassPathJar
        }

        project.afterEvaluate {

            DefaultConfiguration serverConf = project.configurations
                    .getByName(GradleVaadinPlugin.CONFIGURATION_RUN_SERVER)
            FileCollection classPath = Util.getCompileClassPath(project)
            CompositeFileCollection files = (serverConf + classPath)

            inputs.files(files)

            manifest.attributes('Class-Path':files.collect { File file ->
                file.toURI().toString()
            }.join(' '))
        }
    }
}
