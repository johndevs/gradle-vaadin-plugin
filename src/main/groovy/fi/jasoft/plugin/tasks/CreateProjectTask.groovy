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
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Project
 *
 * @author John Ahlroos
 */
class CreateProjectTask extends DefaultTask {

    public static final NAME = 'vaadinCreateProject'

    private static final String DOT = '.'
    private static final String APPLICATION_NAME_KEY = 'applicationName'
    private static final String APPLICATION_PACKAGE_KEY = 'applicationPackage'
    private static final String WIDGETSET_KEY = 'widgetset'

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
        def configuration = project.vaadinCompile as CompileWidgetsetConfiguration

        if(!applicationName){
            applicationName = project.name.capitalize()
        }
        if(!applicationPackage){
            int endSlashSize = 2
            if(widgetsetFQN?.contains(DOT)){
                String widgetsetName = widgetsetFQN.tokenize(DOT).last()
                applicationPackage = widgetsetFQN[0..(-widgetsetName.size() - endSlashSize)]
            } else if (configuration.widgetset?.contains(DOT)) {
                String widgetsetName = configuration.widgetset.tokenize(DOT).last()
                applicationPackage = configuration.widgetset[0..(-widgetsetName.size() - endSlashSize)]
            } else {
                applicationPackage = "com.example.${applicationName.toLowerCase()}"
            }
        }

        makeUIClass(project)

        makeServletClass(project)

        if (Util.isAddonStylesSupported(project)) {
            project.tasks[CreateThemeTask.NAME].makeTheme(applicationName)
        }

        UpdateWidgetsetTask.ensureWidgetPresent(project, widgetsetFQN)
    }

    @PackageScope
    def makeUIClass(Project project) {

        def substitutions = [:]

        substitutions[APPLICATION_NAME_KEY] = applicationName
        substitutions[APPLICATION_PACKAGE_KEY] = applicationPackage

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
    def makeServletClass(Project project) {
        def configuration = project.vaadinCompile as CompileWidgetsetConfiguration

        def substitutions = [:]

        substitutions[APPLICATION_NAME_KEY] = applicationName
        substitutions[APPLICATION_PACKAGE_KEY] = applicationPackage
        substitutions['asyncEnabled'] = Util.isPushSupportedAndEnabled(project)

        //#######################################################################

        def initParams = ['ui': "$applicationPackage.${applicationName}UI"]

        if (widgetsetFQN) {
            if(configuration.widgetsetCDN){
                initParams.put(WIDGETSET_KEY, "${widgetsetFQN.replaceAll("[^a-zA-Z0-9]+","")}")
            } else {
                initParams.put(WIDGETSET_KEY, "$widgetsetFQN")
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

