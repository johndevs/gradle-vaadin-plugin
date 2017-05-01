package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.BuildJavadocJarTask
import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

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
}
