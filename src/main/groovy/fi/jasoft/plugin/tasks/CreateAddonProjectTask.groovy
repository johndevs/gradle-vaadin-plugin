package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
import fi.jasoft.plugin.creators.AddonThemeCreator
import fi.jasoft.plugin.creators.ComponentCreator
import fi.jasoft.plugin.creators.ProjectCreator
import fi.jasoft.plugin.creators.ThemeCreator
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Created by john on 8/31/16.
 */
class CreateAddonProjectTask extends DefaultTask {

    static final String NAME = 'vaadinCreateAddonProject'
    static final String DOT = '.'
    static final String PLUGIN_VERSION_ATTRIBUTE = 'pluginVersion'
    static final String MAIN_SOURCE_FOLDER = 'src/main/java/'
    static final String TEMPLATE_DIRECTORY = 'addonProject'
    static final String DEMO_APPLICATION_NAME = 'Demo'
    static final String BUILD_FILE = 'build.gradle'

    @Option(option = 'name', description = 'Addon name')
    def componentName = 'MyComponent'

    public CreateAddonProjectTask() {
        description = "Creates a new Vaadin addon development project."
    }

    @TaskAction
    def run() {

        makeGradleSettings()

        makeAddonModule()

        makeDemoModule()
    }

    private makeAddonModule() {
        def addonDir = project.file('addon')
        addonDir.mkdirs()

        def substitutions = [:]
        substitutions[PLUGIN_VERSION_ATTRIBUTE] = GradleVaadinPlugin.PLUGIN_VERSION
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

    def makeDemoModule() {
        def demoDir = project.file('demo')
        demoDir.mkdirs()

        def substitutions = [:]
        substitutions[PLUGIN_VERSION_ATTRIBUTE] = GradleVaadinPlugin.PLUGIN_VERSION

        TemplateUtil.writeTemplate('addonProject/demo.gradle', demoDir, BUILD_FILE, substitutions)

        new ProjectCreator(
                applicationName:DEMO_APPLICATION_NAME,
                applicationPackage: 'com.example.demo',
                widgetsetConfiguration:project.vaadinCompile as CompileWidgetsetConfiguration,
                javaDir:new File(demoDir, MAIN_SOURCE_FOLDER),
                resourceDir:new File(demoDir, 'src/main/resources/'),
                templateDir:TEMPLATE_DIRECTORY,
                uiImports: ["server.${componentName.toLowerCase()}.$componentName"],
                uiSubstitutions: ['addonComponentType' : componentName],
                pushSupported:Util.isPushSupportedAndEnabled(project),
                addonStylesSupported:Util.isAddonStylesSupported(project)
        ).run()

        new ThemeCreator(
                themeName:DEMO_APPLICATION_NAME,
                themesDirectory:new File(demoDir, 'src/main/webapp/VAADIN/themes'),
                vaadinVersion:Util.getVaadinVersion(project)
        ).run()
    }

    private makeGradleSettings() {
        TemplateUtil.writeTemplate('addonProject/settings.gradle', project.rootDir, 'settings.gradle')
    }
}
