package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CompileWidgetsetTask
import fi.jasoft.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

/**
 * Created by john on 3/17/16.
 */
class CompileWidgetsetTest extends IntegrationTest {

    @Test void 'No widgetset, no compile'(){
        def result = runWithArguments(CreateProjectTask.NAME, CompileWidgetsetTask.NAME, '--info')
        assertFalse result, result.contains('Detected widgetset')
        assertFalse result, result.contains('Compiling module')
    }

    @Test void 'No widgetset defined, automatic widgetset detected and compiled'(){
        runWithArguments(CreateProjectTask.NAME, '--widgetset=com.example.MyWidgetset')
        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains('Detected widgetset com.example.MyWidgetset')
        assertTrue result, result.contains('Compiling module com.example.MyWidgetset')
        assertTrue result, result.contains('Linking succeeded')
    }

    @Test void 'Widgetset defined, manual widgetset detected and compiled'(){
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
                compile 'org.vaadin.addons:qrcode:2.0.1'
            }
        """

        runWithArguments(CreateProjectTask.NAME)

        def widgetsetName = 'addon.client.' + projectDir.root.name.capitalize() + 'Widgetset'
        def result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains("Compiling module $widgetsetName")
        assertTrue result, result.contains('Linking succeeded')

        File widgetsetFile = Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'resources',
                'addon','client', projectDir.root.name.capitalize() + 'Widgetset.gwt.xml').toFile()
        assertTrue "Widgetset file $widgetsetFile did not exist", widgetsetFile.exists()
    }
}
