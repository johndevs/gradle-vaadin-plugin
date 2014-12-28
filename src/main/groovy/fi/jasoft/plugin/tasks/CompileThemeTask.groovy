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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class CompileThemeTask extends DefaultTask {

    public static final String NAME = 'vaadinCompileThemes'

    @InputDirectory
    def themesDirectory = Util.getThemesDirectory(project)

    @InputFiles
    def scssFiles = project.fileTree(dir: themesDirectory, include: '**/*.scss').collect()

    @OutputFiles
    def cssFiles =  project.fileTree(dir: themesDirectory, include: '**/styles.scss').collect { File theme ->
        new File(new File(theme.parent).canonicalPath + '/styles.css')
    }

    public CompileThemeTask() {
        dependsOn project.tasks.classes
        description = "Compiles a Vaadin SASS theme into CSS"
    }

    @TaskAction
    public void exec() {
        compile(project)
    }

    def static compile(Project project, boolean isRecompile=false) {
        File themesDir = Util.getThemesDirectory(project)

        project.logger.info("Compiling themes found in "+themesDir)

        FileTree themes = project.fileTree(dir: themesDir, include: '**/styles.scss')

        project.logger.info("Found ${themes.files.size() } themes.")

        themes.each { File theme ->
            File dir = new File(theme.parent)

            if(isRecompile){
                project.logger.lifecycle("Recompiling " + theme.canonicalPath + "...")
            } else {
                project.logger.info("Compiling " + theme.canonicalPath + "...")
            }

            def start = System.currentTimeMillis()

            def compileProcess = ['java']
            compileProcess += ['-cp',  Util.getCompileClassPath(project).asPath]
            compileProcess += 'com.vaadin.sass.SassCompiler'
            compileProcess += [theme.canonicalPath, dir.canonicalPath + '/styles.css']

            def Process process = compileProcess.execute()

            Util.logProcess(project, process, 'theme-compile.log')

            process.waitFor()

            if(isRecompile){
                project.logger.lifecycle('Theme was recompiled in '+ (System.currentTimeMillis()-start)/1000+' seconds')
            } else {
                project.logger.info('Theme was compiled in '+ (System.currentTimeMillis()-start)/1000+' seconds')
            }
        }
    }
}