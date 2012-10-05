package fi.jasoft.plugin.ui;

import org.gradle.api.Project;

class TemplateUtil {
	
	public static void writeTemplate(Project project, String template, File targetDir){
		writeTemplate(project, template, targetDir, template)
    }

    public static  void writeTemplate(Project project,  String template, File targetDir, String targetFileName){
    	def substitutions = [:]
    	substitutions['%PACKAGE%'] = getRootPackagePath(project)
    	substitutions['%APPLICATION_NAME%'] = project.vaadin.applicationName
    	substitutions['%INHERITS%'] = ""

    	if(project.vaadin.widgetset != null){
    		substitutions['%WIDGETSET%'] = project.vaadin.widgetset
    	}

    	writeTemplate(project, template, targetDir, targetFileName, substitutions)
    }

    public static  void writeTemplate(Project project,  String template, File targetDir, String targetFileName, Map substitutions){
    	InputStream templateStream = TemplateUtil.class.getClassLoader().getResourceAsStream("templates/${template}.template")
		if(templateStream == null){
			println "Failed to open template file templates/${template}.template"
			return;
		}

		String content = templateStream.getText().toString()
		substitutions.each { key, value ->
			content = content.replaceAll(key, value)	
		}
		
		File targetFile = new File(targetDir.canonicalPath + '/'+targetFileName)
		targetFile.write(content)
    }

    public static String getRootPackagePath(Project project){
    	return project.vaadin.applicationPackage+'.'+project.vaadin.applicationName.toLowerCase()
    }
}