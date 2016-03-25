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
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction
import org.gradle.util.VersionNumber

/**
 * Creates a new theme
 *
 * @author John Ahlroos
 */
class CreateThemeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateTheme'
    static final String STYLES_SCSS_FILE = 'styles.scss'

    @Option(option = 'name', description = 'Theme name')
    def String themeName

    public CreateThemeTask() {
        description = "Creates a new Vaadin Theme"
    }

    @TaskAction
    def run() {
        if(!themeName){
            themeName = project.getName()
        }
        makeTheme(themeName)
    }

    @PackageScope
    def makeTheme(String themeName) {
        def themeDir = new File(Util.getThemesDirectory(project), themeName)
        themeDir.mkdirs()

        def substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = themeName.toLowerCase()

        String themeScssFile = themeName.toLowerCase() + '.scss'
        substitutions['themeImport'] = themeScssFile

        VersionNumber version = VersionNumber.parse(Util.getVaadinVersion(project))
        substitutions['basetheme'] = version.minor < 3 ? 'reindeer' : 'valo'

        TemplateUtil.writeTemplate(STYLES_SCSS_FILE, themeDir, STYLES_SCSS_FILE, substitutions)
        TemplateUtil.writeTemplate('MyTheme.scss', themeDir, themeScssFile, substitutions)

        project.tasks[UpdateAddonStylesTask.NAME].run()
    }
}