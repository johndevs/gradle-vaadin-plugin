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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.creators.AddonThemeCreator
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a theme for an addon
 *
 * @author John Ahlroos
 */
class CreateAddonThemeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateAddonTheme'

    @Option(option = 'name', description = 'Theme name')
    def themeName = 'MyAddonTheme'

    public CreateAddonThemeTask() {
        description = "Creates a new theme for addon project."
    }

    @TaskAction
    def run() {

        // Build theme name from addon title
        if(!themeName && project.vaadin.addon.title){
              def title = project.vaadin.addon.title as String;
              themeName = title.toLowerCase().replaceAll(/[_ ](\w)?/){ wholeMatch, firstLetter ->
                  firstLetter?.toUpperCase() ?: ""
              }.capitalize()
        }

        new AddonThemeCreator(
                resourceDir: project.sourceSets.main.resources.srcDirs.first(),
                themeName: themeName,
        ).run()
    }
}