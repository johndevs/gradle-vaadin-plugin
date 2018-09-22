package com.devsoap.plugin.tests

import com.devsoap.plugin.categories.WidgetsetAndThemeCompile
import org.junit.Assert
import org.junit.Test
import org.junit.experimental.categories.Category

import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.junit.Assert.assertTrue

/**
 * Tests the creation the WAR archive
 */
class WARArchiveTest extends IntegrationTest {

    @Override
    void setup() {
        super.setup()
        buildFile << "vaadin.version = '7.6.4'\n"
    }

    @Test void 'WAR task action is run'() {
        String output = runWithArguments('--info', 'build')
        assertTrue output.contains('Applying JavaPluginAction')
        assertTrue output.contains('Applying WarPluginAction')
    }

    @Category(WidgetsetAndThemeCompile)
    @Test void 'Project with no dependencies'() {

        runWithArguments('war')

        // Files in WEB-INF/lib
        def final FILES_IN_WEBINF_LIB = [
                'vaadin-server-7.6.4.jar',
                'vaadin-themes-7.6.4.jar',
                'vaadin-sass-compiler-0.9.13.jar',
                'vaadin-shared-7.6.4.jar',
                'jsoup-1.8.3.jar',
                'sac-1.3.jar',
                'flute-1.3.0.gg2.jar',
                'yuicompressor-2.4.8.jar',
                'streamhtmlparser-jsilver-0.0.10.vaadin1.jar',
                'guava-16.0.1.vaadin1.jar',
                'js-1.7R2.jar',
                'vaadin-client-compiled-7.6.4.jar'
        ]

        assertFilesInFolder(warFile, FILES_IN_WEBINF_LIB, 'WEB-INF/lib')
    }

    @Category(WidgetsetAndThemeCompile)
    @Test void 'Project with widgetset'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.TestWidgetset'\n"

        runWithArguments('war')

        // Files in WEB-INF/lib
        def final FILES_IN_WEBINF_LIB = [
                'vaadin-server-7.6.4.jar',
                'vaadin-themes-7.6.4.jar',
                'vaadin-sass-compiler-0.9.13.jar',
                'vaadin-shared-7.6.4.jar',
                'jsoup-1.8.3.jar',
                'sac-1.3.jar',
                'flute-1.3.0.gg2.jar',
                'yuicompressor-2.4.8.jar',
                'streamhtmlparser-jsilver-0.0.10.vaadin1.jar',
                'guava-16.0.1.vaadin1.jar',
                'js-1.7R2.jar'
        ]

        assertFilesInFolder(warFile, FILES_IN_WEBINF_LIB, 'WEB-INF/lib')
    }

    @Category(WidgetsetAndThemeCompile)
    @Test void 'Project theme is included in archive'() {

        runWithArguments('vaadinCreateProject')
        runWithArguments('war')

        def final THEME_FILES = [
            'styles.scss',
            'styles.css',
            'styles.css.gz',
            "${projectDir.root.name}.scss".toString(),
            'addons.scss',
            'favicon.ico'
        ]

        assertFilesInFolder(warFile, THEME_FILES, "VAADIN/themes/${projectDir.root.name.capitalize()}".toString())
    }

    @Category(WidgetsetAndThemeCompile)
    @Test void 'Project widgetset is included in archive'() {

        runWithArguments('vaadinCreateProject', '--widgetset=com.example.Widgetset')
        runWithArguments('war')

        def final WIDGETSET_FILES = [
                'com.example.Widgetset.nocache.js.gz',
                'com.example.Widgetset.nocache.js'
        ]

        assertFilesInFolder(warFile, WIDGETSET_FILES, 'VAADIN/widgetsets/com.example.Widgetset', true)
    }

    @Test void 'Provided and runtime dependencies not included'() {
        buildFile << """
        dependencies {
            runtime 'commons-lang:commons-lang:2.6'
            compileOnly 'commons-lang:commons-lang:2.6'
            providedCompile 'commons-lang:commons-lang:2.6'
        }
        """.stripIndent()

        // Adding provided and runtime dependencies should result in the same WAR as when
        // none of those dependencies are added
        'Project with no dependencies'()
    }

    @Category(WidgetsetAndThemeCompile)
    @Test void 'Vaadin addons in vaadinCompile are added to war'() {
        buildFile << """
        dependencies {
            vaadinCompile 'commons-lang:commons-lang:2.6'
        }
        """.stripIndent()

        runWithArguments('war')

        assertFilesInFolder(warFile, ["commons-lang-2.6.jar"], 'WEB-INF/lib', true)
    }

    private static List<ZipEntry> getFilesInFolder(ZipFile archive, String folder) {
        archive.entries().findAll { ZipEntry entry ->
            !entry.directory && entry.name.startsWith(folder)
        }
    }

    private ZipFile getWarFile() {
        def libsDir = Paths.get(projectDir.root.canonicalPath, 'build', 'libs').toFile()
        new ZipFile(new File(libsDir, projectDir.root.name + '.war'))
    }

    private static void assertFilesInFolder(ZipFile archive, List<String> files, String folder,
                                            boolean ignoreExtraFiles = false) {
        def webInfLib = getFilesInFolder(archive, folder)

        // Check for extra files
        if ( !ignoreExtraFiles ) {
            webInfLib.each { ZipEntry entry ->
                Assert.assertTrue(
                        "Archive contained extra file $entry.name",
                        files.contains(entry.name - (folder + '/')))
            }
        }

        // Check for missing files
        files.each { String fileName ->
            ZipEntry file = webInfLib.find { ZipEntry entry ->
                entry.name == "$folder/$fileName".toString()
            }
            Assert.assertNotNull("File $folder/$fileName was missing from archive", file)
        }
    }
}
