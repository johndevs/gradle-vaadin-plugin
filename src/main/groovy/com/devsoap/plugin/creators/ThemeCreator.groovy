/*
 * Copyright 2017 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin.creators

import com.devsoap.plugin.TemplateUtil
import groovy.transform.Canonical
import org.gradle.util.VersionNumber

import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Creates a new Vaadin project theme from a pre-defined template
 *
 * @author John Ahlroos
 * @since 1.1
 */
@Canonical
class ThemeCreator implements Runnable {

    private static final String STYLES_SCSS_FILE = 'styles.scss'
    private static final String FAVICON_FILENAME = 'favicon.ico'

    /**
     * The theme name. If not set then it will be the project name
     */
    String themeName

    /**
     * The theme directory
     */
    File themesDirectory

    /**
     * The vaadin version
     */
    String vaadinVersion

    @Override
    void run() {

        themeName = themeName ?: project.name

        File themeDir = new File(themesDirectory, themeName)
        themeDir.mkdirs()

        Map substitutions = [:]
        substitutions['themeName'] = themeName
        substitutions['theme'] = themeName.toLowerCase()

        String themeScssFile = themeName.toLowerCase() + '.scss'
        substitutions['themeImport'] = themeScssFile

        VersionNumber version = VersionNumber.parse(vaadinVersion)
        substitutions['basetheme'] = (version.major < 8 && version.minor < 3) ? 'reindeer' : 'valo'

        TemplateUtil.writeTemplate(STYLES_SCSS_FILE, themeDir, STYLES_SCSS_FILE, substitutions)
        TemplateUtil.writeTemplate('MyTheme.scss', themeDir, themeScssFile, substitutions)

        URL favicon = ThemeCreator.classLoader.getResource(FAVICON_FILENAME)
        File faviconFile = new File(themeDir, FAVICON_FILENAME)

        Files.copy(favicon.openStream(), faviconFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}
