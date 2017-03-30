package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.RunTask
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

/**
 * Created by john on 7/13/16.
 */
class GroovyProjectTest extends IntegrationTest{

    @Override
    protected void applyPlugin(File buildFile) {
        buildFile << "apply plugin:fi.jasoft.plugin.GradleVaadinGroovyPlugin\n"
    }

    @Test void 'Run Groovy Project'() {
        def output = runWithArguments(CreateProjectTask.NAME, RunTask.NAME, '--stopAfterStart')
        assertServerRunning output
    }

    @Test void 'Create Groovy Project'() {
        runWithArguments(CreateProjectTask.NAME)

        File sourceFolder = Paths.get(projectDir.root.canonicalPath, 'src','main','groovy').toFile()
        assertTrue('Source folder did not exist', sourceFolder.exists())

        File packageFolder = Paths.get(sourceFolder.canonicalPath, 'com', 'example', projectDir.root.name).toFile()
        assertTrue('Package did not exist', packageFolder.exists())

        assertEquals("There should be 2 files, found ${packageFolder.list().length}", 2, packageFolder.list().length)
        packageFolder.eachFile { File file ->
            if(!file.name.endsWith('.groovy')){
                fail "Only groovy files should exist, found $file.name"
            }
        }
    }

    private void assertServerRunning(String output) {
        assertTrue output, output.contains('Application running on ')
    }

}
