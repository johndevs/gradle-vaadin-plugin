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
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Composite
 *
 * @author John Ahlroos
 */
class CreateCompositeTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComposite'

    static final String DOT = '.'

    @Option(option = 'name', description = 'Component name')
    def componentName = 'MyComposite'

    @Option(option = 'package', description = 'Package name')
    def componentPackage = "com.example.${componentName.toLowerCase()}"

    public CreateCompositeTask() {
        description = "Creates a new Vaadin Composite."
    }

    @TaskAction
    public void run() {
        def configuration = project.vaadinCompile as CompileWidgetsetConfiguration
        if(!componentPackage && configuration.widgetset){
            String widgetsetClass = configuration.widgetset
            String widgetsetPackage = widgetsetClass.substring(0, widgetsetClass.lastIndexOf(DOT))
            componentPackage = widgetsetPackage + DOT + componentName.toLowerCase();
        }

        makeCompositeClass()
    }

    @PackageScope
    def makeCompositeClass() {
        def javaDir = Util.getMainSourceSet(project).srcDirs.first()

        def componentDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(componentPackage))
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