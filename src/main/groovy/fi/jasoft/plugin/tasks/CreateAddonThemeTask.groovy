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
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class CreateAddonThemeTask extends DefaultTask {

    public static final String NAME = 'vaadinCreateAddonTheme'

    private String themeName

    public CreateAddonThemeTask() {
        description = "Creates a new theme for addon project."
    }

    @TaskAction
    public void run() {

        // Build theme name from addon title
        def title = 'MyAddonTheme'
        if(project.vaadin.addon.title != null && project.vaadin.addon.title != ''){
              title = project.vaadin.addon.title as String;
              title = title.toLowerCase().replaceAll(/[_ ](\w)?/){ wholeMatch, firstLetter ->
                  firstLetter?.toUpperCase() ?: ""
              }.capitalize()
        }

        themeName = Util.readLine("\nTheme Name ($title): ")
        if (themeName == null || themeName == '') {
            themeName = title
        }

        createTheme(themeName)
    }

    public void createTheme(String themeName) {
        File resourceDir = project.sourceSets.main.resources.srcDirs.iterator().next()
        File themeDir = new File(resourceDir.canonicalPath + '/VAADIN/addons/' + themeName)
        themeDir.mkdirs()

        def substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = substitutions['themeName'].toLowerCase()

        TemplateUtil.writeTemplate('myaddon.scss', themeDir, themeName+'.scss', substitutions)
    }
}