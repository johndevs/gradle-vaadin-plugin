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
import fi.jasoft.plugin.Util
import fi.jasoft.plugin.creators.ThemeCreator
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Creates a new theme
 *
 * @author John Ahlroos
 */
class CreateThemeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateTheme'

    @Option(option = 'name', description = 'Theme name')
    def String themeName

    public CreateThemeTask() {
        description = "Creates a new Vaadin Theme"
        dependsOn(BuildClassPathJar.NAME)
        finalizedBy(UpdateAddonStylesTask.NAME)
    }

    @TaskAction
    def run() {

        if ( !themeName ) {
            themeName = project.getName()
        }

        new ThemeCreator(themeName:themeName,
                themesDirectory:Util.getThemesDirectory(project),
                vaadinVersion:Util.getVaadinVersion(project)
        ).run()
    }
}