package fi.jasoft.plugin.ui;

import org.gradle.api.Project
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.jar.Attributes

class TemplateUtil {

    public static  void writeTemplate(String template, File targetDir, Map substitutions){
        writeTemplate(template, targetDir, template, substitutions)
    }

    public static  void writeTemplate(String template, File targetDir, String targetFileName, Map substitutions){
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

    public static void ensureWidgetPresent(Project project){
        File widgetsetFile = new File('src/main/java/'+project.vaadin.widgetset.replaceAll(/\./,'/')+".gwt.xml")
        
        new File(widgetsetFile.parent).mkdirs()
        
        if(!widgetsetFile.exists()){
            widgetsetFile.createNewFile()
        }

        String inherits = ""
        project.configurations.compile.each{
            JarInputStream jarStream = new JarInputStream(it.newDataInputStream());
            Manifest mf = jarStream.getManifest();
            if(mf != null){
                 Attributes attributes = mf.getMainAttributes()
                if(attributes != null){
                    String widgetset = attributes.getValue('Vaadin-Widgetsets')
                    if(widgetset != null && widgetset != 'com.vaadin.terminal.gwt.DefaultWidgetSet' && widgetset != 'com.vaadin.DefaultWidgetSet'){
                        inherits += "\t<inherits name=\"${widgetset}\" />\n"
                    }
                }
            }
        }

        File widgetsetDir = new File(widgetsetFile.parent)
        TemplateUtil.writeTemplate('Widgetset.xml', widgetsetDir, project.vaadin.widgetset.tokenize('.').last()+".gwt.xml", 
                ['%INHERITS%' : inherits, '%WIDGETSET%' : project.vaadin.widgetset, '%SUPERDEVMODE%' : String.valueOf(project.vaadin.superDevModeEnabled)])
    }
}