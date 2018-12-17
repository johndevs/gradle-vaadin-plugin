package com.devsoap.plugin.tests

import com.devsoap.plugin.tasks.CreateDirectoryZipTask
import com.devsoap.plugin.tasks.CreateProjectTask
import com.devsoap.plugin.tasks.RunTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.junit.Assert.assertEquals
import static org.junit.Assert.fail
import static org.junit.Assert.assertNull
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
        [ '1.3.11']
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
                'build', 'classes', 'kotlin', 'main', 'com','example', 'helloworld').toFile()
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

    @Test void 'No javadoc for Kotlin projects'() {
        runWithArguments(CreateDirectoryZipTask.NAME)

        File libsDir = Paths.get(projectDir.root.canonicalPath, 'build', 'libs').toFile()

        File javadocJar = libsDir.listFiles().find { it.name.endsWith('-javadoc.jar')}
        assertNull 'Javadoc was built', javadocJar

        File sourcesJar = libsDir.listFiles().find { it.name.endsWith('-sources.jar')}
        assertTrue 'Sources was not built', sourcesJar.exists()

        File distributionDir = Paths.get(projectDir.root.canonicalPath, 'build', 'distributions').toFile()

        File addonZip = distributionDir.listFiles().first()
        assertTrue 'Distribution zip was not built', addonZip.exists()

        ZipFile zip = new ZipFile(addonZip)
        zip.entries().eachWithIndex { ZipEntry entry, int i ->
            switch (i) {
                case 0: assertEquals 'META-INF/', entry.name; break
                case 1: assertEquals 'META-INF/MANIFEST.MF', entry.name; break
                case 2: assertEquals 'libs/', entry.name; break
                case 3: assertEquals "libs/$projectDir.root.name-sources.jar".toString(), entry.name; break
                case 4: assertEquals "libs/${projectDir.root.name}.jar".toString(), entry.name; break
                default: fail("Unexpected file $entry.name")
            }
        }
    }
}

