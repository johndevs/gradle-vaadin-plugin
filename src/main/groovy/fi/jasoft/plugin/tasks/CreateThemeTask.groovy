/*
* Copyright 2013 John Ahlroos
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

import fi.jasoft.plugin.Util;
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import fi.jasoft.plugin.TemplateUtil
import org.gradle.api.plugins.WarPluginConvention

class CreateThemeTask extends DefaultTask {

	public CreateThemeTask() {
		description = "Creates a new Vaadin Theme"
	}

	 @TaskAction
    public void run() {

    	String themeName = Util.readLine('\nTheme Name (MyTheme): ')
    	if(themeName == ''){
    		themeName = 'MyTheme'
    	}

    	File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
    	File themeDir = new File(webAppDir.canonicalPath + '/VAADIN/themes/'+themeName)
    	themeDir.mkdirs()

        def substitutions = [:]
        substitutions['%THEME%'] = themeName.toLowerCase()

    	if(project.vaadin.version.startsWith("6")){
    		TemplateUtil.writeTemplate('MyTheme.css', themeDir, 'styles.css', substitutions)
    		println "Remember to call setTheme(\"${themeName}\") in your Application to use your new theme."

    	} else {
    		TemplateUtil.writeTemplate('MyTheme.scss', themeDir, 'styles.scss', substitutions)
    		println "Remember to annotate your UI with the @Theme(\"${themeName}\") to use your new theme."
    	}
    }
}