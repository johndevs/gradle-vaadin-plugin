package fi.jasoft.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.plugins.JavaPluginConvention; 
import fi.jasoft.plugin.ui.TemplateUtil;

class CreateProjectTask extends DefaultTask {

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

