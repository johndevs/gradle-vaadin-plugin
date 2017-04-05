package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CreateProjectTask
import org.junit.Test

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import static org.junit.Assert.assertTrue

/**
 * Created by john on 4/5/17.
 */
class CreateProjectTest extends IntegrationTest {

    @Test void 'Create Project with special name'() {

        // Create
        runWithArguments(CreateProjectTask.NAME, '--name=hello-world')

        // Ensure it compiles
        runWithArguments('classes')

        File pkg = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java','com','example', 'helloworld').toFile()
        assertTrue 'Package name should have been converted', pkg.exists()
        assertTrue 'Servlet should exist', new File(pkg, 'HelloWorldServlet.java').exists()
        assertTrue 'UI should exist', new File(pkg, 'HelloWorldUI.java').exists()
    }
}
