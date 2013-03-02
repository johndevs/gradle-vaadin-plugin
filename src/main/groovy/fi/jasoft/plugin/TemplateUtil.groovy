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
        if(!project.vaadin.manageWidgetset){
            return false;
        }

        // Check source dir if widgetset is present there
        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
        File widgetsetFile = new File(javaDir.canonicalPath + '/'+ project.vaadin.widgetset.replaceAll(/\./,'/')+".gwt.xml")
        
        if(widgetsetFile.exists()){
            updateWidgetset(widgetsetFile, project);
            return false;
        } 

        // Check resource dir if widgetset is present there
        File resourceDir = project.sourceSets.main.resources.srcDirs.iterator().next()
        widgetsetFile = new File(resourceDir.canonicalPath + '/'+ project.vaadin.widgetset.replaceAll(/\./,'/')+".gwt.xml")

        if(widgetsetFile.exists()){
            updateWidgetset(widgetsetFile, project);
            return false;
        }

        // No widgetset detected, create one
        new File(widgetsetFile.parent).mkdirs()
        
        widgetsetFile.createNewFile()
           
        updateWidgetset(widgetsetFile, project);

        return true;
    }

    public static void updateWidgetset(File widgetsetFile, Project project) {
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
        
        String moduleXML = project.vaadin.widgetset.tokenize('.').last()+".gwt.xml"

        String sourcePaths = ""
        for(String path : project.vaadin.gwt.sourcePaths){
            sourcePaths+="\t<source path=\"${path}\" />\n"
        }

        def substitutions = [:]
        substitutions['%INHERITS%'] = inherits
        substitutions['%WIDGETSET%'] = project.vaadin.widgetset
        substitutions['%SUPERDEVMODE%'] = String.valueOf(project.vaadin.devmode.superDevMode)
        substitutions['%USERAGENT%'] = project.vaadin.gwt.userAgent
        substitutions['%SOURCE%'] = sourcePaths

        String name, pkg, filename
        if(project.vaadin.widgetsetGenerator == null){
            name = project.vaadin.widgetset.tokenize('.').last()
            pkg = project.vaadin.widgetset.replaceAll('.'+ name,'') + '.client.ui'
            filename = name + "Generator.java"

        } else {
            name = project.vaadin.widgetsetGenerator.tokenize('.').last()
            pkg = project.vaadin.widgetsetGenerator.replaceAll('.'+ name,'')
            filename = name + ".java"
        }

        File javaDir = project.sourceSets.main.java.srcDirs.iterator().next()
        File f = new File(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./,'/') + '/' + filename)

        if(f.exists() || project.vaadin.widgetsetGenerator != null){

            String generatorString = "\t<generate-with class=\"${pkg}.${filename.replaceAll('.java','')}\">\n" + 
            "\t\t<when-type-assignable class=\"com.vaadin.client.metadata.ConnectorBundleLoader\" />\n" +
            "\t</generate-with>"

            substitutions['%WIDGETSET_GENERATOR%'] = generatorString
        } else {
            substitutions['%WIDGETSET_GENERATOR%'] = ''
        }

        File[] clientSCSS = getClientSCSSFiles(project)
        if(clientSCSS.length > 0){
            String linkerString = "\t<define-linker name=\"scssintegration\" class=\"com.vaadin.sass.linker.SassLinker\" />\n" +
            "\t<add-linker name=\"scssintegration\" />";
            substitutions['%SASS_LINKER%'] = linkerString

            StringBuilder stylesheets = new StringBuilder()
            clientSCSS.each {

                // Get the relative path to the public folder
                String path = it.parent.substring(it.parent.lastIndexOf('public')+'public'.length())
                if(path.startsWith('/')){
                    path = path.substring(1);
                }

                // Convert file name from scss -> css
                filename = it.name.substring(0, it.name.length()-4) + 'css'

                // Recreate relative path to scss file
                path = path+'/'+filename
                if(path.startsWith('/')){
                    path = path.substring(1);
                }

                stylesheets.append("\t<stylesheet src=\"${path}\"/>\n")
            }

            substitutions['%STYLESHEETS%'] = stylesheets.toString()

        } else {
            substitutions['%SASS_LINKER%'] = ''
            substitutions['%STYLESHEETS%'] = ''
        }
        
        if(project.vaadin.version.startsWith('6')){
            TemplateUtil.writeTemplate('Widgetset.xml.vaadin6', widgetsetDir, moduleXML, substitutions)
        } else {
            TemplateUtil.writeTemplate('Widgetset.xml', widgetsetDir, moduleXML, substitutions)
        }     
    }

    public static File[] getClientSCSSFiles(Project project){

        def files = []
        project.sourceSets.main.resources.srcDirs.each {
            project.fileTree(it.absolutePath).include('**/*/public/**/*.scss').each {
                files.add(it)
            }
        }

        project.sourceSets.main.java.srcDirs.each {
            project.fileTree(it.absolutePath).include('**/*/public/**/*.scss').each {
                files.add(it)
            }
        }

        return files;
    }
}