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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.Util
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar

/**
 * Builds a classpath jar used by external java processes.
 *
 * @author John Ahlroos
 * @since 1.1
 */
class BuildClassPathJar extends Jar {

    public static final String NAME = 'vaadinClassPathJar'

    private final Property<Boolean> useClassPathJar = project.objects.property(Boolean)

    BuildClassPathJar() {
        description = 'Creates a Jar with the project classpath'
        classifier = 'classpath'
        dependsOn 'classes'
        onlyIf { getUseClassPathJar() }

        inputs.files project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER]
        inputs.files project.configurations[GradleVaadinPlugin.CONFIGURATION_THEME]
        inputs.files Util.getCompileClassPath(project)
    }

    @Override
    protected void copy() {
        FileCollection files = (Util.getWarClasspath(project) +
                project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER] +
                project.configurations[GradleVaadinPlugin.CONFIGURATION_THEME]
                )
                .filter { it.file && it.canonicalFile.name.endsWith('.jar')}
        manifest {
            it.attributes('Class-Path':files.collect { File file -> file.toURI().toString() }.join(' '))
        }
        super.copy()
    }

    /**
     * Is classpath jar enabled
     */
    Boolean getUseClassPathJar() {
        useClassPathJar.get()
    }

    /**
     * Is classpath jar enabled
     */
    void setUseClassPathJar(Boolean enabled) {
        useClassPathJar.set(enabled)
    }

    /**
     * Is classpath jar enabled
     */
    void setUseClassPathJar(Provider<Boolean> enabled) {
        useClassPathJar.set(enabled)
    }
}
