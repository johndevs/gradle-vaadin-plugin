/*
* Copyright 2016 John Ahlroos
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
import groovy.transform.PackageScope
import org.apache.commons.lang.StringUtils

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction

import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.regex.Matcher

class UpdateWidgetsetTask extends DefaultTask {

    public static final String NAME = 'vaadinUpdateWidgetset'

    private static final String PUBLIC_FOLDER = 'public'

    private static final String CSS_FILE_POSTFIX = 'css'

    private static final String SCSS_FILE_POSTFIX = 'scss'

    private static final String GWT_MODULE_XML_POSTFIX = '.gwt.xml'

    private static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'

    private static final String DEFAULT_LEGACY_WIDGETSET = 'com.vaadin.terminal.gwt.DefaultWidgetSet'

    public UpdateWidgetsetTask() {
        description = "Updates the widgetset xml file"
        onlyIf { project.vaadin.manageWidgetset && project.vaadin.widgetset }
    }

    @TaskAction
    void run() {
       ensureWidgetPresent(project)
    }

    static boolean ensureWidgetPresent(Project project, String widgetsetFQN=project.vaadin.widgetset) {
        if (!project.vaadin.manageWidgetset || !widgetsetFQN) {
            return false
        }

        File widgetsetFile

        // Check source dir if widgetset is present there
        if(!Util.getMainSourceSet(project).srcDirs.isEmpty()){
            File javaDir = Util.getMainSourceSet(project).srcDirs.first()
            widgetsetFile = new File(javaDir, convertFQNToFilePath(widgetsetFQN, GWT_MODULE_XML_POSTFIX))
            if (widgetsetFile.exists()) {
                updateWidgetset(widgetsetFile, widgetsetFQN, project)
                return false
            }
        }

        // Check resource dir if widgetset is present there
        if(!project.sourceSets.main.resources.srcDirs.isEmpty()){
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            widgetsetFile = new File(resourceDir, convertFQNToFilePath(widgetsetFQN, GWT_MODULE_XML_POSTFIX))
            if (widgetsetFile.exists()) {
                updateWidgetset(widgetsetFile, widgetsetFQN, project)
                return false
            }
        }

        if(widgetsetFile){
            // No widgetset detected, create one
            widgetsetFile.parentFile.mkdirs()
            widgetsetFile.createNewFile()
            updateWidgetset(widgetsetFile, widgetsetFQN, project)
            return true
        } else {
            throw new GradleException("No source or resource directory present. Cannot generate widgeset file.")
        }
    }

    static String convertFQNToFilePath(String fqn, String postfix){
        fqn.replaceAll(/\./, Matcher.quoteReplacement(File.separator)) + postfix
    }

    static String convertFilePathToFQN(String path, String postfix){
        StringUtils.removeEnd(path, postfix).replaceAll(Matcher.quoteReplacement(File.separator), '.')
    }

    @PackageScope
    static updateWidgetset(File widgetsetFile, String widgetsetFQN, Project project) {
        def substitutions = [:]

        def inherits = [DEFAULT_WIDGETSET]

        // Scan classpath for Vaadin addons and inherit their widgetsets
        Configuration compileConf =  project.configurations.compile
        compileConf.allDependencies.each { Dependency dependency ->
            if(dependency instanceof ProjectDependency){
                def depProject = dependency.dependencyProject
                if(depProject.hasProperty('vaadin')){
                    // A vaadin submodule

                    // Scan in source folder
                    Util.getMainSourceSet(depProject).srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath).include("**/*/*$GWT_MODULE_XML_POSTFIX").each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = convertFilePathToFQN(path, GWT_MODULE_XML_POSTFIX)
                            inherits.push(widgetset)
                        }
                    }

                    // Scan in resource folders
                    depProject.sourceSets.main.resources.srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath).include("**/*/*$GWT_MODULE_XML_POSTFIX").each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = convertFilePathToFQN(path, GWT_MODULE_XML_POSTFIX)
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
                                    if (widgetset != DEFAULT_LEGACY_WIDGETSET
                                            && widgetset != DEFAULT_WIDGETSET) {
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
        File f = new File(new File(javaDir, pkg.replaceAll(/\./, File.separator)), filename)


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
            stylesheets.add(
                    Util.replaceExtension(
                            Util.getRelativePathForFile(PUBLIC_FOLDER, it),
                            SCSS_FILE_POSTFIX,
                            CSS_FILE_POSTFIX
                    )
            )
        }

        File[] clientCSS = TemplateUtil.getFilesFromPublicFolder(project, CSS_FILE_POSTFIX)
        clientCSS.each {
            stylesheets.add(
                    Util.getRelativePathForFile(PUBLIC_FOLDER, it)
            )
        }

        substitutions['stylesheets'] = stylesheets

        //###################################################################

        substitutions['collapsePermutations'] = project.vaadin.gwt.collapsePermutations

        //###################################################################

        // Write widgetset file
        TemplateUtil.writeTemplate('Widgetset.xml', widgetsetFile.parentFile, widgetsetFile.name, substitutions, true)
    }
}