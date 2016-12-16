package fi.jasoft.plugin.integration

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.tasks.UpdateWidgetsetTask
import org.junit.Assert
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Created by john on 20.1.2016.
 */
class UpdateWidgetsetTest extends IntegrationTest {

    @Test void 'No Widgetset generated without property'() {
        runWithArguments(UpdateWidgetsetTask.NAME)
        assertFalse widgetsetFile.exists()
    }

    @Test void 'No Widgetset generated when widgetset management off'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.MyWidgetset'\n"
        buildFile << "vaadin.manageWidgetset = false"
        runWithArguments(UpdateWidgetsetTask.NAME)
        assertFalse widgetsetFile.exists()
    }

    @Test void 'Widgetset generated into resource folder'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.MyWidgetset'"
        runWithArguments(UpdateWidgetsetTask.NAME)
        assertTrue widgetsetFile.exists()
    }

    @Test void 'Widgetset file contains addon widgetset inherits'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.MyWidgetset'\n"
        buildFile << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:2.0.1'
            }
        """

        runWithArguments(UpdateWidgetsetTask.NAME)
        assertTrue widgetsetFile.text.contains('<inherits name="fi.jasoft.qrcode.QrcodeWidgetset" />')
    }

    @Test void 'Widgetset file contains inherits from sub-project dependencies'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.MyWidgetset'\n"

        // Setup project 1
        File project1Dir = projectDir.newFolder('project1')
        project1Dir.mkdirs()
        File buildFile1 = makeBuildFile(project1Dir)
        buildFile1 << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:2.0.1'
            }
        """

        // Setup project 2
        File project2Dir = projectDir.newFolder('project2')
        project2Dir.mkdirs()
        File buildFile2 = makeBuildFile(project2Dir)
        buildFile2 << "vaadinCompile.widgetset = 'com.example.MyWidgetset'\n"
        buildFile2 << """
            dependencies {
                compile project(':project1')
            }
        """

        // Setup settings.gradle
        settingsFile << """
            include 'project1'
            include 'project2'
        """

        runWithArguments(':project2:' + UpdateWidgetsetTask.NAME)
        assertTrue getWidgetsetFile(project2Dir).text.contains('<inherits name="fi.jasoft.qrcode.QrcodeWidgetset" />')
    }

    @Test void 'AppWidgetset created when project contains addon dependencies'() {
        buildFile << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:2.0.1'
            }
        """
        runWithArguments(UpdateWidgetsetTask.NAME)
        assertTrue appWidgetsetFile.exists()
        assertTrue appWidgetsetFile.text.contains('<inherits name="fi.jasoft.qrcode.QrcodeWidgetset" />')
    }

    @Test void 'AppWidgetset created when dependant project contains widgetset file'() {

        // Setup addon project
        File project1Dir = projectDir.newFolder('project1')
        project1Dir.mkdirs()

        File widgetsetFile = getWidgetsetFile(project1Dir)
        widgetsetFile.parentFile.mkdirs()
        widgetsetFile.createNewFile()
        widgetsetFile.text = """
            <module>
                <inherits name="com.vaadin.DefaultWidgetSet" />
            </module>
        """.stripIndent()

        File buildFile1 = makeBuildFile(project1Dir)
        buildFile1 << "vaadinCompile.widgetset = 'com.example.MyWidgetset'\n"

        // Setup demo project
        File project2Dir = projectDir.newFolder('project2')
        project2Dir.mkdirs()
        File buildFile2 = makeBuildFile(project2Dir)
        buildFile2 << """
            dependencies {
                compile project(':project1')
            }
        """

        // Setup settings.gradle
        settingsFile << """
            include 'project1'
            include 'project2'
        """

        runWithArguments(':project2:' + UpdateWidgetsetTask.NAME)
        assertTrue getAppWidgetsetFile(project2Dir).text.contains('<inherits name="com.example.MyWidgetset" />')
    }

    private File getWidgetsetFile(File projectDir = this.projectDir.root, String fileName='MyWidgetset') {
        Paths.get(projectDir.canonicalPath,
                'src', 'main', 'resources', 'com', 'example', "${fileName}.gwt.xml").toFile()
    }

    private File getAppWidgetsetFile(File projectDir = this.projectDir.root) {
        Paths.get(projectDir.canonicalPath,'src', 'main', 'resources', "AppWidgetset.gwt.xml").toFile()
    }
}
