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
import fi.jasoft.plugin.creators.ComponentCreator
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

    @Option(option = 'name', description = 'Component name')
    def componentName = 'MyComponent'

    public CreateComponentTask() {
        description = "Creates a new Vaadin Component."
    }

    @TaskAction
    public void run() {

        def widgetset = Util.getWidgetset(project)
        if ( !widgetset ) {
            // Project does not yet have a client package, addons or widgetset. use AppWidgetset
            widgetset = Util.APP_WIDGETSET
        }

        new ComponentCreator(
                widgetset:widgetset,
                javaDir:Util.getMainSourceSet(project, true).srcDirs.first(),
                componentName:componentName
        ).run()
    }
}