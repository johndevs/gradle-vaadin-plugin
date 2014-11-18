/*
* Copyright 2014 John Ahlroos
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
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class CompileThemeTask extends DefaultTask {

    public static final String NAME = 'vaadinCompileThemes'

    public CompileThemeTask() {
        dependsOn project.tasks.classes

        description = "Compiles a Vaadin SASS theme into CSS"

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        getInputs().files(project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/*.scss'))

        FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
        themes.each { File theme ->
            File dir = new File(theme.parent)
            File css = new File(dir.canonicalPath + '/styles.css')
            getOutputs().files(css)
        }
    }

    @TaskAction
    public void exec() {
        compile(project)
    }

    def static compile(Project project) {
        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        FileTree themes = project.fileTree(dir: webAppDir.canonicalPath + '/VAADIN/themes', include: '**/styles.scss')
        themes.each { File theme ->
            File dir = new File(theme.parent)
            project.logger.info("Compiling " + theme.canonicalPath + "...")

            def start = System.currentTimeMillis()

            def compileProcess = ['java']
            compileProcess += ['-cp',  Util.getClassPath(project).asPath]
            compileProcess += 'com.vaadin.sass.SassCompiler'
            compileProcess += [theme.canonicalPath, dir.canonicalPath + '/styles.css']

            def Process process = compileProcess.execute()

            Util.logProcess(project, process, 'theme-compile.log')

            process.waitFor()

            project.logger.info('Theme was compiled in '+ (System.currentTimeMillis()-start)/1000+' seconds')
        }
    }
}