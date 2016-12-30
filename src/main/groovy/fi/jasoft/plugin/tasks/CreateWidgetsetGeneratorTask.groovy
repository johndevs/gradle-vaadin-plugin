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
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Creates the widgetset generated class
 *
 * @author John Ahlroos
 */
class CreateWidgetsetGeneratorTask extends DefaultTask {

    public static final String NAME = 'vaadinCreateWidgetsetGenerator'

    private static final String DOT = '.'

    private static final String JAVA_FILE_POSTFIX = '.java'

    public CreateWidgetsetGeneratorTask() {
        description = "Creates a new widgetset generator for optimizing the widgetset"
    }

    @TaskAction
    def run() {
        if ( !project.vaadinCompile.widgetset ) {
            throw new GradleException("No widgetset found. Please define a widgetset " +
                    "using the vaadinCompile.widgetset property.")
        }
        makeWidgetsetGeneratorClass()
    }

    @PackageScope
    def makeWidgetsetGeneratorClass() {
        def javaDir = Util.getMainSourceSet(project, true).srcDirs.first()
        def widgetset = project.vaadinCompile.widgetset as String
        def widgetsetGenerator = project.vaadinCompile.widgetsetGenerator as String

        String name, pkg, filename
        if ( !widgetsetGenerator ) {
            name = widgetset.tokenize(DOT).last()
            pkg = widgetset.replaceAll(DOT + name, '')
            filename = name + "Generator.java"

        } else {
            name = widgetsetGenerator.tokenize(DOT).last()
            pkg = widgetsetGenerator.replaceAll(DOT + name, '')
            filename = name + JAVA_FILE_POSTFIX
        }

        List<String> sourcePaths = project.vaadinCompile.sourcePaths as List
        sourcePaths.each { String path ->
            println path
            println pkg
            if(pkg.contains(".${path}.") || pkg.endsWith(".${path}")){
                throw new GradleException("Widgetset generator cannot be placed inside the client package.")
            }
        }

        def dir = new File(javaDir, TemplateUtil.convertFQNToFilePath(pkg))
        dir.mkdirs()

        def substitutions = [:]
        substitutions['packageName'] = pkg
        substitutions['className'] = filename.replaceAll(JAVA_FILE_POSTFIX, '')

        TemplateUtil.writeTemplate('MyConnectorBundleLoaderFactory.java', dir, filename, substitutions)
    }
}
