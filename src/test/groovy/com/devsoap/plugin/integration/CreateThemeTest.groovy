package com.devsoap.plugin.integration

import com.devsoap.plugin.tasks.CompileThemeTask
import com.devsoap.plugin.tasks.CreateThemeTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.nio.file.Paths
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

/**
 * Created by john on 18.1.2016.
 */
@RunWith(Parameterized)
class CreateThemeTest extends IntegrationTest {

    final String themeCompiler

    CreateThemeTest(String themeCompiler) {
        this.themeCompiler = themeCompiler
    }

    @Parameterized.Parameters(name = "{0}")
    static Collection<String> getThemeCompilers() {
        [ 'vaadin', 'compass', 'libsass' ]
    }

    @Override
    void setup() {
        super.setup()
        buildFile << "vaadinThemeCompile.compiler = '$themeCompiler'\n"
    }

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
        buildFile << "vaadinThemeCompile.themesDirectory = new File(project.buildDir, 'mythemedir').canonicalPath\n"
        runWithArguments(CreateThemeTask.NAME)

        def themesDir = Paths.get(projectDir.root.canonicalPath, 'build', 'mythemedir').toFile()
        assertThemeInDirectory(themesDir, projectDir.root.name)

        runWithArguments(CompileThemeTask.NAME)
        assertCompiledThemeInDirectory(themesDir, projectDir.root.name)
    }

    @Test void 'Create theme in custom external theme directory'() {
        File customThemesDir = File.createTempDir("$projectDir.root.name-", '-themes')
        customThemesDir.deleteOnExit()
        println "Created themes in $customThemesDir"

        buildFile << "vaadinThemeCompile.themesDirectory = '$customThemesDir.canonicalPath'"
        runWithArguments(CreateThemeTask.NAME)

        assertThemeInDirectory(customThemesDir, projectDir.root.name)

        runWithArguments(CompileThemeTask.NAME)
        assertCompiledThemeInDirectory(customThemesDir, projectDir.root.name)
    }

    @Test void 'Theme is compressed by default'() {
        assertThemeCreatedAndCompiled()
        assertCompressedThemeInDirectory(themesDir, projectDir.root.name)
    }

    @Test void 'Theme is not compressed if disabled'() {
        buildFile << "vaadinThemeCompile.compress = false"
        assertThemeCreatedAndCompiled()
        assertNoCompressedThemeInDirectory(themesDir, projectDir.root.name)
    }

    @Test void 'Build fails if compilation fails'() {

        runWithArguments(CreateThemeTask.NAME)

        File stylesSCSS = Paths.get(themesDir.canonicalPath, projectDir.root.name, 'styles.scss').toFile()

        // Add garbage so compilation fails
        stylesSCSS << "@mixin ic_img(\$name) {.}"

        runFailureExpected(CompileThemeTask.NAME)

        File stylesCSS = Paths.get(themesDir.canonicalPath, projectDir.root.name, 'styles.css').toFile()
        assertFalse 'Compiled theme should not exist', stylesCSS.exists()
    }

    private void assertThemeCreatedAndCompiled(String themeName=null) {
        if ( themeName ) {
            runWithArguments(CreateThemeTask.NAME, "--name=$themeName")
        } else {
            runWithArguments(CreateThemeTask.NAME)
            themeName = projectDir.root.name
        }

        assertThemeInDirectory(themesDir, themeName)

        runWithArguments(CompileThemeTask.NAME)
        assertCompiledThemeInDirectory(themesDir, themeName)
    }

    private static void assertThemeInDirectory(File directory, String themeName) {
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

    private static void assertCompiledThemeInDirectory(File directory, String themeName) {
        assertThemeInDirectory(directory, themeName)

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue "$themeDir does not exist", themeDir.exists()

        def stylesCompiled = Paths.get(themeDir.canonicalPath, 'styles.css').toFile()
        assertTrue "styles.css does not exist in theme dir, theme dir only contains " +
                themeDir.list().toArrayString(),
                stylesCompiled.exists()
    }

    private static void assertCompressedThemeInDirectory(File directory, String themeName) {
        assertThemeInDirectory(directory, themeName)

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue "$themeDir does not exist", themeDir.exists()

        def stylesCompiled = Paths.get(themeDir.canonicalPath, 'styles.css.gz').toFile()
        assertTrue "styles.css.gz does not exist in theme dir, theme dir only contains " +
                themeDir.list().toArrayString(),
                stylesCompiled.exists()
    }

    private static void assertNoCompressedThemeInDirectory(File directory, String themeName) {
        assertThemeInDirectory(directory, themeName)

        def themeDir = Paths.get(directory.canonicalPath, themeName).toFile()
        assertTrue "$themeDir does not exist", themeDir.exists()

        def stylesCompiled = Paths.get(themeDir.canonicalPath, 'styles.css.gz').toFile()
        assertFalse "styles.css.gz should not exist in theme dir, theme dir only contains " +
                themeDir.list().toArrayString(),
                stylesCompiled.exists()
    }

    private File getThemesDir() {
        Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'webapp', 'VAADIN', 'themes').toFile()
    }
}
