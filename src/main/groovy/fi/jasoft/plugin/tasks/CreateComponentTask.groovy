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

import fi.jasoft.plugin.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import fi.jasoft.plugin.TemplateUtil;

class CreateComponentTask extends DefaultTask {

    public static final String NAME = 'vaadinCreateComponent'

    public CreateComponentTask() {
        description = "Creates a new Vaadin Component."
    }

    @TaskAction
    public void run() {

        if (project.vaadin.widgetset == null) {
            project.logger.error("No widgetset found. Please define a widgetset using the vaadin.widgetset property.")
            return
        }

        String componentName = Util.readLine('\nComponent Name (MyComponent): ')
        if (componentName == null || componentName == '') {
            componentName = 'MyComponent'
        }

        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetFile = new File(javaDir.canonicalPath + '/' + project.vaadin.widgetset.replaceAll(/\./, '/') + ".gwt.xml")
        File widgetsetDir = new File(widgetsetFile.parent)
        File componentDir = new File(widgetsetDir.canonicalPath + '/server/' + componentName.toLowerCase())

        componentDir.mkdirs()

        String widgetsetName = project.vaadin.widgetset.tokenize('.').last()
        String widgetsetPackage = project.vaadin.widgetset.replaceAll('.' + widgetsetName, '')

        def substitutions = [:]
        substitutions['%PACKAGE%'] = widgetsetPackage + '.server.' + componentName.toLowerCase()
        substitutions['%COMPONENT_NAME%'] = componentName
        substitutions['%COMPONENT_STYLENAME%'] = componentName.toLowerCase()

        substitutions['%PACKAGE_CLIENT%'] = widgetsetPackage + '.client.' + componentName.toLowerCase()
        File clientui = new File(widgetsetDir.canonicalPath + '/client/' + componentName.toLowerCase())
        clientui.mkdirs()

        TemplateUtil.writeTemplate("MyComponent.java", componentDir, componentName + ".java", substitutions)
        TemplateUtil.writeTemplate("MyComponentWidget.java", clientui, componentName + "Widget.java", substitutions)
        TemplateUtil.writeTemplate("MyComponentConnector.java", clientui, componentName + "Connector.java", substitutions)


        if (project.vaadin.widgetset != null) {
            String compile = Util.readLine("\nCompile widgetset (Y/N)[Y]: ")
            if (compile == null || compile == '' || compile == 'Y') {
                project.tasks[CompileWidgetsetTask.NAME].run()
            }
        }
    }
}