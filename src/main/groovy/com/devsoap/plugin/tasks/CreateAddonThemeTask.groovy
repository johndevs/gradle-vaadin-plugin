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

import com.devsoap.plugin.creators.AddonThemeCreator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction

/**
 * Creates a theme for an addon
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateAddonThemeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateAddonTheme'

    @Input
    private final Property<String> addonTitle = project.objects.property(String)

    /**
     * The theme name
     */
    @Input
    @Optional
    @Option(option = 'name', description = 'Theme name')
    String themeName = 'MyAddonTheme'


    CreateAddonThemeTask() {
        description = "Creates a new theme for addon project."
    }

    /**
     * Creates the addon theme
     */
    @TaskAction
    void run() {

        // Build theme name from addon title
        if ( !themeName && addonTitle.present ) {
              themeName = addonTitle.get().toLowerCase().replaceAll(/[_ ](\w)?/) { wholeMatch, firstLetter ->
                  firstLetter?.toUpperCase() ?: ""
              }.capitalize()
        }

        new AddonThemeCreator(
                resourceDir:project.sourceSets.main.resources.srcDirs.first(),
                themeName:themeName,
        ).run()
    }

    /**
     * Get the title displayed in the directory
     */
    String getAddonTitle() {
        addonTitle.getOrNull()
    }

    /**
     * Set the title displayed in the directory
     */
    void setAddonTitle(String title) {
        addonTitle.set(title)
    }

    /**
     * Set the title displayed in the directory
     */
    void setAddonTitle(Provider<String> title) {
        addonTitle.set(title)
    }
}