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
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Component
 *
 * @author John Ahlroos
 */
class CreateComponentTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComponent'

    static final String DOT = '.'
    static final String SERVER_PACKAGE = 'server'
    static final String CLIENT_PACKAGE = 'client'

    @Option(option = 'name', description = 'Component name')
    def componentName = 'MyComponent'

    public CreateComponentTask() {
        description = "Creates a new Vaadin Component."
    }

    @TaskAction
    public void run() {
        makeComponentClasses()
    }

    @PackageScope
    def makeComponentClasses() {

        def widgetset = Util.getWidgetset(project)

        def widgetsetPackagePath
        def widgetsetPackage
        if(widgetset.contains(DOT)){
            def widgetsetPackageFQN = widgetset.substring(0, widgetset.lastIndexOf(DOT))
            widgetsetPackagePath = TemplateUtil.convertFQNToFilePath(widgetsetPackageFQN)
            def widgetsetName = widgetset.tokenize(DOT).last()
            widgetsetPackage = widgetset.replaceAll("$DOT$widgetsetName", StringUtils.EMPTY)
        } else {
            widgetsetPackagePath = ''
            widgetsetPackage = null
        }

        def srcDir = Util.getMainSourceSet(project, true).srcDirs.first()
        def widgetsetDir = new File(srcDir, widgetsetPackagePath)

        def componentDir = new File(new File(widgetsetDir, SERVER_PACKAGE), componentName.toLowerCase())
        componentDir.mkdirs()

        def widgetDir = new File(new File(widgetsetDir, CLIENT_PACKAGE), componentName.toLowerCase())
        widgetDir.mkdirs()

        def substitutions = [:]
        substitutions['componentServerPackage'] = "${widgetsetPackage? widgetsetPackage + DOT : StringUtils.EMPTY}" +
                "$SERVER_PACKAGE.${componentName.toLowerCase()}"
        substitutions['componentClientPackage'] = "${widgetsetPackage? widgetsetPackage + DOT : StringUtils.EMPTY}" +
                "$CLIENT_PACKAGE.${componentName.toLowerCase()}"
        substitutions['componentName'] = componentName
        substitutions['componentStylename'] = componentName.toLowerCase()

        TemplateUtil.writeTemplate("MyComponent.java", componentDir,
                "${componentName}.java", substitutions)
        TemplateUtil.writeTemplate("MyComponentWidget.java", widgetDir,
                "${componentName}Widget.java", substitutions)
        TemplateUtil.writeTemplate("MyComponentConnector.java", widgetDir,
                "${componentName}Connector.java", substitutions)
    }
}