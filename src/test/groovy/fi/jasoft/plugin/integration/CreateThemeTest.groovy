package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.CreateThemeTask
import org.junit.Test

import java.nio.file.Paths
import static org.junit.Assert.assertTrue

/**
 * Created by john on 18.1.2016.
 */
class CreateThemeTest extends IntegrationTest {

    @Test void 'Create default theme'() {
        runWithArguments(CreateThemeTask.NAME, CompileThemeTask.NAME)
        def themesDir = Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'webapp', 'VAADIN', 'themes').toFile()
        assertThemeInDirectory(themesDir, projectDir.root.getName())
    }

    @Test void 'Create theme with name'() {
        runWithArguments(CreateThemeTask.NAME, '--name=TestingTheme', CompileThemeTask.NAME)
        def themesDir = Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'webapp', 'VAADIN', 'themes').toFile()
        assertThemeInDirectory(themesDir, 'TestingTheme')
    }

    @Test void 'Create theme in custom theme directory'() {
        buildFile << "vaadin.plugin.themesDirectory = 'build/mythemedir'"
        runWithArguments(CreateThemeTask.NAME, CompileThemeTask.NAME)
        def themesDir = Paths.get(projectDir.root.canonicalPath, 'build', 'mythemedir').toFile()
        assertThemeInDirectory(themesDir, projectDir.root.getName())
    }

    private void assertThemeInDirectory(File directory, String themeName){
        assertTrue directory.exists()

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue themeDir.exists()

        def addons = Paths.get(themeDir.canonicalPath, 'addons.scss').toFile()
        assertTrue themeDir.list().toArrayString(), addons.exists()

        def theme = Paths.get(themeDir.canonicalPath, themeName.toLowerCase()+'.scss').toFile()
        assertTrue themeDir.list().toArrayString(), theme.exists()

        def styles = Paths.get(themeDir.canonicalPath, 'styles.scss').toFile()
        assertTrue themeDir.list().toArrayString(), styles.exists()

        def stylesCompiled = Paths.get(themeDir.canonicalPath, 'styles.css').toFile()
        assertTrue themeDir.list().toArrayString(), stylesCompiled.exists()
    }




}
