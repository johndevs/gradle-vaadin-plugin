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

import fi.jasoft.plugin.TemplateUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


public class CreateCompositeTask extends DefaultTask {

    public CreateCompositeTask(){
        description = "Creates a new Vaadin Composite."
    }

    @TaskAction
    public void run(){

        Console console = System.console()
        if(console == null){
            println "Create project task needs a console but could not get one. Quitting..."
            return;
        }

        String componentName = console.readLine('\nComposite Name (MyComposite): ')
        if(componentName == ''){
            componentName = 'MyComposite'
        }

        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
        String componentPackage
        if(project.vaadin.widgetset){
            String widgetsetClass = project.vaadin.widgetset
            String widgetsetPackage = widgetsetClass.substring(0, widgetsetClass.lastIndexOf("."))
            componentPackage = widgetsetPackage+'.'+componentName.toLowerCase();

        } else {
            componentPackage = console.readLine("\nComposite Package (com.example.${componentName.toLowerCase()}): ")
            if(componentPackage == ''){
                componentPackage = "com.example.${componentName.toLowerCase()}"
            }
        }

        File componentDir = new File(javaDir.canonicalPath+'/'+componentPackage.replaceAll(/\./,'/'))

        componentDir.mkdirs()

        def substitutions = [:]
        substitutions['%PACKAGE%'] = componentPackage
        substitutions['%COMPONENT_NAME%'] = componentName

        TemplateUtil.writeTemplate("MyComposite.java", componentDir, componentName+".java", substitutions)
    }
}