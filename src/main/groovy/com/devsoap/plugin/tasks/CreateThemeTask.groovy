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

import com.devsoap.plugin.Util
import com.devsoap.plugin.creators.ThemeCreator
import com.devsoap.plugin.extensions.VaadinPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new theme
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateThemeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateTheme'

    /**
     * The theme name
     */
    @Input
    @Optional
    @Option(option = 'name', description = 'Theme name')
    String themeName

    CreateThemeTask() {
        description = "Creates a new Vaadin Theme"
        dependsOn(BuildClassPathJar.NAME)
        finalizedBy(UpdateAddonStylesTask.NAME)
    }

    /**
     * Creates a new theme
     */
    @TaskAction
    void run() {

        if ( !themeName ) {
            themeName = project.name
        }

        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)

        new ThemeCreator(themeName:themeName,
                themesDirectory:Util.getThemesDirectory(project),
                vaadinVersion:vaadin.version
        ).run()
    }
}