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
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.plugins.JavaPluginConvention;
import fi.jasoft.plugin.TemplateUtil;

class CreateServlet3ProjectTask extends DefaultTask {

    public CreateServlet3ProjectTask() {
        description = "Creates a new Vaadin Project bases on Java Servlet 3.0"
    }

    @TaskAction
    public void run() {

        String applicationName = Util.readLine('\nApplication Name (MyApplication): ')
        if (applicationName == '') {
            applicationName = 'MyApplication'
        }

        String applicationPackage;
        if (project.vaadin.widgetset != null) {
            String widgetsetName = project.vaadin.widgetset.tokenize('.').last()
            applicationPackage = project.vaadin.widgetset[0..(-widgetsetName.size() - 2)]
        } else {
            applicationPackage = Util.readLine("\nApplication Package (com.example.${applicationName.toLowerCase()}): ")
            if (applicationPackage == '') {
                applicationPackage = 'com.example.' + applicationName.toLowerCase()
            }
        }

        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File uidir = new File(javaDir.canonicalPath + '/' + applicationPackage.replaceAll(/\./, '/'))

        uidir.mkdirs()

        def substitutions = [:]
        substitutions['%PACKAGE%'] = applicationPackage
        substitutions['%APPLICATION_NAME%'] = applicationName
        substitutions['%PUSH%'] = Util.isPushSupportedAndEnabled(project) ? '@Push' : ''
        substitutions['%PUSH_IMPORT%'] = Util.isPushSupportedAndEnabled(project) ? "\nimport com.vaadin.annotations.Push;" : ''
        substitutions['%THEME%'] = Util.isAddonStylesSupported(project) ? "@Theme(\"${applicationName}\")" : ''
        substitutions['%THEME_IMPORT%'] = Util.isAddonStylesSupported(project) ? "\nimport com.vaadin.annotations.Theme;" : ''
        substitutions['%ASYNC_SUPPORTED%'] = Util.isPushSupportedAndEnabled(project) ? "\n    asyncSupported=true," : ''

        if (project.vaadin.widgetset != null) {
            substitutions['%WIDGETSETPARAM%'] = ",\n\t\t@WebInitParam(name=\"widgetset\", value=\"${project.vaadin.widgetset}\")"
        } else {
            substitutions['%WIDGETSETPARAM%'] = ''
        }

        TemplateUtil.writeTemplate('MyUI.java', uidir, applicationName + "UI.java", substitutions)

        TemplateUtil.writeTemplate("MyServlet.java", uidir, applicationName + "Servlet.java", substitutions)

        if (Util.isAddonStylesSupported(project)) {
            project.tasks.createVaadinTheme.createTheme(applicationName)
        }
    }
}

