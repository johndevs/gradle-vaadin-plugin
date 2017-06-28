/*
 * Copyright 2017 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project

/**
 * Utility class for template converstions
 *
 * @author John Ahlroos
 * @since 1.0
 */
class TemplateUtil {

    private static final String DOT = '.'

    /**
     * Write a template based file to a directory
     *
     * @templateFileName
     *      The filename of the template without the .template postfix
     *
     * @targetDir
     *      The directory where the file should be put
     *
     * @targetFileName
     *      The filename of the resulting file. By default the same filename as the template.
     *
     * @substitutions
     *      Map of substitutions in the template. By default none.
     *
     * @removeBlankLines
     *      Should resulting blank lines be removed. By default false.
     *
     * @return
     *      the target file where the template was written
     *
     */
    static File writeTemplate(String templateFileName, File targetDir, String targetFileName = templateFileName,
                         Map substitutions = [:], removeBlankLines = false) {
        URL templateUrl = TemplateUtil.getClassLoader().getResource("templates/${templateFileName}.template")
        if ( !templateUrl ) {
            throw new FileNotFoundException("Could not find template 'templates/${templateFileName}.template'")
        }

        TemplateEngine engine = new SimpleTemplateEngine()
        String content = engine.createTemplate(templateUrl).make(substitutions.withDefault { null })

        if ( removeBlankLines ) {
            content = content.replaceAll("(?m)^[ \t]*\r?\n", "")
        }

        writeTemplateFromString(content, targetDir, targetFileName)
    }

    /**
     * Writes template content to file
     *
     * @param templateContent
     *      the content of the template
     * @param targetDir
     *      the target directory
     * @param targetFileName
     *      the target file name
     * @return
     *      the target file where the template was written
     */
    static File writeTemplateFromString(String templateContent, File targetDir, String targetFileName) {
        File targetFile = new File(targetDir, targetFileName)
        if ( !targetFile.exists() ) {
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }

        if ( !targetFile.canWrite() ) {
            throw new FileNotFoundException("Could not write to target file $targetFile.canonicalPath")
        }

        targetFile.write(templateContent)
        targetFile
    }

    /**
     * Returns files from the public folder in the project. The public folder can either be located in the resources or
     * main source set folders. By default returns all the files, but can be limited to a set of files with a specific
     * postfix (e.g. .css)
     *
     * @project
     *      The project which public folder should be searched in
     *
     * @postfix
     *      The optional postfix (e.g. css)
     */
    static File[] getFilesFromPublicFolder(Project project, String postfix='*') {
        Util.getMainSourceSet(project).srcDirTrees
        .collect { project.fileTree(it.dir)}
        .inject(project.sourceSets.main.resources) {a, b -> a + b}
        .matching { include "**/*/public/**/*.$postfix"}
        .files
    }

    /**
     * Converts a fully qualified class name (com.example.MyClass) to a path (com/example/MyClass) and adds a optional
     * postfix (using ".java" postfix the path becomes com/example/MyClass.java).
     *
     * @param fqn
     *      the fully qualified classname
     * @param postfix
     *      the optional postfix
     * @return
     *      the path statement
     */
    static String convertFQNToFilePath(String fqn, String postfix='') {
        fqn.replace(DOT, File.separator) + postfix
    }

    /**
     * Converts a path statement (com/example/MyClass.java) to a fully qualified class name (com.example.MyClass)
     * @param path
     *      The path statement
     * @param postfix
     *      the postfix (.java for example)
     * @return
     *      the fully qualified class name
     */
    static String convertFilePathToFQN(String path, String postfix) {
        StringUtils.removeEnd(path, postfix).replace(File.separator, DOT)
    }
}