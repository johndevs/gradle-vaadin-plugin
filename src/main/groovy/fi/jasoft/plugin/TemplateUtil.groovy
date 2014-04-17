/*
* Copyright 2014 John Ahlroos
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
package fi.jasoft.plugin

import groovy.text.SimpleTemplateEngine;
import org.gradle.api.Project
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.jar.Attributes

class TemplateUtil {

    @Deprecated
    protected static String getTemplateContent(String template) {
        if (template == null){
            throw new IllegalArgumentException("Template name cannot be null")
        }

        InputStream templateStream = TemplateUtil.class.getClassLoader().getResourceAsStream("templates/${template}.template")
        if (templateStream == null) {
            throw new FileNotFoundException("The template file "+template+ ".template could not be found.")
        }

        return templateStream.getText()
    }

    @Deprecated
    public static void writeTemplate(String template, File targetDir, Map substitutions) {
        writeTemplate(template, targetDir, template, substitutions)
    }

    @Deprecated
    public static void writeTemplate(String template, File targetDir, String targetFileName=template, Map substitutions=[:], boolean removeBlankLines=false) {
        String content = TemplateUtil.getTemplateContent(template)

        substitutions.each { key, value ->
            content = content.replaceAll(key, value)
        }

        if (removeBlankLines){
            content = content.replaceAll("(?m)^[ \t]*\r?\n", "")
        }

        File targetFile = new File(targetDir.canonicalPath + '/' + targetFileName)
        if(!targetFile.exists()){
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }

        if (!targetFile.canWrite()){
             throw new FileNotFoundException("Could not write to target file "+targetFile.canonicalPath)
        }

        targetFile.write(content)
    }

    public static void writeTemplate2(String templateFileName, File targetDir, String targetFileName=templateFileName, Map substitutions=[:], boolean removeBlankLines=false) {
        def templateUrl = TemplateUtil.class.getClassLoader().getResource("templates/${templateFileName}.template")
        if (templateUrl == null) {
            throw new FileNotFoundException("Could not find template 'templates/${templateFileName}.template'")
        }

        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(templateUrl).make(substitutions.withDefault {null})
        def content = template.toString()

        if (removeBlankLines){
            content = content.replaceAll("(?m)^[ \t]*\r?\n", "")
        }

        File targetFile = new File(targetDir.canonicalPath + '/' + targetFileName)
        if(!targetFile.exists()){
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }

        if (!targetFile.canWrite()){
            throw new FileNotFoundException("Could not write to target file "+targetFile.canonicalPath)
        }

        targetFile.write(content)
    }

    /**
     * Searches for SCSS files in the public folder and returns them
     */
    @Deprecated
    public static File[] getClientSCSSFiles(Project project) {
        return getFilesFromPublicFolder(project, 'scss')
    }

    public static File[] getFilesFromPublicFolder(Project project, String prefix) {

        def files = []
        project.sourceSets.main.resources.srcDirs.each {
            project.fileTree(it.absolutePath).include("**/*/public/**/*.$prefix").each {
                files.add(it)
            }
        }

        Util.getMainSourceSet(project).srcDirs.each {
            project.fileTree(it.absolutePath).include("**/*/public/**/*.$prefix").each {
                files.add(it)
            }
        }

        return files;
    }
}