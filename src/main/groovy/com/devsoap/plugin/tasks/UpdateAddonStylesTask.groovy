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
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

/**
 * Updates the addon.scss file listing with addons styles found in the classpath
 *
 * @author John Ahlroos
 */
class UpdateAddonStylesTask extends DefaultTask {

    static final String NAME = 'vaadinUpdateAddonStyles'

    static final String ADDONS_SCSS_FILE = 'addons.scss'

    UpdateAddonStylesTask() {
        dependsOn('classes', BuildClassPathJar.NAME)
        description = 'Updates the addons.scss file with addon styles.'
        onlyIf { Util.isAddonStylesSupported(project) }

        project.afterEvaluate {

            // Themes dirs
            def themesDir = Util.getThemesDirectory(project)
            if ( themesDir && themesDir.exists() ) {
                themesDir.eachDir {
                    inputs.dir it
                    outputs.file new File(it, ADDONS_SCSS_FILE)
                }
            }

            // Add classpath jar
            if ( project.vaadin.useClassPathJar ) {
                BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
                inputs.file(pathJarTask.archivePath)
            }
        }
    }

    @TaskAction
    public void run() {
        if ( !Util.isAddonStylesSupported(project) ) {
            return
        }

        File themesDir = Util.getThemesDirectory(project)
        themesDir.mkdirs()
        themesDir.eachDir {

            File addonsScss = new File(it, ADDONS_SCSS_FILE)

            project.logger.info("Updating $addonsScss")

            // Get compile classpath
            FileCollection classpath = Util.getCompileClassPathOrJar(project)

            // SASSAddonImportFileCreator cannot handle classpath jar, so if
            // that is used we need to manually add the addons to the classpath
            // even though they are listen inside the classpath jar
            if ( project.vaadin.useClassPathJar ) {
                Util.findAddonsInProject(project, 'Vaadin-Stylesheets', true).each {
                    classpath += project.files(it.file)
                }
            }

            List<String> importer = [Util.getJavaBinary(project)]
            importer.add('-cp')
            importer.add(classpath.asPath)
            importer.add('com.vaadin.server.themeutils.SASSAddonImportFileCreator')
            importer.add(it.canonicalPath)

            Process process = importer.execute([], project.buildDir)

            Util.logProcess(project, process, 'addon-style-updater.log') { true }

            int result = process.waitFor()
            if ( result != 0 ) {
                project.logger.error("Failed to update $addonsScss. SASS importer returned error code $result")
            }
        }
    }
}
