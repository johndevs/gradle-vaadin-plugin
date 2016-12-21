package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.UpdateWidgetsetTask
import org.junit.Test

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

    @Test void 'No widgetset defined, automatic widgetset detected and compiled'() {
        runWithArguments(CreateProjectTask.NAME, '--widgetset=com.example.MyWidgetset')
        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains('Detected widgetset com.example.MyWidgetset')
        assertTrue result, result.contains('Compiling module com.example.MyWidgetset')
        assertTrue result, result.contains('Linking succeeded')
    }

    @Test void 'Widgetset defined, manual widgetset detected and compiled'() {
        buildFile << """
            vaadinCompile.widgetset = 'com.example.MyWidgetset'
        """
        runWithArguments(CreateProjectTask.NAME)

        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertFalse result, result.contains('Detected widgetset com.example.MyWidgetset')
        assertTrue result, result.contains('Compiling module com.example.MyWidgetset')
        assertTrue result, result.contains('Linking succeeded')
    }

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
        assertTrue result, result.contains('Generating AppWidgetset.java')

        File appWidgetset = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'AppWidgetset.java').toFile()
        assertTrue 'AppWidgetset.java was not created', appWidgetset.exists()

        File widgetsetFolder = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'webapp', 'VAADIN', 'widgetsets').toFile()
        assertTrue 'Widgetsets folder did not exist', widgetsetFolder.exists()
        assertTrue 'Widgetsets folder did not contain widgetset',
                widgetsetFolder.listFiles().size() == 1

    }

}
