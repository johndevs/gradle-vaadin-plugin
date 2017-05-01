/*
* Copyright 2017 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.devsoap.plugin.tasks

import com.devsoap.plugin.Util
import com.devsoap.plugin.configuration.CompileThemeConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction

/**
 * Compresses the theme styles with GZip
 *
 * @author John Ahlroos
 */
class CompressCssTask extends DefaultTask {

    static final String NAME = 'vaadinThemeCompress'
    static final String STYLES_CSS = 'styles.css'

    CompileThemeConfiguration configuration

    /**
     * Create a CSS compression task
     */
    CompressCssTask() {
        description = 'Compresses the theme with GZip'
        configuration = Util.findOrCreateExtension(project, CompileThemeConfiguration)
        onlyIf = { configuration.compress }
        dependsOn(CompileThemeTask.NAME)
        project.afterEvaluate {
            File themesDir = Util.getThemesDirectory(project)
            FileTree themes = project.fileTree(dir:themesDir,
                    include:CompileThemeTask.STYLES_SCSS_PATTERN)
            themes.each { File theme ->
                File dir = new File(theme.parent)
                inputs.file new File(dir, STYLES_CSS)
                outputs.file new File(dir, 'styles.css.gz')
            }
        }
    }

    /**
     * Executes the Gzip compression on the remaining styles.css file. Must be executed after the theme is compiled
     */
    @TaskAction
    def run() {
        compress(project)
    }

    /**
     * Compresses the compiled CSS theme
     *
     * @param project
     *      the project
     * @param isRecompress
     *      are we re-compressing on-the-fly
     */
    static compress(Project project, boolean isRecompress=false) {
        File themesDir = Util.getThemesDirectory(project)
        FileTree themes = project.fileTree(dir: themesDir, include: CompileThemeTask.STYLES_SCSS_PATTERN)
        themes.each { File theme ->
            File dir = new File(theme.parent)
            File stylesCss = new File(dir, STYLES_CSS)
            if (stylesCss.exists()) {
                if(isRecompress) {
                    project.logger.lifecycle("Recompressing $stylesCss.canonicalPath...")
                } else {
                    project.logger.info("Compressing $stylesCss.canonicalPath...")
                }

                long start = System.currentTimeMillis()

                project.ant.gzip(src: stylesCss.canonicalPath, destfile: "${stylesCss.canonicalPath}.gz")

                long time = (System.currentTimeMillis()-start)/1000
                if ( isRecompress ) {
                    project.logger.lifecycle("Theme was recompressed in $time seconds")
                } else {
                    project.logger.info("Theme was compressed in $time seconds")
                }
            } else {
                project.logger.warn("Failed to find $theme pre-compiled styles.css file.")
            }
        }
    }
}

