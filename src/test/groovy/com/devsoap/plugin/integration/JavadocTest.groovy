package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.BuildJavadocJarTask
import com.devsoap.plugin.tasks.CreateDirectoryZipTask
import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Created by john on 1/7/17.
 */
class JavadocTest extends IntegrationTest {

    @Test void 'Build Javadoc jar'() {
        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments(BuildJavadocJarTask.NAME)
        assertFalse result, result.contains('warnings')

        File libsDir = Paths.get(projectDir.root.canonicalPath, 'build', 'libs').toFile()
        File javadocJar = libsDir.listFiles().first()
        assertTrue 'Javadoc jar was missing', javadocJar.exists()
        assertTrue "$javadocJar was not a javadoc jar", javadocJar.name.endsWith('-javadoc.jar')
    }

    @Test void 'Build Javadoc jar with client dependencies'() {
        buildFile << 'vaadinCompile.widgetset = "com.example.MyWidgetset"\n'
        buildFile << 'vaadinCompile.widgetsetGenerator = "com.example.MyWidgetsetGenerator"\n'

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments(BuildJavadocJarTask.NAME)
        assertFalse result, result.contains('warnings')

        File libsDir = Paths.get(projectDir.root.canonicalPath, 'build', 'libs').toFile()
        File javadocJar = libsDir.listFiles().first()
        assertTrue 'Javadoc jar was missing', javadocJar.exists()
        assertTrue "$javadocJar was not a javadoc jar", javadocJar.name.endsWith('-javadoc.jar')
    }

    @Test void 'Build directory zip with javadoc and sources'() {
        runWithArguments(CreateProjectTask.NAME)

        runWithArguments(CreateDirectoryZipTask.NAME)

        File libsDir = Paths.get(projectDir.root.canonicalPath, 'build', 'libs').toFile()

        File javadocJar = libsDir.listFiles().find { it.name.endsWith('-javadoc.jar')}
        assertTrue 'Javadoc was not built', javadocJar.exists()

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
                case 3: assertEquals "libs/$projectDir.root.name-javadoc.jar".toString(), entry.name; break
                case 4: assertEquals "libs/$projectDir.root.name-sources.jar".toString(), entry.name; break
                case 5: assertEquals "libs/${projectDir.root.name}.jar".toString(), entry.name; break
                case 6: assertEquals 'javadoc/', entry.name; break
                default:
                    if(!entry.name.startsWith('javadoc/')) {
                        fail("Unexpected file $entry.name")
                    }
            }
        }
    }
}
