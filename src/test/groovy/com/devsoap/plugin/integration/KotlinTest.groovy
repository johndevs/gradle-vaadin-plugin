package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue

/**
 * Tests Kotlin project creation and usage
 */
class KotlinTest extends KotlinIntegrationTest {

    KotlinTest(String kotlinVersion) {
        super(kotlinVersion)
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
}

