/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.tasks

import com.devsoap.plugin.Util
import com.devsoap.plugin.creators.ComponentCreator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Component
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateComponentTask extends DefaultTask {

    static final String NAME = 'vaadinCreateComponent'

    /**
     * The component name
     */
    @Input
    @Option(option = 'name', description = 'Component name')
    String componentName = 'MyComponent'

    CreateComponentTask() {
        description = "Creates a new Vaadin Component."
    }

    @TaskAction
    void run() {

        String widgetset = Util.getWidgetset(project)
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