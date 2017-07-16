package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.RunTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.nio.file.Paths

import static org.junit.Assert.assertTrue

/**
 * Tests Kotlin project creation and usage
 */
@RunWith(Parameterized)
class KotlinTest extends KotlinIntegrationTest {

    KotlinTest(String kotlinVersion) {
        super(kotlinVersion)
    }

    @Parameterized.Parameters(name = "Kotlin {0}")
    static Collection<String> getKotlinVersions() {
        [ '1.1.3-2']
    }

    @Test void 'Create project'() {

        runWithArguments(CreateProjectTask.NAME, '--name=hello-world')

        File pkg = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'kotlin','com','example', 'helloworld').toFile()
        assertTrue 'Package name should have been converted', pkg.exists()
        assertTrue 'Servlet should exist', new File(pkg, 'HelloWorldServlet.kt').exists()
        assertTrue 'UI should exist', new File(pkg, 'HelloWorldUI.kt').exists()

        runWithArguments('classes')

        File classes = Paths.get(projectDir.root.canonicalPath,
                'build', 'classes', 'java', 'main', 'com','example', 'helloworld').toFile()
        assertTrue 'Classes should exist', classes.exists()
        assertTrue 'Servlet not compiled', new File(classes, 'HelloWorldServlet.class').exists()
        assertTrue 'UI not compiled', new File(classes, 'HelloWorldUI.class').exists()
    }

    @Test void 'Run with Jetty'() {
        buildFile << """
           val vaadinRun : com.devsoap.plugin.tasks.RunTask by tasks
           vaadinRun.apply {
                server = "jetty"
           }
        """.stripIndent()

        def output = runWithArguments('--info', CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertTrue output, output.contains("Starting jetty")
        assertTrue output, output.contains('Application running on ')
    }
}

