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

import org.gradle.api.tasks.bundling.Jar

/**
 * Build the Javadoc jar
 *
 * @author John Ahlroos
 */
class BuildJavadocJarTask extends Jar {

    static final String NAME = 'vaadinJavadocJar'

    BuildJavadocJarTask() {
        description = 'Creates a javadoc jar for the project'
        classifier = 'javadoc'
        dependsOn project.tasks.javadoc
        from project.tasks.javadoc.destinationDir
    }
}
