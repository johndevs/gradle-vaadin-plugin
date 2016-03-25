/*
* Copyright 2016 John Ahlroos
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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

/**
 * Updates the addon.scss file listing with addons styles found in the classpath
 *
 * @author John Ahlroos
 */
class UpdateAddonStylesTask extends DefaultTask {

    static final String NAME = 'vaadinUpdateAddonStyles'

    UpdateAddonStylesTask() {
        dependsOn('classes', BuildClassPathJar.NAME)
        description = 'Updates the addons.scss file with addon styles.'

        project.afterEvaluate {

            // Themes dirs
            def themesDir = Util.getThemesDirectory(project)
            if(themesDir && themesDir.exists()){
                themesDir.eachDir {
                    inputs.dir it.canonicalPath
                    outputs.file "${it.canonicalPath}/addons.scss"
                }
            }

            // Add classpath jar
            if(project.vaadin.plugin.useClassPathJar) {
                BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
                inputs.file(pathJarTask.archivePath)
            }
        }
    }

    @TaskAction
    public void run() {
        if (!Util.isAddonStylesSupported(project)) {
            return
        }

        def themesDir = Util.getThemesDirectory(project)
        themesDir.mkdirs()
        themesDir.eachDir {
            project.logger.info("Updating ${it.canonicalPath}/addons.scss")

            // Get compile classpath
            FileCollection classpath = Util.getCompileClassPathOrJar(project)

            // SASSAddonImportFileCreator cannot handle classpath jar, so if
            // that is used we need to manually add the addons to the classpath
            // even though they are listen inside the classpath jar
            if(project.vaadin.plugin.useClassPathJar){
                Util.findAddonsInProject(project, 'Vaadin-Stylesheets', true).each {
                    classpath += project.files(it.file)
                }
            }

            def importer = [Util.getJavaBinary(project)]
            importer.add('-cp')
            importer.add(classpath.asPath)
            importer.add('com.vaadin.server.themeutils.SASSAddonImportFileCreator')
            importer.add(it.canonicalPath)

            importer = importer.execute()

            if (importer.waitFor() != 0) {
                project.logger.error("Failed to update ${it.canonicalPath}/addons.scss")
            }
        }
    }
}
