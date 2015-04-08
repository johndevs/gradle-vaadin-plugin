/*
* Copyright 2015 John Ahlroos
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


public class CreateCompositeTask extends DefaultTask {

    public static final String NAME = 'vaadinCreateComposite'

    private String componentName

    private String componentPackage

    public CreateCompositeTask() {
        description = "Creates a new Vaadin Composite."
    }

    @TaskAction
    public void run() {

        componentName = Util.readLine('\nComposite Name (MyComposite): ')
        if (componentName == null || componentName == '') {
            componentName = 'MyComposite'
        }

        if (project.vaadin.widgetset) {
            String widgetsetClass = project.vaadin.widgetset
            String widgetsetPackage = widgetsetClass.substring(0, widgetsetClass.lastIndexOf("."))
            componentPackage = widgetsetPackage + '.' + componentName.toLowerCase();

        } else {
            componentPackage = Util.readLine("\nComposite Package (com.example.${componentName.toLowerCase()}): ")
            if (componentPackage == null || componentPackage == '') {
                componentPackage = "com.example.${componentName.toLowerCase()}"
            }
        }

        createCompositeClass()
    }

    private void createCompositeClass() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File componentDir = new File(javaDir.canonicalPath + '/' + componentPackage.replaceAll(/\./, '/'))
        componentDir.mkdirs()

        def substitutions = [:]
        substitutions['componentPackage'] = componentPackage
        substitutions['componentName'] = componentName

        if(Util.isGroovyProject(project)){
            TemplateUtil.writeTemplate("MyComposite.groovy", componentDir, componentName + ".groovy", substitutions)
        } else {
            TemplateUtil.writeTemplate("MyComposite.java", componentDir, componentName + ".java", substitutions)
        }

    }
}