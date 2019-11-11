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

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util
import com.devsoap.plugin.creators.AddonThemeCreator
import com.devsoap.plugin.creators.ComponentCreator
import com.devsoap.plugin.creators.ProjectCreator
import com.devsoap.plugin.creators.ThemeCreator
import com.devsoap.plugin.extensions.VaadinPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new addon project with two modules, one addon module and one demo modules which utilizes the addon
 *
 * @author John Ahlroos
 * @since 1.1
 */
class CreateAddonProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateAddonProject'

    private static final String PLUGIN_VERSION_ATTRIBUTE = 'pluginVersion'
    private static final String MAIN_SOURCE_FOLDER = 'src/main/java/'
    private static final String TEMPLATE_DIRECTORY = 'addonProject'
    private static final String DEMO_APPLICATION_NAME = 'Demo'
    private static final String BUILD_FILE = 'build.gradle'

    /**
     * The addon name
     */
    @Input
    @Option(option = 'name', description = 'Addon name')
    String componentName = 'MyComponent'

    CreateAddonProjectTask() {
        description = "Creates a new Vaadin addon development project."
    }

    /**
     * Creates the addon project
     */
    @TaskAction
    void run() {

        makeGradleSettings()

        makeAddonModule()

        makeDemoModule()
    }

    private makeAddonModule() {
        def addonDir = project.file('addon')
        addonDir.mkdirs()

        def substitutions = [:]
        substitutions[PLUGIN_VERSION_ATTRIBUTE] = GradleVaadinPlugin.version
        substitutions['author'] = 'Kickass Vaadin Ninja'
        substitutions['license'] = 'Apache 2.0'
        substitutions['title'] = componentName
        substitutions['rootPackagePath'] = "server/${componentName.toLowerCase()}"
        substitutions['artifactId'] = componentName.toLowerCase()
        TemplateUtil.writeTemplate('addonProject/addon.gradle', addonDir, BUILD_FILE, substitutions)

        new ComponentCreator(
                javaDir:new File(addonDir, MAIN_SOURCE_FOLDER),
                componentName:componentName
        ).run()

        new AddonThemeCreator(
                resourceDir:new File(addonDir, 'src/main/resources'),
                themeName:componentName,
                templateDir:TEMPLATE_DIRECTORY
        ).run()

        def widgetsetDir = new File(addonDir, 'src/main/resources/client')
        widgetsetDir.mkdirs()
        TemplateUtil.writeTemplate('addonProject/AddonWidgetset.xml', widgetsetDir, "${componentName}Widgetset.gwt.xml")
    }

    private makeDemoModule() {
        def demoDir = project.file('demo')
        demoDir.mkdirs()

        def substitutions = [:]
        substitutions[PLUGIN_VERSION_ATTRIBUTE] = GradleVaadinPlugin.version

        TemplateUtil.writeTemplate('addonProject/demo.gradle', demoDir, BUILD_FILE, substitutions)

        CompileWidgetsetTask compileWidgetsetTask = project.tasks.getByName(CompileWidgetsetTask.NAME)
        VaadinPluginExtension vaadin = project.extensions.getByType(VaadinPluginExtension)
        new ProjectCreator(
                applicationName:DEMO_APPLICATION_NAME,
                applicationPackage: 'com.example.demo',
                widgetsetCDN: compileWidgetsetTask.widgetsetCDN,
                javaDir:new File(demoDir, MAIN_SOURCE_FOLDER),
                resourceDir:new File(demoDir, 'src/main/resources/'),
                templateDir:TEMPLATE_DIRECTORY,
                uiImports: ["server.${componentName.toLowerCase()}.$componentName"],
                uiSubstitutions: ['addonComponentType' : componentName],
                pushSupported:Util.isPushEnabled(project),
                projectType: Util.getProjectType(project)
        ).run()

        new ThemeCreator(
                themeName:DEMO_APPLICATION_NAME,
                themesDirectory:new File(demoDir, 'src/main/webapp/VAADIN/themes'),
                vaadinVersion:vaadin.version
        ).run()
    }

    private makeGradleSettings() {
        TemplateUtil.writeTemplate('addonProject/settings.gradle', project.rootDir, 'settings.gradle')
    }
}
