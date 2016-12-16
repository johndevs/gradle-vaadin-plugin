package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CreateProjectTask
import fi.jasoft.plugin.tasks.RunTask
import org.junit.Test

import static org.junit.Assert.assertTrue

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

    private void assertServerRunning(String output) {
        assertTrue output, output.contains('Application running on ')
    }

}
