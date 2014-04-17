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

class CreateWidgetsetGeneratorTask extends DefaultTask {

    public static final String NAME = 'vaadinCreateWidgetsetGenerator'

    public CreateWidgetsetGeneratorTask() {
        description = "Creates a new widgetset generator for optimizing the widgetset"
    }

    @TaskAction
    public void run() {

        if (project.vaadin.widgetset == null) {
            project.logger.error("No widgetset found. Please define a widgetset using the vaadin.widgetset property.")
            return
        }

        createWidgetsetGeneratorClass()
    }

    private void createWidgetsetGeneratorClass() {

        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()

        String name, pkg, filename
        if (project.vaadin.widgetsetGenerator == null) {
            name = project.vaadin.widgetset.tokenize('.').last()
            pkg = project.vaadin.widgetset.replaceAll('.' + name, '') + '.client.ui'
            filename = name + "Generator.java"

        } else {
            name = project.vaadin.widgetsetGenerator.tokenize('.').last()
            pkg = project.vaadin.widgetsetGenerator.replaceAll('.' + name, '')
            filename = name + ".java"
        }

        File dir = new File(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./, '/'))

        dir.mkdirs()

        def substitutions = [:]
        substitutions['packageName'] = pkg
        substitutions['className'] = filename.replaceAll('.java', '')

        TemplateUtil.writeTemplate2('MyConnectorBundleLoaderFactory.java', dir, filename, substitutions)
    }
}
