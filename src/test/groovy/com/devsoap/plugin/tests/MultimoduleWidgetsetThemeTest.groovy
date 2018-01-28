package com.devsoap.plugin.tests

import com.devsoap.plugin.categories.WidgetsetCompile
import com.devsoap.plugin.tasks.BuildClassPathJar
import com.devsoap.plugin.tasks.CreateComponentTask
import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.CreateThemeTask
import org.junit.Test
import org.junit.experimental.categories.Category

import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Created by john on 1/11/17.
 */
class MultimoduleWidgetsetThemeTest extends MultiProjectIntegrationTest {

    @Category(WidgetsetCompile)
    @Test void 'Multimodule project with shared widgetset and theme'() {
        File widgetsetModule = makeProject('widgetset-module')
        File widgetsetBuildFile = makeBuildFile(widgetsetModule)
        widgetsetBuildFile << """
            vaadinCompile {
                widgetset 'com.example.MyWidgetset'
            }

            vaadinAddon {
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
            vaadinAddon {
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

        runWithArgumentsOnProject(themeModule, CreateThemeTask.NAME, '--name=AppTheme')
        runWithArgumentsOnProject(widgetsetModule, CreateComponentTask.NAME, '--name=MyLabel')
        runWithArguments("app:$CreateProjectTask.NAME")

        // Remove generated theme from app
        Paths.get(appModule.canonicalPath, 'src', 'main', 'webapp').deleteDir()

        // Generate war
        String result = runWithArguments('app:war')
        assertTrue result, result.contains('BUILD SUCCESSFUL')
    }

    @Test void 'Multimodule project with classpath jar'() {

        buildFile = makeBuildFile(projectDir.root)
        buildFile << """
            vaadin.useClassPathJar = true

            dependencies {
                compile project(':theme-module')
            }
        """.stripIndent()

        File themeModule = makeProject('theme-module')
        File themeModuleBuildFile = makeBuildFile(themeModule)
        themeModuleBuildFile << """
            vaadinAddon {
                title 'app-theme'
                version '1'
            }

            // Package theme into jar for use by apps
            jar.dependsOn 'vaadinThemeCompile'
            jar.from 'src/main/webapp'
        """.stripIndent()

        runWithArgumentsOnProject(themeModule, CreateThemeTask.NAME, '--name=AppTheme')
        runWithArguments(CreateProjectTask.NAME)
        runWithArguments(BuildClassPathJar.NAME)

        File manifest = Paths.get(projectDir.root.canonicalPath,
                'build', 'tmp', 'vaadinClassPathJar', 'MANIFEST.MF').toFile()
        assertTrue 'Manifest did not exist', manifest.exists()

        manifest.withDataInputStream { stream ->
            Manifest m = new Manifest(stream)
            String cp = m.mainAttributes.getValue("Class-Path")
            assertNotNull 'Attribute Class-Path not found in attributes '+m.mainAttributes, cp
            assertTrue cp.contains('theme-module-1.jar')
        }

    }
}