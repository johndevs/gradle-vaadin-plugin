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

import fi.jasoft.plugin.ApplicationServer
import fi.jasoft.plugin.DependencyListener
import fi.jasoft.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import org.gradle.api.tasks.TaskAction

class SuperDevModeTask extends DefaultTask {

    public static final String NAME = 'vaadinSuperDevMode'

    public SuperDevModeTask() {
        dependsOn(CompileWidgetsetTask.NAME)
        description = "Run Super Development Mode for easier client widget development."
    }

    @TaskAction
    public void run() {

        if (!project.vaadin.devmode.superDevMode) {
            println "SuperDevMode is a experimental feature and is not enabled for project by default. To enable it set vaadin.devmode.superDevMode to true"
            return;
        }

        ApplicationServer server = new ApplicationServer(project)

        server.start()

        if (project.vaadin.debug) {
            Util.openBrowser(project, "http://localhost:${project.vaadin.serverPort}/?superdevmode&debug")
        } else {
            Util.openBrowser(project, "http://localhost:${project.vaadin.serverPort}/?superdevmode")
        }

        runCodeServer()

        server.terminate()
    }

    private runCodeServer() {

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File javaDir = Util.getMainSourceSet(project).srcDirs.iterator().next()
        File widgetsetsDir = new File(webAppDir.canonicalPath + '/VAADIN/widgetsets')
        widgetsetsDir.mkdirs()
        String widgetset = project.vaadin.widgetset == null ? 'com.vaadin.terminal.gwt.DefaultWidgetSet' : project.vaadin.widgetset

        def jettyClasspath = project.configurations[DependencyListener.Configuration.JETTY8.caption()];
        def classpath = jettyClasspath + Util.getClassPath(project)

        if(project.vaadin.gwt.gwtSdkFirstInClasspath){
            FileCollection gwtCompilerClasspath = project.configurations[DependencyListener.Configuration.CLIENT.caption()];
            classpath = jettyClasspath + gwtCompilerClasspath + classpath.minus(gwtCompilerClasspath+jettyClasspath);
        }

        project.javaexec {
            setMain('com.google.gwt.dev.codeserver.CodeServer')
            setClasspath(classpath)
            setArgs([
                    '-bindAddress', project.vaadin.devmode.bindAddress,
                    '-port', 9876,
                    '-workDir', widgetsetsDir.canonicalPath,
                    '-src', javaDir.canonicalPath,
                    '-logLevel', project.vaadin.gwt.logLevel,
                    '-noprecompile',
                    widgetset]
            )
        }
    }
}