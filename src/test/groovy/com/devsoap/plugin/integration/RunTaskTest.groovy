package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.RunTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue


/**
 * Created by john on 18.1.2016.
 */
@RunWith(Parameterized)
class RunTaskTest extends IntegrationTest {

    final String server

    RunTaskTest(String server) {
        this.server = server
    }

    @Parameterized.Parameters(name = "{0}")
    static Collection<String> getServers() {
        [ 'payara', 'jetty']
    }

    @Override
    void setup() {
        super.setup()
        buildFile << "vaadinRun.server = '$server'\n"
    }

    @Test void 'Run server'() {
        def output = runWithArguments('--info', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertTrue output, output.contains("Starting $server")
        assertServerRunning output
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

    @Test void 'Run with custom logging'() {
        buildFile << """
            dependencies {
                compile 'ch.qos.logback:logback-classic:1.1.7'
                compile 'org.slf4j:slf4j-api:1.7.21'
            }
        """.stripIndent()
        def output = runWithArguments('--debug', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    @Test void 'Spring Loaded with classpath jar'() {
        buildFile << """
            vaadin.useClassPathJar true
        """.stripIndent()

        def output = runWithArguments('--info', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertFalse(output, output.contains('Spring Loaded jar not found'))
        assertTrue(output, output.contains('Using Spring Loaded found from'))
    }

    private void assertServerRunning(String output) {
        assertTrue output, output.contains('Application running on ')
    }
}
