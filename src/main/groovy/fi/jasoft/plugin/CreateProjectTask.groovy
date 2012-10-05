package fi.jasoft.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.plugins.JavaPluginConvention; 
import fi.jasoft.plugin.ui.TemplateUtil;

class CreateProjectTask extends DefaultTask {

    @TaskAction
    public void run() {
		File javaDir = new File('src/main/java/')
		File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
		File uidir = new File(javaDir.canonicalPath + '/' + TemplateUtil.getRootPackagePath(project).replaceAll(/\./,'/'))
		File webinf = new File(webAppDir.canonicalPath + '/WEB-INF')
		
		webAppDir.mkdirs()
		uidir.mkdirs()
		webinf.mkdirs()
		
		if(project.vaadin.version.startsWith("6")){
			TemplateUtil.writeTemplate(project, "MyApplication.java", uidir, project.vaadin.applicationName+".java")
			if(project.vaadin.widgetset == null){
				TemplateUtil.writeTemplate(project, "web.xml.vaadin6", webinf, "web.xml")	
			} else {
				TemplateUtil.writeTemplate(project, "web.xml.vaadin6.widgetset", webinf, "web.xml")	
			}
			
		} else {
			TemplateUtil.writeTemplate(project, 'MyUI.java', uidir, project.vaadin.applicationName+"UI.java")
			if(project.vaadin.widgetset == null){
				TemplateUtil.writeTemplate(project, 'web.xml', webinf) 
			} else {
				TemplateUtil.writeTemplate(project, 'web.xml.widgetset', webinf, "web.xml") 
			}
		}
    }

}
