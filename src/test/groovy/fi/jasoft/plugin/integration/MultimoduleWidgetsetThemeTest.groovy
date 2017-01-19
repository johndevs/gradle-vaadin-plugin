package fi.jasoft.plugin.integration

import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue

/**
 * Created by john on 1/11/17.
 */
class MultimoduleWidgetsetThemeTest extends MultiProjectIntegrationTest {

    @Test void 'Multimodule project with shared widgetset and theme'() {
        File widgetsetModule = makeProject('widgetset-module')
        File widgetsetBuildFile = makeBuildFile(widgetsetModule)
        widgetsetBuildFile << """
            vaadinCompile {
                widgetset 'com.example.MyWidgetset'
            }

            vaadin.addon {
                title 'app-widgetset'
                version '1'
            }

            // Package widgetset into jar for use by apps
            jar.dependsOn 'vaadinCompile'
            jar.from 'src/main/webapp'
        """.stripIndent()

        File themeModule = makeProject('theme-module')
        File themeModuleBuildFile = makeBuildFile(themeModule)
        themeModuleBuildFile << """
            vaadin.addon {
                title 'app-theme'
                version '1'
            }

            // Package theme into jar for use by apps
            jar.dependsOn 'vaadinThemeCompile'
            jar.from 'src/main/webapp'
        """.stripIndent()

        File appModule = makeProject('app')
        File appBuildFile = makeBuildFile(appModule)
        appBuildFile << """
            // Disable widgetset compilation as widgetset
            // is served from widgetset-module
            project.tasks.vaadinCompile.enabled = false
            project.tasks.vaadinUpdateWidgetset.enabled = false

            // Disable theme compilation as theme is pre-compiled
            // in another module
            project.tasks.vaadinThemeCompile.enabled = false
            project.tasks.vaadinUpdateAddonStyles.enabled = false

            dependencies {
                compile project(':theme-module')
                compile project(':widgetset-module')
            }
        """.stripIndent()

        runWithArgumentsOnProject(themeModule, 'vaadinCreateTheme', '--name=AppTheme')
        runWithArgumentsOnProject(widgetsetModule, 'vaadinCreateComponent', '--name=MyLabel')
        runWithArguments('app:vaadinCreateProject')

        // Remove generated theme from app
        Paths.get(appModule.canonicalPath, 'src', 'main', 'webapp').deleteDir()

        // Generate war
        String result = runWithArguments('app:war')
        assertTrue result, result.contains('BUILD SUCCESSFUL')
    }
}