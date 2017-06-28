/*
 * Copyright 2017 John Ahlroos
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
import com.devsoap.plugin.extensions.AddonExtension
import com.devsoap.plugin.tasks.CreateDirectoryZipTask
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle

/**
 * Actions applied when the Vaadin plugin is added to the build
 *
 * @author John Ahlroos
 * @since 1.2
 */
class VaadinPluginAction extends PluginAction {

    @Override
    String getPluginId() {
        GradleVaadinPlugin.pluginId
    }

    @Override
    protected void beforeTaskExecuted(Task task) {
        super.beforeTaskExecuted(task)
        switch (task.name) {
            case CreateDirectoryZipTask.NAME:
                configureAddonZipMetadata(task)
                break
        }
    }

    @Override
    protected void executeAfterEvaluate(Project project) {
        super.executeAfterEvaluate(project)
        VersionPrinter.instance.printIfNotPrintedBefore(project)
    }

    private static configureAddonZipMetadata(Task task) {
        Project project = task.project
        AddonExtension addonExtension = project.extensions.getByType(AddonExtension)
        Map attributes = [
                'Vaadin-Package-Version':1,
                'Vaadin-License-Title':addonExtension.license,
                'Implementation-Title':addonExtension.title,
                'Implementation-Version':project.version != null ? project.version : '',
                'Implementation-Vendor':addonExtension.author,
                'Vaadin-Addon': "libs/${project.jar.archiveName}"
        ] as HashMap<String, String>

        // Create metadata file
        File buildDir = project.file('build/tmp/zip')
        buildDir.mkdirs()

        File meta = project.file(buildDir.absolutePath + '/META-INF')
        meta.mkdirs()

        File manifestFile = project.file(meta.absolutePath + '/MANIFEST.MF')
        manifestFile.createNewFile()
        manifestFile << attributes.collect { key, value -> "$key: $value" }.join("\n")
    }

    @Singleton(lazy = false, strict = true)
    private static class VersionPrinter {
        private Gradle gradle
        void printIfNotPrintedBefore(Project project) {
            if(project.gradle == gradle){
                return
            }
            gradle = project.gradle
            project.logger.quiet "Using Gradle Vaadin Plugin $GradleVaadinPlugin.version"
        }
    }
}
