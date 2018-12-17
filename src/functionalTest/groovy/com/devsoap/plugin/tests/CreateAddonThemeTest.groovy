package com.devsoap.plugin.tests

import com.devsoap.plugin.tasks.CreateAddonThemeTask
import org.junit.Assert
import org.junit.Test

import java.nio.file.Paths

/**
 * Created by john on 19.1.2016.
 */
class CreateAddonThemeTest extends IntegrationTest {

    @Test void 'Create default theme'() {
        runWithArguments(CreateAddonThemeTask.NAME)
        assertThemeInDirectory('MyAddonTheme')
    }

    @Test void 'Create theme with name'() {
        runWithArguments(CreateAddonThemeTask.NAME, '--name=TestingTheme')
        assertThemeInDirectory('TestingTheme')
    }

    private void assertThemeInDirectory(String themeName) {

        def addonsDir = Paths.get(projectDir.root.canonicalPath, 'src', 'main', 'resources', 'VAADIN','addons').toFile()
        Assert.assertTrue addonsDir.exists()

        def themeDir = Paths.get(addonsDir.canonicalPath, themeName).toFile()
        Assert.assertTrue themeDir.exists()

        def theme = Paths.get(themeDir.canonicalPath, themeName+".scss").toFile()
        Assert.assertTrue theme.exists()
    }
}
