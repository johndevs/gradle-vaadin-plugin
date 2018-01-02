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
import com.devsoap.plugin.actions.SpringBootAction
import com.devsoap.plugin.creators.ProjectCreator
import com.devsoap.plugin.creators.ThemeCreator
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Vaadin Project
 *
 * @author John Ahlroos
 * @since 1.0
 */
class CreateProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateProject'

    private static final String DOT = '.'

    /**
     * The application class name
     */
    @Option(option = 'name', description = 'Application name')
    String applicationName

    /**
     * The application package
     */
    @Option(option = 'package', description = 'Application UI package')
    String applicationPackage

    /**
     * The fully qualified name of the widgetset
     */
    @Option(option = 'widgetset', description = 'Widgetset name')
    String widgetsetFQN

    CreateProjectTask() {
        description = "Creates a new Vaadin Project."
        finalizedBy UpdateAddonStylesTask.NAME, CompileThemeTask.NAME
    }

    /**
     * Creates a new project
     */
    @TaskAction
    void run() {
        CompileWidgetsetTask compileWidgetsetTask = project.tasks.getByName(CompileWidgetsetTask.NAME)

        String widgetset
        if(widgetsetFQN) {
            widgetset = widgetsetFQN
        } else if(compileWidgetsetTask.widgetset) {
            widgetset = compileWidgetsetTask.widgetset
        }

        new ProjectCreator(
                applicationName:resolveApplicationName(),
                applicationPackage:resolveApplicationPackage(),
                widgetsetCDN: compileWidgetsetTask.widgetsetCDN,
                widgetsetFQN:widgetset,
                pushSupported:Util.isPushEnabled(project),
                javaDir:Util.getMainSourceSet(project).srcDirs.first(),
                resourceDir:project.sourceSets.main.resources.srcDirs.iterator().next(),
                templateDir: 'simpleProject',
                projectType: Util.getProjectType(project),
                bootEnabled: SpringBootAction.isSpringBootPresent(project)
        ).run()

        new ThemeCreator(
                themeName:resolveApplicationName(),
                themesDirectory:Util.getThemesDirectory(project),
                vaadinVersion:Util.getVaadinVersion(project)
        ).run()

        UpdateWidgetsetTask.ensureWidgetPresent(project, widgetsetFQN)
    }

    private String resolveApplicationName() {

        // Use capitalized project name if no application name is given
        if ( !applicationName ) {
            applicationName = project.name.capitalize()
        }

        // Ensure name is Java compatible
        Util.makeStringJavaCompatible(applicationName).capitalize()
    }

    private String resolveApplicationPackage() {
        CompileWidgetsetTask compileWidgetsetTask = project.tasks.getByName(CompileWidgetsetTask.NAME)

        if ( !applicationPackage ) {
            int endSlashSize = 2
            if ( widgetsetFQN?.contains(DOT) ) {
                String widgetsetName = widgetsetFQN.tokenize(DOT).last()
                return widgetsetFQN[0..(-widgetsetName.size() - endSlashSize)]
            } else if ( compileWidgetsetTask.widgetset?.contains(DOT) ) {
                String widgetsetName = compileWidgetsetTask.widgetset.tokenize(DOT).last()
                return compileWidgetsetTask.widgetset[0..(-widgetsetName.size() - endSlashSize)]
            } else {
                return "com.example.${resolveApplicationName().toLowerCase()}"
            }
        }
        applicationPackage
    }
}
