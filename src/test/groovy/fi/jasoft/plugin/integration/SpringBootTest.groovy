package fi.jasoft.plugin.integration

import fi.jasoft.plugin.TemplateUtil
import groovy.util.logging.Log
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertTrue

/**
 * Created by john on 2/1/17.
 */
class SpringBootTest extends IntegrationTest {

    int port

    @Override
    void setup() {
        super.setup()

        port = resolvePort()
        println "Running on port $port"

        buildFile << """
            springBoot {
                mainClass = 'com.example.sprintboottest.SpringBootApplication'
            }
            bootRun {
                bootRun.systemProperty 'server.port', '$port'
            }
        """.stripIndent()
    }

    int resolvePort() {
        ServerSocket socket
        int port = 8080
        try {
            socket = new ServerSocket(0)
            socket.reuseAddress = true
            port = socket.localPort
        } finally {
            socket.close()
        }
        port
    }

    @Test void 'Spring boot project with server only dependencies'() {

        runWithArguments('vaadinCreateProject', '--package=com.example.sprintboottest', '--name=MyApp')

        File appPackage = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'com', 'example', 'sprintboottest').toFile()
        assertTrue 'App package exists', appPackage.exists()

        File servlet = new File(appPackage, 'MyAppServlet.java')
        assertTrue 'Servlet could not be removed', servlet.delete()

        File ui = new File(appPackage, 'MyAppUI.java')
        assertTrue 'UI could not be removed', ui.delete()

        TemplateUtil.writeTemplate('SpringBootUI.java', appPackage, 'MyAppUI.java')

        TemplateUtil.writeTemplate('SpringBootApplication.java', appPackage)

        String result = runWithArgumentsTimeout(TimeUnit.SECONDS.toMillis(60), {
            def page = "http://localhost:$port".toURL().text
            assertTrue 'Vaadin application was not loaded', page.contains(
                    'class="v-app MyApp myappui">')
        }, 'bootRun')

        assertTrue result, result.contains('Started SpringBootApplication in')
        assertTrue result, result.contains('Vaadin is running')
        assertTrue result, result.contains('Found Vaadin UI [com.example.sprintboottest.MyAppUI]')
    }

    @Test void 'Spring boot project with client dependencies'() {

        buildFile << """
            vaadinCompile {
                widgetset 'com.example.sprintboottest.MyWidgetset'
            }
        """.stripIndent()

        runWithArguments('vaadinCreateProject', '--package=com.example.sprintboottest', '--name=MyApp')

        runWithArguments('vaadinCreateComponent', '--name=MyLabel')

        File appPackage = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'com', 'example', 'sprintboottest').toFile()
        assertTrue 'App package exists', appPackage.exists()

        File servlet = new File(appPackage, 'MyAppServlet.java')
        assertTrue 'Servlet could not be removed', servlet.delete()

        File ui = new File(appPackage, 'MyAppUI.java')
        assertTrue 'UI could not be removed', ui.delete()

        TemplateUtil.writeTemplate('SpringBootUI.java', appPackage, 'MyAppUI.java')

        TemplateUtil.writeTemplate('SpringBootApplication.java', appPackage)

        String result = runWithArgumentsTimeout(TimeUnit.SECONDS.toMillis(60), {
            def page = "http://localhost:$port".toURL().text
            assertTrue 'Vaadin application was not loaded', page.contains(
                    'class="v-app MyApp myappui">')
        }, 'bootRun')

        assertTrue result, result.contains('Started SpringBootApplication in')
        assertTrue result, result.contains('Vaadin is running')
        assertTrue result, result.contains('Found Vaadin UI [com.example.sprintboottest.MyAppUI]')
    }

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        buildFile << """
            plugins {
                id 'org.springframework.boot' version '1.4.4.RELEASE'
            }
        """
    }
}
