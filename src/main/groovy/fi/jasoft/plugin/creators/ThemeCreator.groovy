package fi.jasoft.plugin.creators

import fi.jasoft.plugin.TemplateUtil
import groovy.transform.Canonical
import org.gradle.util.VersionNumber

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Created by john on 9/12/16.
 */
@Canonical
class ThemeCreator implements Runnable {

    static final String STYLES_SCSS_FILE = 'styles.scss'
    static final String FAVICON_FILENAME = 'favicon.ico'

    private String themeName
    private File themesDirectory
    private String vaadinVersion

    @Override
    void run() {

        themeName = themeName ?: project.name

        def themeDir = new File(themesDirectory, themeName)
        themeDir.mkdirs()

        def substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = themeName.toLowerCase()

        String themeScssFile = themeName.toLowerCase() + '.scss'
        substitutions['themeImport'] = themeScssFile

        VersionNumber version = VersionNumber.parse(vaadinVersion)
        substitutions['basetheme'] = (version.major < 8 && version.minor < 3) ? 'reindeer' : 'valo'

        TemplateUtil.writeTemplate(STYLES_SCSS_FILE, themeDir, STYLES_SCSS_FILE, substitutions)
        TemplateUtil.writeTemplate('MyTheme.scss', themeDir, themeScssFile, substitutions)

        def favicon = ThemeCreator.class.getClassLoader().getResource(FAVICON_FILENAME)
        def faviconFile = new File(themeDir, FAVICON_FILENAME)

        Files.copy(favicon.openStream(), faviconFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
