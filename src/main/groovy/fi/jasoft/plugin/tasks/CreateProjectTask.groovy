/*
* Copyright 2014 John Ahlroos
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
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateProjectTask extends DefaultTask {

    public static final NAME = 'vaadinCreateProject'

    private String applicationName

    private String applicationPackage

    public CreateProjectTask() {
        description = "Creates a new Vaadin Project."
    }

    @TaskAction
    public void run() {

        if(System.console()){
            applicationName = Util.readLine('\nApplication Name (MyApplication): ')
        }

        if (applicationName == null || applicationName == '') {
            applicationName = 'MyApplication'
        }

        if (project.vaadin.widgetset != null && project.vaadin.widgetset.contains('.')) {
            String widgetsetName = project.vaadin.widgetset.tokenize('.').last()

            applicationPackage = project.vaadin.widgetset[0..(-widgetsetName.size() - 2)]
        } else {
            if(System.console()){
                applicationPackage = Util.readLine("\nApplication Package (com.example.${applicationName.toLowerCase()}): ")
            }
            if (applicationPackage == null || applicationPackage == '') {
                applicationPackage = 'com.example.' + applicationName.toLowerCase()
            }
        }

        createUIClass(project)
        createServletClass(project)

        if (Util.isAddonStylesSupported(project)) {
            project.tasks[CreateThemeTask.NAME].createTheme(applicationName)
        }

        project.tasks[UpdateWidgetsetTask.NAME].run()
    }

    private void createUIClass(Project project) {

        def substitutions = [:]

        substitutions['applicationName'] = applicationName
        substitutions['applicationPackage'] = applicationPackage

        //#######################################################################

        def imports = []

        if (Util.isPushSupportedAndEnabled(project)) {
            imports.add('com.vaadin.annotations.Push')
        }

        if (Util.isAddonStylesSupported(project)) {
            imports.add('com.vaadin.annotations.Theme')
        }

        substitutions['imports'] = imports

        //#######################################################################

        def annotations = []

        if (Util.isPushSupportedAndEnabled(project)) {
            annotations.add('Push')
        }

        if (Util.isAddonStylesSupported(project)) {
            if(Util.isGroovyProject(project)){
                annotations.add("Theme('${applicationName}')")
            } else {
                annotations.add("Theme(\"${applicationName}\")")
            }
        }

        substitutions['annotations'] = annotations

        //#######################################################################

        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File uidir = new File(javaDir.canonicalPath + '/' + applicationPackage.replaceAll(/\./, '/'))
        uidir.mkdirs()

        if(Util.isGroovyProject(project)){
            TemplateUtil.writeTemplate('MyUI.groovy', uidir, applicationName + "UI.groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate('MyUI.java', uidir, applicationName + "UI.java", substitutions)
        }
    }

    private void createServletClass(Project project) {

        def substitutions = [:]

        substitutions['applicationName'] = applicationName
        substitutions['applicationPackage'] = applicationPackage
        substitutions['asyncEnabled'] = Util.isPushSupportedAndEnabled(project)

        //#######################################################################

        def initParams = ['ui': "$applicationPackage.${applicationName}UI"]

        if (project.vaadin.widgetset != null) {
            initParams.put('widgetset', project.vaadin.widgetset.replaceAll("[^a-zA-Z0-9]+",""))
        }

        substitutions['initParams'] = initParams

        //#######################################################################

        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File uidir = new File(javaDir.canonicalPath + '/' + applicationPackage.replaceAll(/\./, '/'))
        uidir.mkdirs()

        if(Util.isGroovyProject(project)){
            TemplateUtil.writeTemplate("MyServlet.groovy", uidir, applicationName + "Servlet.groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate("MyServlet.java", uidir, applicationName + "Servlet.java", substitutions)
        }
    }
}

