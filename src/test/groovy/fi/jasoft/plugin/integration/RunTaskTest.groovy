package fi.jasoft.plugin.integration

import fi.jasoft.plugin.servers.JettyApplicationServer
import fi.jasoft.plugin.servers.PayaraApplicationServer
import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.RunTask
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue


/**
 * Created by john on 18.1.2016.
 */
class RunTaskTest extends IntegrationTest {

    @Test void 'Run default server'() {
        def output = runWithArguments(CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    @Test void 'Run Jetty server'() {
        buildFile << "vaadinRun.server = '${JettyApplicationServer.NAME}'"
        def output = runWithArguments('--info', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertTrue output, output.contains('Starting '+JettyApplicationServer.NAME)
        assertServerRunning output
    }

    @Test void 'Run Payara server'() {
        buildFile << "vaadinRun.server = '${PayaraApplicationServer.NAME}'"
        def output = runWithArguments('--info', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertTrue output, output.contains('Starting '+PayaraApplicationServer.NAME)
        assertServerRunning output
    }

    @Test void 'Run with custom classesDir without classes'() {
        buildFile << """
            vaadinRun {
                classesDir 'bin'
            }
        """
        def output = runWithArguments(CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output

        assertTrue output, output.contains('The defined classesDir does not contain any classes')
    }

    @Test void 'Run with custom classesDir'() {
        buildFile << """
            sourceSets {
                main.output.classesDir = 'bin'
                main.output.resourcesDir = 'bin'
            }
            vaadinRun {
                classesDir 'bin'
            }
        """

        def output = runWithArguments(CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output

        assertFalse output, output.contains('The defined classesDir does not contain any classes')
    }

    @Test void 'Run with debug flag'() {
        def output = runWithArguments('--debug', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    private void assertServerRunning(String output) {
        assertTrue output, output.contains('Application running on ')
    }
}
