package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertTrue

/**
 * Created by john on 5/31/17.
 */
class KotlinTest extends IntegrationTest {

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        super.applyThirdPartyPlugins(buildFile)

        buildFile << """
           plugins {
                id "org.jetbrains.kotlin.jvm" version "1.1.2-5"
           }

           dependencies {
                compile "org.jetbrains.kotlin:kotlin-stdlib:1.1.2-5"
           }

        """.stripIndent()
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

