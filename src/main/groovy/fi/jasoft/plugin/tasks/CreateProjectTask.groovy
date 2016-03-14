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
import org.gradle.api.Project
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

import java.rmi.server.UID

class CreateProjectTask extends DefaultTask {

    public static final NAME = 'vaadinCreateProject'

    @Option(option = 'name', description = 'Application name')
    def String applicationName

    @Option(option = 'package', description = 'Application UI package')
    def String applicationPackage

    @Option(option = 'widgetset', description = 'Widgetset name')
    def String widgetsetFQN

    public CreateProjectTask() {
        description = "Creates a new Vaadin Project."
    }

    @TaskAction
    def run() {
        if(!applicationName){
            applicationName = project.getName()
        }
        if(!applicationPackage){
            if(widgetsetFQN?.contains('.')){
                String widgetsetName = widgetsetFQN.tokenize('.').last()
                applicationPackage = widgetsetFQN[0..(-widgetsetName.size() - 2)]
            } else if (project.vaadin.widgetset?.contains('.')) {
                String widgetsetName = project.vaadin.widgetset.tokenize('.').last()
                applicationPackage = project.vaadin.widgetset[0..(-widgetsetName.size() - 2)]
            } else {
                applicationPackage = "com.example.${applicationName.toLowerCase()}"
            }
        }

        createUIClass(project)

        createServletClass(project)

        if (Util.isAddonStylesSupported(project)) {
            project.tasks[CreateThemeTask.NAME].createTheme(applicationName)
        }

        if(widgetsetFQN){
            UpdateWidgetsetTask.ensureWidgetPresent(project, widgetsetFQN)
        } else {
            project.tasks[UpdateWidgetsetTask.NAME].run()
        }
    }

    @PackageScope
    def createUIClass(Project project) {

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

        if(Util.isGroovyProject(project)){
            TemplateUtil.writeTemplate('MyUI.groovy', UIDir, applicationName + "UI.groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate('MyUI.java', UIDir, applicationName + "UI.java", substitutions)
        }
    }

    @PackageScope
    def createServletClass(Project project) {

        def substitutions = [:]

        substitutions['applicationName'] = applicationName
        substitutions['applicationPackage'] = applicationPackage
        substitutions['asyncEnabled'] = Util.isPushSupportedAndEnabled(project)

        //#######################################################################

        def initParams = ['ui': "$applicationPackage.${applicationName}UI"]

        if (project.vaadin.widgetset != null) {
            if(project.vaadinCompile.configuration.widgetsetCDN){
                initParams.put('widgetset', project.vaadin.widgetset.replaceAll("[^a-zA-Z0-9]+",""))
            } else {
                initParams.put('widgetset', project.vaadin.widgetset)
            }
        }

        substitutions['initParams'] = initParams

        //#######################################################################

        if(Util.isGroovyProject(project)){
            TemplateUtil.writeTemplate("MyServlet.groovy", UIDir, applicationName + "Servlet.groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate("MyServlet.java", UIDir, applicationName + "Servlet.java", substitutions)
        }
    }

    @PackageScope
    def File getUIDir(){
        def javaDir = Util.getMainSourceSet(project).srcDirs.first()
        def uidir = new File(javaDir, TemplateUtil.convertFQNToFilePath(applicationPackage))
        uidir.mkdirs()
        uidir
    }
}

