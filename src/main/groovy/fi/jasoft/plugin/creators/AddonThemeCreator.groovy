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
package fi.jasoft.plugin.creators

import fi.jasoft.plugin.TemplateUtil
import groovy.transform.Canonical

/**
 * Created by john on 9/15/16.
 */
@Canonical
class AddonThemeCreator implements Runnable {

    private File resourceDir

    private String themeName

    private String templateDir

    @Override
    void run() {
        File vaadinDir = new File(resourceDir, 'VAADIN')
        File addonsDir = new File(vaadinDir, 'addons')
        File themeDir = new File(addonsDir, themeName)
        themeDir.mkdirs()

        Map<String, String> substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = themeName.toLowerCase()

        if ( templateDir ) {
            TemplateUtil.writeTemplate("${templateDir}/myaddon.scss", themeDir, "${themeName}.scss", substitutions)
        } else {
            TemplateUtil.writeTemplate('myaddon.scss', themeDir, "${themeName}.scss", substitutions)
        }
    }
}
