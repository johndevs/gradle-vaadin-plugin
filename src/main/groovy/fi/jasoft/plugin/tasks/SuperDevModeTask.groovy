/*
* Copyright 2013 John Ahlroos
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

import fi.jasoft.plugin.Util;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.WarPluginConvention
import fi.jasoft.plugin.ApplicationServer
import java.lang.Process
import fi.jasoft.plugin.TemplateUtil

class SuperDevModeTask extends DefaultTask  {

    private Process appServerProcess;

    public SuperDevModeTask(){
        dependsOn(project.tasks.classes)
        description = "Run Super Development Mode for easier client widget development."
    }

    @TaskAction
    public void run() {  
    	
        if(!project.vaadin.devmode.superDevMode){
            println "SuperDevMode is a experimental feature and is not enabled for project by default. To enable it set vaadin.superDevModeEnabled to true"
            return;
        }

        ApplicationServer server = new ApplicationServer(project)

        server.start()

        runCodeServer()

        server.terminate()
    }

    private runCodeServer(){

        File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
        File widgetsetsDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')
        widgetsetsDir.mkdirs()
        String widgetset = project.vaadin.widgetset == null ? 'com.vaadin.terminal.gwt.DefaultWidgetSet' : project.vaadin.widgetset

        def classpath = Util.getClassPath(project)

        project.javaexec{
            setMain('com.google.gwt.dev.codeserver.CodeServer')
            setClasspath(classpath)
            setArgs([
                    '-port', 9876,
                    '-workDir', widgetsetsDir.canonicalPath,
                    '-src', javaDir.canonicalPath,
                    widgetset ])
            jvmArgs('-Dgwt.compiler.skip=true')
        }
    }
}