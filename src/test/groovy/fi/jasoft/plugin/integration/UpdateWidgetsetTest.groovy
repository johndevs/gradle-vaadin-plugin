package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.UpdateWidgetsetTask
import org.junit.Assert
import org.junit.Test

import java.nio.file.Paths

/**
 * Created by john on 20.1.2016.
 */
class UpdateWidgetsetTest extends IntegrationTest {

    @Test void 'No Widgetset generated without property'(){
        runWithArguments(UpdateWidgetsetTask.NAME)
        Assert.assertFalse widgetsetFile.exists()
    }

    @Test void 'No Widgetset generated when widgetset management off'() {
        buildFile << "vaadin.widgetset = 'com.example.MyWidgetset'\n"
        buildFile << "vaadin.manageWidgetset = false"
        runWithArguments(UpdateWidgetsetTask.NAME)
        Assert.assertFalse widgetsetFile.exists()
    }

    @Test void 'Widgetset generated into resource folder'(){
        buildFile << "vaadin.widgetset = 'com.example.MyWidgetset'"
        runWithArguments(UpdateWidgetsetTask.NAME)
        Assert.assertTrue widgetsetFile.exists()
    }

    private File getWidgetsetFile() {
        Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'resources', 'com', 'example', 'MyWidgetset.gwt.xml').toFile()
    }
}
