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
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

import java.util.jar.Attributes
import java.util.jar.JarFile

class CompileWidgetsetTask extends DefaultTask {

    public static final NAME = 'vaadinCompileWidgetset'

    public CompileWidgetsetTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME, BuildClassPathJar.NAME)
        description = "Compiles Vaadin Addons and components into Javascript."

        project.afterEvaluate {

            /* Monitor changes in dependencies since upgrading a
            * dependency should also trigger a recompile of the widgetset
            */
            inputs.files(project.configurations.compile)

            // Monitor changes in client side classes and resources
            project.sourceSets.main.java.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
            }

            //Monitor changes in resources
            project.sourceSets.main.resources.srcDirs.each {
                inputs.files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
                inputs.files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
            }

            // Add classpath jar
            if(project.vaadin.plugin.useClassPathJar) {
                BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
                inputs.file(pathJarTask.archivePath)
            }

            def webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

            // Widgetset output directory
            def targetDir = new File(webAppDir.canonicalPath, 'VAADIN/widgetsets')
            outputs.dir(targetDir)

            // Unit cache output directory
            def unitCacheDir = new File(webAppDir.canonicalPath, 'VAADIN/gwt-unitCache')
            outputs.dir(unitCacheDir)
        }
    }

    @TaskAction
    public void run() {
        if (project.vaadin.widgetset == null) {
            return;
        }

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        File targetDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        targetDir.mkdirs()

        // Ensure unit cache dir is present so the compiler does not complain
        new File(webAppDir.canonicalPath + '/VAADIN/gwt-unitCache').mkdirs()

        FileCollection classpath

        if(project.vaadin.plugin.useClassPathJar){
            // Add dependencies using the classpath jar
            BuildClassPathJar pathJarTask = project.getTasksByName(BuildClassPathJar.NAME, true).first()
            classpath = project.files(pathJarTask.archivePath)

            classpath += Util.getClientCompilerClassPath(project).filter { File file ->
                if(file.name.endsWith('.jar')){
                    // Add GWT compiler + deps
                    if(file.name.startsWith('vaadin-client') ||
                            file.name.startsWith('vaadin-shared') ||
                            file.name.startsWith('validation-api')){
                        return true
                    }

                    // Addons with client side widgetset
                    JarFile jar = new JarFile(file.absolutePath)
                    Attributes attributes = jar.manifest.mainAttributes
                    return attributes.getValue('Vaadin-Widgetsets')
                }
                true
            }
        } else {
            classpath = Util.getClientCompilerClassPath(project)
        }

        def widgetsetCompileProcess = ['java']

        if (project.vaadin.gwt.jvmArgs) {
            widgetsetCompileProcess += project.vaadin.gwt.jvmArgs as List
        }

        widgetsetCompileProcess += ['-cp',  classpath.getAsPath()]

        widgetsetCompileProcess += 'com.google.gwt.dev.Compiler'

        widgetsetCompileProcess += ['-style', project.vaadin.gwt.style]
        widgetsetCompileProcess += ['-optimize', project.vaadin.gwt.optimize]
        widgetsetCompileProcess += ['-war', targetDir.canonicalPath]
        widgetsetCompileProcess += ['-logLevel', project.vaadin.gwt.logLevel]
        widgetsetCompileProcess += ['-localWorkers', project.vaadin.gwt.localWorkers]

        if (project.vaadin.gwt.draftCompile) {
            widgetsetCompileProcess += '-draftCompile'
        }

        if (project.vaadin.gwt.strict) {
            widgetsetCompileProcess += '-strict'
        }

        if (project.vaadin.gwt.extraArgs) {
            widgetsetCompileProcess += project.vaadin.gwt.extraArgs as List
        }

        widgetsetCompileProcess += project.vaadin.widgetset

        def Process process = widgetsetCompileProcess.execute()
        def failed = false
        Util.logProcess(project, process, 'widgetset-compile.log', { String output ->
            // Monitor log for errors
            if(output.trim().startsWith('[ERROR]')){
                failed = true
            }
        })

        // Block
        process.waitFor()

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
        new File(targetDir.canonicalPath + "/WEB-INF").deleteDir()

        if(failed) {
            // Terminate build
            throw new GradleException('Widgetset failed to compile. See error log above.')
        }
    }
}
