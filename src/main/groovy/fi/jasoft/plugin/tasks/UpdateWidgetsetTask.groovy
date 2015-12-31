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
package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable

import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest

class UpdateWidgetsetTask extends DefaultTask {

    public static final String NAME = 'vaadinUpdateWidgetset'

    private static final String PUBLIC_FOLDER = 'public'

    private static final String CSS_FILE_POSTFIX = 'css'

    private static final String SCSS_FILE_POSTFIX = 'scss'

    public UpdateWidgetsetTask() {
        description = "Updates the widgetset xml file"
    }

    @TaskAction
    void run() {
        if (project.vaadin.widgetset) {
            ensureWidgetPresent(project)
        }
    }

    static boolean ensureWidgetPresent(Project project, String widgetsetFQN=project.vaadin.widgetset) {
        if (!project.vaadin.manageWidgetset) {
            return false;
        }

        File widgetsetFile

        // Check source dir if widgetset is present there
        if(!Util.getMainSourceSet(project).srcDirs.isEmpty()){
            File javaDir = Util.getMainSourceSet(project).srcDirs.first()
            widgetsetFile = new File(javaDir.canonicalPath + '/' + widgetsetFQN.replaceAll(/\./, '/') + ".gwt.xml")
            if (widgetsetFile.exists()) {
                updateWidgetset(widgetsetFile, widgetsetFQN, project)
                return false;
            }
        }

        // Check resource dir if widgetset is present there
        if(!project.sourceSets.main.resources.srcDirs.isEmpty()){
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            widgetsetFile = new File(resourceDir.canonicalPath + '/' + widgetsetFQN.replaceAll(/\./, '/') + ".gwt.xml")
            if (widgetsetFile.exists()) {
                updateWidgetset(widgetsetFile, widgetsetFQN, project)
                return false;
            }
        }

        if(widgetsetFile){
            // No widgetset detected, create one
            new File(widgetsetFile.parent).mkdirs()
            widgetsetFile.createNewFile()
            updateWidgetset(widgetsetFile, widgetsetFQN, project)
            return true;
        } else {
            throw new GradleException("No source or resource directory present. Cannot generate widgeset file.")
        }
    }

    private static void updateWidgetset(File widgetsetFile, String widgetsetFQN, Project project) {
        def substitutions = [:]

        def inherits = ['com.vaadin.DefaultWidgetSet']

        // Scan classpath for Vaadin addons and inherit their widgetsets
        Configuration compileConf =  project.configurations.compile
        compileConf.allDependencies.each { Dependency dependency ->
            if(dependency instanceof ProjectDependency){
                def depProject = dependency.dependencyProject
                if(depProject.hasProperty('vaadin')){
                    // A vaadin submodule

                    // Scan in source folder
                    Util.getMainSourceSet(depProject).srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath).include('**/*/*.gwt.xml').each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = StringUtils.removeEnd(path, ".gwt.xml").replaceAll(File.separator, '.')
                            inherits.push(widgetset)
                        }
                    }

                    // Scan in resource folders
                    depProject.sourceSets.main.resources.srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath).include('**/*/*.gwt.xml').each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = StringUtils.removeEnd(path, ".gwt.xml").replaceAll(File.separator, '.')
                            inherits.push(widgetset)
                        }
                    }
                }
            } else {
                compileConf.files(dependency).each {
                    JarInputStream jarStream = new JarInputStream(it.newDataInputStream());
                    Manifest mf = jarStream.getManifest();
                    if (mf != null) {
                        Attributes attributes = mf.getMainAttributes()
                        if (attributes != null) {
                            String widgetsets = attributes.getValue('Vaadin-Widgetsets')
                            if (widgetsets != null) {
                                for (String widgetset : widgetsets.split(",")) {
                                    if (widgetset != 'com.vaadin.terminal.gwt.DefaultWidgetSet'
                                            && widgetset != 'com.vaadin.DefaultWidgetSet') {
                                        inherits.push(widgetset)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom inherits
        if (project.vaadin.gwt.extraInherits != null) {
            inherits.addAll(project.vaadin.gwt.extraInherits)
        }

        substitutions['inherits'] = inherits

        //###################################################################

        substitutions['sourcePaths'] = project.vaadin.gwt.sourcePaths

        //###################################################################

        def configurationProperties = [:]
        configurationProperties['devModeRedirectEnabled'] = true

        substitutions['configurationProperties'] = configurationProperties

        //###################################################################

        def properties = [:]

        def ua = 'ie8,ie9,gecko1_8,safari'
        if (project.vaadin.gwt.userAgent == null) {
            if (Util.isOperaUserAgentSupported(project)) {
                ua += ',opera'
            }
            if (Util.isIE10UserAgentSupported(project)) {
                ua += ',ie10'
            }
        } else {
            ua = project.vaadin.gwt.userAgent
        }
        properties.put('user.agent', ua)

        if (project.vaadin.profiler) {
            properties.put('vaadin.profiler', true)
        }

        if (!project.vaadin.gwt.logging) {
            properties.put('gwt.logging.enabled', false)
        }

        substitutions['properties'] = properties

        //###################################################################

        String name, pkg, filename
        if (project.vaadin.widgetsetGenerator == null) {

            name = widgetsetFQN.tokenize('.').last()
            pkg = widgetsetFQN.replaceAll('.' + name, '') + '.client.ui'
            filename = name + "Generator.java"

        } else {
            name = project.vaadin.widgetsetGenerator.tokenize('.').last()
            pkg = project.vaadin.widgetsetGenerator.replaceAll('.' + name, '')
            filename = name + ".java"
        }

        if(Util.getMainSourceSet(project).srcDirs.isEmpty()){
            throw new GradleException('No source sets was found.')
        }

        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File f = new File(javaDir.canonicalPath + '/' + pkg.replaceAll(/\./, '/') + '/' + filename)

        if (f.exists() || project.vaadin.widgetsetGenerator != null) {
            substitutions['widgetsetGenerator'] = "${pkg}.${filename.replaceAll('.java$', '')}"
        }

        //###################################################################

        def linkers = [:]

        File[] clientSCSS = TemplateUtil.getFilesFromPublicFolder(project, SCSS_FILE_POSTFIX)
        if (clientSCSS.length > 0) {
            linkers.put('scssintegration', 'com.vaadin.sass.linker.SassLinker')
        }

        substitutions['linkers'] = linkers

        //###################################################################

        def stylesheets = []

        clientSCSS.each {

            // Get the relative path to the public folder
            def path = it.parent.substring(it.parent.lastIndexOf(PUBLIC_FOLDER) + PUBLIC_FOLDER.length())
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }

            // Convert file name from scss -> css
            def cssFile = it.name.substring(0, it.name.length() - SCSS_FILE_POSTFIX.length()) + CSS_FILE_POSTFIX
            stylesheets.add(path ? path + File.separator + cssFile : cssFile)
        }

        // Retrive CSS files
        File[] clientCSS = TemplateUtil.getFilesFromPublicFolder(project, CSS_FILE_POSTFIX)
        clientCSS.each {

            // Get the relative path to the public folder
            def path = it.parent.substring(it.parent.lastIndexOf(PUBLIC_FOLDER) + PUBLIC_FOLDER.length())
            if (path.startsWith(File.separator)) {
                path = path.substring(1);
            }

            def scssFile = it.name
            stylesheets.add(path ? path + File.separator + scssFile : scssFile)
        }

        substitutions['stylesheets'] = stylesheets

        //###################################################################

        substitutions['collapsePermutations'] = project.vaadin.gwt.collapsePermutations

        //###################################################################

        // Write widgetset file
        TemplateUtil.writeTemplate('Widgetset.xml', widgetsetFile.parentFile, widgetsetFile.name, substitutions, true)
    }
}