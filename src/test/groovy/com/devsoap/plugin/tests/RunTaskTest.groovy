package com.devsoap.plugin.tests

import com.devsoap.plugin.categories.RunProject
import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.RunTask
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue


/**
 * Created by john on 18.1.2016.
 */
@RunWith(Parameterized)
@Category(RunProject)
class RunTaskTest extends IntegrationTest {

    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.MINUTES)

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
        int port = freePort
        println "Running server on port $port"
        buildFile << """
        vaadinRun {
            debug false
            server '$server'
            serverPort $port
            themeAutoRecompile false
        }
        """.stripMargin()
    }

    @Test void 'Run server'() {
        runWithArguments(CreateProjectTask.NAME)
        def output = runWithArguments('--info', RunTask.NAME, '--stopAfterStart')

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

        runWithArguments(CreateProjectTask.NAME)
        def output = runWithArguments(RunTask.NAME, '--stopAfterStart')

        assertServerRunning output
        assertFalse output, output.contains('The defined classesDir does not contain any classes')
    }

    @Test void 'Run with debug flag'() {
        runWithArguments(CreateProjectTask.NAME)
        def output = runWithArguments('--info', RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    @Test void 'Run with custom logging'() {
        buildFile << """
            dependencies {
                compile 'ch.qos.logback:logback-classic:1.1.7'
                compile 'org.slf4j:slf4j-api:1.7.21'
            }
        """.stripIndent()

        runWithArguments(CreateProjectTask.NAME)
        def output = runWithArguments('--info', RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    @Test void 'Spring Loaded with classpath jar'() {
        buildFile << """
            vaadin.useClassPathJar true
        """.stripIndent()

        runWithArguments(CreateProjectTask.NAME)

        def output = runWithArguments('--debug', RunTask.NAME, '--stopAfterStart')
        assertFalse(output, output.contains('Spring Loaded jar not found'))
        assertTrue(output, output.contains('Using Spring Loaded found from'))
    }

    private void assertServerRunning(String output) {
        assertTrue output, output.contains('Application running on ')
    }

    private int getFreePort() {
        new ServerSocket(0).getLocalPort()
    }
}
