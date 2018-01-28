package com.devsoap.plugin.tests

import com.devsoap.plugin.categories.ThemeCompile
import com.devsoap.plugin.categories.WidgetsetCompile
import com.devsoap.plugin.tasks.CompileWidgetsetTask
import com.devsoap.plugin.tasks.CreateComponentTask
import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test
import org.junit.experimental.categories.Category

import java.nio.file.Paths

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Created by john on 3/17/16.
 */
class CompileWidgetsetTest extends IntegrationTest {

    @Test void 'No widgetset, no compile'() {
        def result = runWithArguments(CreateProjectTask.NAME, CompileWidgetsetTask.NAME, '--info')
        assertFalse result, result.contains('Detected widgetset')
        assertFalse result, result.contains('Compiling module')
    }

    @Category(WidgetsetCompile)
    @Test void 'No widgetset defined, automatic widgetset detected and compiled'() {
        runWithArguments(CreateProjectTask.NAME, '--widgetset=com.example.MyWidgetset')
        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains('Detected widgetset com.example.MyWidgetset')
        assertTrue result, result.contains('Compiling module com.example.MyWidgetset')
        assertTrue result, result.contains('Linking succeeded')
    }

    @Category(WidgetsetCompile)
    @Test void 'Widgetset defined, manual widgetset detected and compiled'() {
        buildFile << """
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """
        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'No Widgetset defined, but addons exist in project'() {
        buildFile << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:+'
            }
        """

        runWithArguments(CreateProjectTask.NAME)

        def widgetsetName = 'AppWidgetset'
        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains("Compiling module $widgetsetName")
        assertTrue result, result.contains('Linking succeeded')

        File widgetsetFile = Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'resources',
                'AppWidgetset.gwt.xml').toFile()
        assertTrue "Widgetset file $widgetsetFile did not exist", widgetsetFile.exists()
    }

    @Test void 'Compile with Vaadin CDN'() {
        buildFile << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:+'
            }

            vaadinCompile {
                widgetsetCDN true
            }
        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains('Querying widgetset for')
        assertTrue result, result.contains('Widgetset is available, downloading...')
        assertTrue result, result.contains('Extracting widgetset')
        assertTrue result, result.contains('Generating AppWidgetset')

        File appWidgetset = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'AppWidgetset.java').toFile()
        assertTrue 'AppWidgetset.java was not created', appWidgetset.exists()

        File widgetsetFolder = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'webapp', 'VAADIN', 'widgetsets').toFile()
        assertTrue 'Widgetsets folder did not exist', widgetsetFolder.exists()
        assertTrue 'Widgetsets folder did not contain widgetset',
                widgetsetFolder.listFiles().size() == 1

    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with legacy dependencies'(){
        buildFile << """
            dependencies {
                compile("com.vaadin:vaadin-compatibility-server:8.0.0")
                compile("com.vaadin:vaadin-compatibility-client:8.0.0")
                compile("com.vaadin:vaadin-compatibility-shared:8.0.0")
            }
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with legacy dependencies and classpath jar'(){
        buildFile << """
            dependencies {
                compile("com.vaadin:vaadin-compatibility-server:8.0.0")
                compile("com.vaadin:vaadin-compatibility-client:8.0.0")
                compile("com.vaadin:vaadin-compatibility-shared:8.0.0")
            }
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
            vaadin.useClassPathJar = true

        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with upgraded validation-jar'() {
        buildFile << """
            dependencies {
                compile 'javax.validation:validation-api:1.1.0.Final'              
            }
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with client sources'() {
        buildFile << """            
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """

        runWithArguments(CreateProjectTask.NAME)

        runWithArguments(CreateComponentTask.NAME, '--name=MyLabel')

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with client sources and classpath jar'() {
        buildFile << """            
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
            vaadin.useClassPathJar = true
        """

        runWithArguments(CreateProjectTask.NAME)

        runWithArguments(CreateComponentTask.NAME, '--name=MyLabel')

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    @Category(WidgetsetCompile)
    @Test void 'Compile with third-party non-vaadin addon dependency'() {
        buildFile << """
            vaadin.version = "7.7.7"
            dependencies {
                vaadinCompile "org.vaadin.addon:v-leaflet:0.5.7"
            }
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertCompilationSucceeded(result)
    }

    private static void assertCompilationSucceeded(String result) {
        assertFalse result, result.contains('Detected widgetset com.example.MyWidgetset')
        assertTrue result, result.contains('Compiling module com.example.MyWidgetset')
        assertTrue result, result.contains('Linking succeeded')
    }
}
