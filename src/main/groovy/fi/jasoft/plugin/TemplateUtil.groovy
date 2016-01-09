/*
* Copyright 2015 John Ahlroos
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

import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project

class TemplateUtil {

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
     */
    static writeTemplate(templateFileName, targetDir, targetFileName = templateFileName, substitutions = [:], removeBlankLines = false) {
        def templateUrl = TemplateUtil.class.getClassLoader().getResource("templates/${templateFileName}.template")
        if (templateUrl == null) {
            throw new FileNotFoundException("Could not find template 'templates/${templateFileName}.template'")
        }

        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(templateUrl).make(substitutions.withDefault { null })
        def content = template.toString()

        if (removeBlankLines) {
            content = content.replaceAll("(?m)^[ \t]*\r?\n", "")
        }

        File targetFile = new File(targetDir.canonicalPath + '/' + targetFileName)
        if (!targetFile.exists()) {
            targetFile.parentFile.mkdirs()
            targetFile.createNewFile()
        }

        if (!targetFile.canWrite()) {
            throw new FileNotFoundException("Could not write to target file " + targetFile.canonicalPath)
        }

        targetFile.write(content)
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
        .collect {project.fileTree(it.dir)}
        .inject(project.sourceSets.main.resources){a, b -> a + b}
        .matching {include "**/*/public/**/*.$postfix"}
        .files
    }
}