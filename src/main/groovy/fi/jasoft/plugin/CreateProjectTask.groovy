/*
* Copyright 2012 John Ahlroos
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
package fi.jasoft.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.plugins.JavaPluginConvention; 
import fi.jasoft.plugin.ui.TemplateUtil;

class CreateProjectTask extends DefaultTask {

	public CreateProjectTask(){
		description = "Creates a new Vaadin Project."
	}

    @TaskAction
    public void run() {

    	Console console = System.console()
    	if(console == null){
    		println "Create project task needs a console but could not get one. Quitting..."
    		return;
    	}

    	String applicationPackage = console.readLine('\nApplication Package (com.example): ')
    	if(applicationPackage == ''){
    		applicationPackage = 'com.example'
    	}

    	String applicationName = console.readLine('Application Name (MyApplication): ')
    	if(applicationName == ''){
    		applicationName = 'MyApplication'
    	}

		File javaDir = new File('src/main/java/')
		File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
		File uidir = new File(javaDir.canonicalPath + '/' + (applicationPackage+'.'+applicationName.toLowerCase()).replaceAll(/\./,'/'))
		File webinf = new File(webAppDir.canonicalPath + '/WEB-INF')
		
		webAppDir.mkdirs()
		uidir.mkdirs()
		webinf.mkdirs()

		def substitutions = [:]
    	substitutions['%PACKAGE%'] = applicationPackage+'.'+applicationName.toLowerCase()
    	substitutions['%APPLICATION_NAME%'] = applicationName
    	substitutions['%INHERITS%'] = ""
		
		if(project.vaadin.version.startsWith("6")){
			TemplateUtil.writeTemplate("MyApplication.java", uidir, applicationName+".java", substitutions)
			if(project.vaadin.widgetset == null){
				TemplateUtil.writeTemplate("web.xml.vaadin6", webinf, "web.xml", substitutions)	
			} else {
				substitutions['%WIDGETSET%'] = project.vaadin.widgetset
				TemplateUtil.writeTemplate("web.xml.vaadin6.widgetset", webinf, "web.xml", substitutions)	
				TemplateUtil.ensureWidgetPresent(project)
			}
			
		} else {
			TemplateUtil.writeTemplate('MyUI.java', uidir, applicationName+"UI.java", substitutions)
			if(project.vaadin.widgetset == null){
				TemplateUtil.writeTemplate('web.xml', webinf, substitutions) 
			} else {
				substitutions['%WIDGETSET%'] = project.vaadin.widgetset
				TemplateUtil.writeTemplate('web.xml.widgetset', webinf, "web.xml", substitutions) 
				TemplateUtil.ensureWidgetPresent(project)
			}
		}
    }
}

