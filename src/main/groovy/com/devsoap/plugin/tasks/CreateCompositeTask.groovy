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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util

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

    CreateCompositeTask() {
        description = "Creates a new Vaadin Composite."
    }

    @TaskAction
    void run() {
        CompileWidgetsetTask compileWidgetsetTask = project.tasks.getByName(CompileWidgetsetTask.NAME)
        if ( !componentPackage && compileWidgetsetTask.widgetset ) {
            String widgetsetClass = compileWidgetsetTask.widgetset
            String widgetsetPackage = widgetsetClass.substring(0, widgetsetClass.lastIndexOf(DOT))
            componentPackage = widgetsetPackage + DOT + componentName.toLowerCase()
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

        switch (Util.getProjectType(project)) {
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate("MyComposite.groovy", componentDir,
                        componentName + ".groovy", substitutions)
                break
            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate("MyComposite.kt", componentDir,
                        componentName + ".kt", substitutions)
                break
            case ProjectType.JAVA:
                TemplateUtil.writeTemplate("MyComposite.java", componentDir,
                        componentName + ".java", substitutions)
        }
    }
}