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

import org.gradle.api.Project
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.jar.Attributes

class TemplateUtil {

    public static void writeTemplate(String template, File targetDir, String targetFileName){
        writeTemplate(template, targetDir, targetFileName, [:])
    }

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

    public static boolean ensureWidgetPresent(Project project){
        boolean result = false;

        if(!project.vaadin.manageWidgetset){
            return false;
        }

        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
        File widgetsetFile = new File(javaDir.canonicalPath + '/'+ project.vaadin.widgetset.replaceAll(/\./,'/')+".gwt.xml")
        
        new File(widgetsetFile.parent).mkdirs()
        
        if(!widgetsetFile.exists()){
            widgetsetFile.createNewFile()
            result = true;
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
        if(project.vaadin.version.startsWith('6')){
            TemplateUtil.writeTemplate('Widgetset.xml.vaadin6', 
                widgetsetDir, 
                project.vaadin.widgetset.tokenize('.').last()+".gwt.xml", 
                ['%INHERITS%' : inherits, '%WIDGETSET%' : project.vaadin.widgetset, '%SUPERDEVMODE%' : String.valueOf(project.vaadin.devmode.superDevMode)])
        } else {
            TemplateUtil.writeTemplate('Widgetset.xml',
                 widgetsetDir, 
                 project.vaadin.widgetset.tokenize('.').last()+".gwt.xml", 
                ['%INHERITS%' : inherits, '%WIDGETSET%' : project.vaadin.widgetset, '%SUPERDEVMODE%' : String.valueOf(project.vaadin.devmode.superDevMode)])
        }     

        return result   
    }
}