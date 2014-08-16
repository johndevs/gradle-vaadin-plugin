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

import fi.jasoft.plugin.DependencyListener
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class CompileWidgetsetTask extends DefaultTask {

    public static final NAME = 'vaadinCompileWidgetset'

    public CompileWidgetsetTask() {
        dependsOn('classes', UpdateWidgetsetTask.NAME)

        description = "Compiles Vaadin Addons and components into Javascript."

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir

        File targetDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        getOutputs().dir(targetDir)

        File unitCacheDir = new File(webAppDir.canonicalPath + '/VAADIN/gwt-unitCache')
        getOutputs().dir(unitCacheDir)

        /* Monitor changes in dependencies since upgrading a
        * dependency should also trigger a recompile of the widgetset
        */
        getInputs().files(project.configurations.compile)

        // Monitor changes in client side classes and resources
        project.sourceSets.main.java.srcDirs.each {
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
        }

        //Monitor changes in resources
        project.sourceSets.main.resources.srcDirs.each {
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/public/**/*.*'))
            getInputs().files(project.fileTree(it.absolutePath).include('**/*/*.gwt.xml'))
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

        FileCollection classpath = Util.getClassPath(project)

        if(project.vaadin.gwt.gwtSdkFirstInClasspath){
            FileCollection gwtCompilerClasspath = project.configurations[DependencyListener.Configuration.CLIENT.caption()];
            classpath = gwtCompilerClasspath + classpath.minus(gwtCompilerClasspath);
        }

        def widgetsetCompileProcess = ['java']

        if (project.vaadin.gwt.jvmArgs) {
            widgetsetCompileProcess += project.vaadin.gwt.jvmArgs
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
            widgetsetCompileProcess += project.vaadin.gwt.extraArgs
        }

        widgetsetCompileProcess += project.vaadin.widgetset

        def Process process = widgetsetCompileProcess.execute()

        // Logging
        File logDir = project.file('build/logs/')
        logDir.mkdirs()

        if(project.vaadin.plugin.logToConsole){
            process.getInputStream().eachLine { output ->
                if(output.contains("[WARN]")){
                    project.logger.warn(output.replaceAll("\\[WARN\\]",'').trim())
                } else {
                    project.logger.info(output.trim())
                }
            }
            process.getErrorStream().eachLine { output ->
                project.logger.error(output.replaceAll("\\[ERROR\\]",'').trim())
            }
        } else {
            File logFile = new File(logDir.canonicalPath + '/widgetset-compile.log')
            logFile.withWriter { out ->
                process.getInputStream().eachLine { output ->
                    if(output.contains("[WARN]")){
                        out.println "[WARN] "+output.replaceAll("\\[WARN\\]",'').trim()
                    } else {
                        out.println "[INFO] "+output.trim()
                    }
                }
                process.getErrorStream().eachLine { output ->
                    out.println "[ERROR] "+output.replaceAll("\\[ERROR\\]",'').trim()
                }
            }
        }

        //Block
        process.waitFor()

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
        new File(targetDir.canonicalPath + "/WEB-INF").deleteDir()
    }

}
