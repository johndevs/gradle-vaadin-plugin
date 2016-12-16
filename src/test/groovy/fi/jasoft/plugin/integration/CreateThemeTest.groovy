package fi.jasoft.plugin.integration

import fi.jasoft.plugin.tasks.CompileThemeTask
import fi.jasoft.plugin.tasks.CreateThemeTask
import org.junit.Before
import org.junit.Test

import java.nio.file.Paths
import static org.junit.Assert.assertTrue

/**
 * Created by john on 18.1.2016.
 */
class CreateThemeTest extends IntegrationTest {

    @Test void 'Create default theme'() {
        assertThemeCreatedAndCompiled()
    }

    @Test void 'Create default theme with classpath jar'() {
        buildFile << "vaadin.useClassPathJar = true"
        assertThemeCreatedAndCompiled()
    }

    @Test void 'Create theme with name'() {
        assertThemeCreatedAndCompiled('TestingTheme')
    }

    @Test void 'Create theme in custom theme directory'() {
        buildFile << "vaadinThemeCompile.themesDirectory = 'build/mythemedir'"
        runWithArguments(CreateThemeTask.NAME)

        def themesDir = Paths.get(projectDir.root.canonicalPath, 'build', 'mythemedir').toFile()
        assertThemeInDirectory(themesDir, projectDir.root.name)

        runWithArguments(CompileThemeTask.NAME)
        assertCompiledThemeInDirectory(themesDir, projectDir.root.name)
    }

    @Test void 'Compile with Compass compiler'() {
        buildFile << "vaadinThemeCompile.compiler = 'compass'"

        assertThemeCreatedAndCompiled('CompassTheme')
    }

    @Test void 'Compile with libSass compiler'() {
        buildFile << "vaadinThemeCompile.compiler = 'libsass'"

        assertThemeCreatedAndCompiled('LibsassTheme')
    }

    private void assertThemeCreatedAndCompiled(String themeName) {
        if (  themeName ) {
            runWithArguments(CreateThemeTask.NAME, "--name=$themeName")
        } else {
            runWithArguments(CreateThemeTask.NAME)
            themeName = projectDir.root.name
        }

        assertThemeInDirectory(themesDir, themeName)

        runWithArguments(CompileThemeTask.NAME)
        assertCompiledThemeInDirectory(themesDir, themeName)
    }

    private void assertThemeInDirectory(File directory, String themeName) {
        assertTrue "$directory does not exist", directory.exists()

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue "$themeDir does not exist", themeDir.exists()

        def addons = Paths.get(themeDir.canonicalPath, 'addons.scss').toFile()
        assertTrue themeDir.list().toArrayString(), addons.exists()

        def theme = Paths.get(themeDir.canonicalPath, themeName.toLowerCase()+'.scss').toFile()
        assertTrue themeDir.list().toArrayString(), theme.exists()

        def styles = Paths.get(themeDir.canonicalPath, 'styles.scss').toFile()
        assertTrue themeDir.list().toArrayString(), styles.exists()
    }

    private void assertCompiledThemeInDirectory(File directory, String themeName) {
        assertThemeInDirectory(directory, themeName)

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue "$themeDir does not exist", themeDir.exists()

        def stylesCompiled = Paths.get(themeDir.canonicalPath, 'styles.css').toFile()
        assertTrue "styles.css does not exist in theme dir, theme dir only contains "+themeDir.list().toArrayString(),
                stylesCompiled.exists()
    }

    private File getThemesDir() {
        Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'webapp', 'VAADIN', 'themes').toFile()
    }
}
