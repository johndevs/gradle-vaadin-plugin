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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import org.gradle.tooling.BuildActionFailureException

import java.util.jar.Attributes
import java.util.jar.JarInputStream

class CompileThemeTask extends DefaultTask {

    public static final String NAME = 'vaadinCompileThemes'

    public CompileThemeTask() {
        dependsOn('classes', BuildClassPathJar.NAME, UpdateAddonStylesTask.NAME)
        description = "Compiles a Vaadin SASS theme into CSS"

        project.afterEvaluate {
            def themesDirectory = Util.getThemesDirectory(project)
            inputs.dir themesDirectory
            inputs.files(project.fileTree(dir: themesDirectory, include: '**/*.scss').collect())
            outputs.files(project.fileTree(dir: themesDirectory, include: '**/styles.scss').collect {
                File theme -> new File(new File(theme.parent).canonicalPath + '/styles.css')
            })

            // Add classpath jar
            if(project.vaadin.plugin.useClassPathJar) {
                BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
                inputs.file(pathJarTask.archivePath)
            }
        }
    }

    @TaskAction
    public void exec() {
        compile(project)
    }

    /**
     * Compiles the SASS themes into CSS
     *
     * @param project
     *      the project who's theme should be compiled
     * @param isRecompile
     *      is the compile a recompile
     */
    static compile(Project project, boolean isRecompile=false) {
        File themesDir = Util.getThemesDirectory(project)

        project.logger.info("Compiling themes found in "+themesDir)

        FileTree themes = project.fileTree(dir: themesDir, include: '**/styles.scss')

        project.logger.info("Found ${themes.files.size() } themes.")

        File gemsDir
        File unpackedThemesDir
        if (project.vaadin.plugin.themeCompiler == 'compass'){
            gemsDir = installCompassGem(project)
            unpackedThemesDir = unpackThemes(project)
        }

        themes.each { File theme ->
            File dir = new File(theme.parent)

            if(isRecompile){
                project.logger.lifecycle("Recompiling " + theme.canonicalPath + "...")
            } else {
                project.logger.info("Compiling " + theme.canonicalPath + "...")
            }

            def start = System.currentTimeMillis()

            def Process process
            switch (project.vaadin.plugin.themeCompiler){
                case 'vaadin':
                    process = executeVaadinSassCompiler(project, theme.canonicalPath, dir.canonicalPath + '/styles.css')
                    break
                case 'compass':
                    process = executeCompassSassCompiler(project, gemsDir, unpackedThemesDir, dir)
                    break
                default:
                    throw new BuildActionFailureException("Selected theme compiler \"${project.vaadin.plugin.themeCompiler}\" is not valid",null)
            }

            boolean failed = false
            Util.logProcess(project, process, 'theme-compile.log', { String line ->
                if(line.contains('error')){
                    project.logger.error(line)
                    failed = true
                }
            })

            process.waitFor()

            if(failed){
                throw new BuildActionFailureException('Theme compilation failed. See error log for details.', null)
            } else if(isRecompile){
                project.logger.lifecycle('Theme was recompiled in '+ (System.currentTimeMillis()-start)/1000+' seconds')
            } else {
                project.logger.info('Theme was compiled in '+ (System.currentTimeMillis()-start)/1000+' seconds')
            }
        }
    }

    /**
     * Creates a process that runs the Vaadin SASS compiler
     *
     * @param project
     *      the project to compile the SASS themes for
     * @param themePath
     *      the path of the theme
     * @param targetCSSFile
     *      the CSS file to compile into
     * @return
     *      the process that runs the compiler
     */
    static Process executeVaadinSassCompiler(Project project, String themePath, String targetCSSFile){
        def compileProcess = [Util.getJavaBinary(project)]
        compileProcess += ['-cp',  Util.getCompileClassPathOrJar(project).asPath]
        compileProcess += 'com.vaadin.sass.SassCompiler'
        compileProcess += [themePath, targetCSSFile]
        compileProcess.execute()
    }

    /**
     * Installs the compass gem with JRuby into a directory
     *
     * @param project
     *      the project to install to
     * @return
     *      the directory where the gem was installed
     */
    static File installCompassGem(Project project){
        def gemsDir = project.file("$project.buildDir/jruby/gems")
        if(!gemsDir.exists()){
            gemsDir.mkdirs()

            project.logger.info("Installing compass ruby gem...")
            def gemProcess = [Util.getJavaBinary(project)]
            gemProcess += ['-cp',  Util.getCompileClassPathOrJar(project).asPath]
            gemProcess += 'org.jruby.Main'
            gemProcess += "-S gem install -i $gemsDir --no-rdoc --no-ri compass".tokenize()

            project.logger.debug(gemProcess.toString())
            gemProcess = gemProcess.execute([
                    "GEM_PATH=${gemsDir.canonicalPath}",
                    "PATH=${gemsDir.canonicalPath}/bin"
            ], null)

            Util.logProcess(project, gemProcess, 'compass-gem-install.log')
            def result = gemProcess.waitFor()
            if(result != 0){
                throw new BuildActionFailureException("Installing Compass ruby gem failed. See compass-gem-install.log for further information.",null)
            }
        }
        gemsDir
    }

    /**
     * Unpacks the themes found on classpath into a temporary directory.
     *
     * @param project
     *      the project where to search fro the themes
     * @return
     *      returns the directory where the themes has been unpacked
     */
    static File unpackThemes(Project project) {

        // Unpack Vaadin and addon themes
        def unpackedThemesDir = project.file("$project.buildDir/themes")
        unpackedThemesDir.mkdirs()

        project.logger.info("Unpacking themes to $unpackedThemesDir")
        def themesAttribute = new Attributes.Name('Vaadin-Stylesheets')
        def bundleName = new Attributes.Name('Bundle-Name')
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if(dependency instanceof ProjectDependency) {
                    def dependentProject = dependency.dependencyProject
                    if(dependentProject.hasProperty('vaadin')){
                        dependentProject.copy{
                            from Util.getThemesDirectory(project)
                            into unpackedThemesDir
                        }
                    }
                } else {
                    conf.files(dependency).each { File file ->
                        file.withInputStream { InputStream stream ->
                            def jarStream = new JarInputStream(stream)
                            def mf = jarStream.getManifest()
                            def attributes = mf?.mainAttributes
                            if (attributes?.getValue(themesAttribute)
                                    || attributes?.getValue(bundleName) == 'vaadin-themes' ) {
                                project.logger.info("Unpacking $file")
                                project.copy {
                                    includeEmptyDirs = false
                                    from project.zipTree(file)
                                    into unpackedThemesDir
                                    include 'VAADIN/themes/**/*'
                                    eachFile { details ->
                                        details.path -= 'VAADIN/themes/'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Copy project theme into unpacked directory
        project.logger.info "Copying project theme into $unpackedThemesDir"
        project.copy{
            from Util.getThemesDirectory(project)
            into unpackedThemesDir
        }

        unpackedThemesDir
    }

    /**
     * Creates a process that runs the Compass compiler with JRuby
     *
     * @param project
     *      the project to run the compiler on
     * @param gemsDir
     *      the gem directory which contains the Compass Ruby gem
     * @param unpackedThemesDir
     *      the directory where themes are unpacked
     * @param themeDir
     *      the target directory
     * @return
     *      the process that runs the compiler
     */
    static Process executeCompassSassCompiler(Project project, File gemsDir, File unpackedThemesDir, File themeDir){
        def themePath = new File(unpackedThemesDir, themeDir.name)

        project.logger.info("Compiling $themePath with compass compiler")
        def compileProcess = [Util.getJavaBinary(project)]
        compileProcess += ['-cp',  Util.getCompileClassPathOrJar(project).asPath]
        compileProcess += 'org.jruby.Main'
        compileProcess += "-S compass compile --sass-dir $themePath --css-dir $themeDir --images-dir $themePath --javascripts-dir $themePath --relative-assets".tokenize()

        project.logger.debug(compileProcess.toString())
        compileProcess.execute([
                "GEM_PATH=${gemsDir.canonicalPath}",
                "PATH=${gemsDir.canonicalPath}/bin"
        ], null)
    }
}