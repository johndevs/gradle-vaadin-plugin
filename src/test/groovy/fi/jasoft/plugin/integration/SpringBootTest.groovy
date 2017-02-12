package fi.jasoft.plugin.integration

import fi.jasoft.plugin.TemplateUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Created by john on 2/1/17.
 */
@RunWith(Parameterized)
class SpringBootTest extends IntegrationTest {

    int port

    final String springBootVersion

    SpringBootTest(String springBootVersion) {
        this.springBootVersion = springBootVersion
    }

    @Parameters(name = "Spring Boot {0}")
    static Collection<String> getSpringBootVersions() {
        ['1.4.4.RELEASE', '1.5.1.RELEASE']
    }

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

        makeSpringBootProject()

        assertSpringBootProjectWasRunning()
    }

    @Test void 'Spring boot project with client dependencies'() {

        buildFile << """
            vaadinCompile {
                widgetset 'com.example.sprintboottest.MyWidgetset'
            }
        """.stripIndent()

        makeSpringBootProject()

        runWithArguments('vaadinCreateComponent', '--name=MyLabel')

        // Pre-compile so the timeout does not take the compilation into account
        // (which might make the test unstable on Travis)
        runWithArguments('vaadinCompile')

        assertSpringBootProjectWasRunning()
    }

    @Test void 'Run boot project with vaadinRun'() {

        makeSpringBootProject()

        assertSpringBootProjectWasRunning('vaadinRun')
    }

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        buildFile << """
            plugins {
                id 'org.springframework.boot' version '$springBootVersion'
            }
        """
    }

    private makeSpringBootProject() {
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
    }

    private void assertSpringBootProjectWasRunning(String taskName = 'bootRun') {
        String result = runWithArgumentsTimeout(TimeUnit.SECONDS.toMillis(60), {
            def page = "http://localhost:$port".toURL().text
            assertTrue 'Vaadin application was not loaded', page.contains(
                    'class="v-app MyApp myappui">')
        }, taskName)

        assertTrue result, result.contains('Started SpringBootApplication in')
        assertTrue result, result.contains('Vaadin is running')
        assertTrue result, result.contains('Found Vaadin UI [com.example.sprintboottest.MyAppUI]')
    }
}
