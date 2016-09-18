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

import fi.jasoft.plugin.Util
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
import fi.jasoft.plugin.creators.ProjectCreator
import fi.jasoft.plugin.creators.ThemeCreator
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Project
 *
 * @author John Ahlroos
 */
class CreateProjectTask extends DefaultTask {

    static final NAME = 'vaadinCreateProject'

    static final String DOT = '.'

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

        new ProjectCreator(applicationName: resolveApplicationName(),
                applicationPackage: resolveApplicationPackage(),
                widgetsetConfiguration: configuration,
                widgetsetFQN: widgetsetFQN,
                pushSupported: Util.isPushSupportedAndEnabled(project),
                addonStylesSupported: Util.isAddonStylesSupported(project),
                javaDir: Util.getMainSourceSet(project).srcDirs.first(),
                resourceDir: project.sourceSets.main.resources.srcDirs.iterator().next(),
                templateDir: 'simpleProject'
        ).run()

        if (Util.isAddonStylesSupported(project)) {

            new ThemeCreator(themeName: resolveApplicationName(),
                    themesDirectory: Util.getThemesDirectory(project),
                    vaadinVersion: Util.getVaadinVersion(project)
            ).run()

            project.tasks[UpdateAddonStylesTask.NAME].run()
        }

        UpdateWidgetsetTask.ensureWidgetPresent(project, widgetsetFQN)
    }

    @PackageScope
    String resolveApplicationName() {

        // Use capitalized project name if no application name is given
        if(!applicationName){
            applicationName = project.name.capitalize()
        }

        // Ensure name is Java compatible
        Util.makeStringJavaCompatible(applicationName)
    }

    @PackageScope
    String resolveApplicationPackage() {
        def configuration = project.vaadinCompile as CompileWidgetsetConfiguration
        if(!applicationPackage){
            int endSlashSize = 2
            if(widgetsetFQN?.contains(DOT)){
                String widgetsetName = widgetsetFQN.tokenize(DOT).last()
                widgetsetFQN[0..(-widgetsetName.size() - endSlashSize)]
            } else if (configuration.widgetset?.contains(DOT)) {
                String widgetsetName = configuration.widgetset.tokenize(DOT).last()
                configuration.widgetset[0..(-widgetsetName.size() - endSlashSize)]
            } else {
                "com.example.${applicationName.toLowerCase()}"
            }
        }
    }
}
