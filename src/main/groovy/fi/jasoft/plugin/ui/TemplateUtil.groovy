package fi.jasoft.plugin.ui;

import org.gradle.api.Project;

class TemplateUtil {
	
	public static void writeTemplate(Project project, String template, File targetDir){
		writeTemplate(project, template, targetDir, template)
    }

    public static  void writeTemplate(Project project,  String template, File targetDir, String targetFileName){
    	InputStream templateStream = TemplateUtil.class.getClassLoader().getResourceAsStream("templates/${template}.template")
		if(templateStream == null){
			println "Failed to open template file templates/${template}.template"
			return;
		}

		String content = templateStream.getText().toString()
		content = content.replaceAll("%PACKAGE%", getRootPackagePath(project))
		content = content.replaceAll("%APPLICATION_NAME%", project.vaadin.applicationName)
		
		if(project.vaadin.widgetset != null){
			content = content.replaceAll("%WIDGETSET%", project.vaadin.widgetset)	
		}
		
		File targetFile = new File(targetDir.canonicalPath + '/'+targetFileName)
		targetFile.write(content)
    }

    public static String getRootPackagePath(Project project){
    	return project.vaadin.applicationPackage+'.'+project.vaadin.applicationName.toLowerCase()
    }
}