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
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
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

/**
 * Updates the GWT module XML file with correct imports
 *
 * @author John Ahlroos
 */
class UpdateWidgetsetTask extends DefaultTask {

    public static final String NAME = 'vaadinUpdateWidgetset'

    static final String PUBLIC_FOLDER = 'public'
    static final String CSS_FILE_POSTFIX = 'css'
    static final String SCSS_FILE_POSTFIX = 'scss'
    static final String GWT_MODULE_XML_POSTFIX = '.gwt.xml'
    static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'
    static final String DEFAULT_LEGACY_WIDGETSET = 'com.vaadin.terminal.gwt.DefaultWidgetSet'
    static final String DOT = '.'
    static final String JAVA_FILE_POSTFIX = ".java"

    public UpdateWidgetsetTask() {
        description = "Updates the widgetset xml file"
        onlyIf { project.vaadinCompile.manageWidgetset && Util.getWidgetset(project) }
    }

    @TaskAction
    void run() {
       ensureWidgetPresent(project)
    }

    @PackageScope
    static File ensureWidgetPresent(Project project, String widgetsetFQN=Util.getWidgetset(project)) {
        if (!project.vaadinCompile.manageWidgetset || !widgetsetFQN) {
            return null
        }

        File widgetsetFile = Util.resolveWidgetsetFile(project)

        if(!widgetsetFile){
            // No widgetset file detected, create one
            File resourceDir = project.sourceSets.main.resources.srcDirs.first()
            widgetsetFile = new File(resourceDir,
                    TemplateUtil.convertFQNToFilePath(widgetsetFQN, GWT_MODULE_XML_POSTFIX))
            widgetsetFile.parentFile.mkdirs()
            widgetsetFile.createNewFile()
        }

        updateWidgetset(widgetsetFile, widgetsetFQN, project)
        widgetsetFile
    }

    @PackageScope
    static updateWidgetset(File widgetsetFile, String widgetsetFQN, Project project) {
        def configuration = project.vaadinCompile as CompileWidgetsetConfiguration

        def substitutions = [:]

        def inherits = [DEFAULT_WIDGETSET]

        // Scan classpath for Vaadin addons and inherit their widgetsets
        Configuration compileConf =  project.configurations.compile
        compileConf.allDependencies.each { Dependency dependency ->
            if(dependency in ProjectDependency){
                def depProject = dependency.dependencyProject
                if(depProject.hasProperty('vaadin')){
                    // A vaadin submodule

                    // Scan in source folder
                    Util.getMainSourceSet(depProject).srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath)
                                .include("**/*/*$GWT_MODULE_XML_POSTFIX")
                                .each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = TemplateUtil.convertFilePathToFQN(path, GWT_MODULE_XML_POSTFIX)
                            inherits.push(widgetset)
                        }
                    }

                    // Scan in resource folders
                    depProject.sourceSets.main.resources.srcDirs.each { File srcDir ->
                        depProject.fileTree(srcDir.absolutePath)
                                .include("**/*/*$GWT_MODULE_XML_POSTFIX")
                                .each { File file ->
                            def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                            def widgetset = TemplateUtil.convertFilePathToFQN(path, GWT_MODULE_XML_POSTFIX)
                            inherits.push(widgetset)
                        }
                    }
                }
            } else {
                compileConf.files(dependency).each {
                    JarInputStream jarStream = new JarInputStream(it.newDataInputStream());
                    jarStream.withStream {
                        Manifest mf = jarStream.manifest
                        Attributes attributes = mf?.mainAttributes
                        String widgetsetsValue = attributes?.getValue('Vaadin-Widgetsets')
                        List<String> widgetsets = widgetsetsValue?.split(',')?.collect { it.trim() }
                        widgetsets?.each { String widgetset ->
                            if(widgetset != DEFAULT_WIDGETSET && widgetset != DEFAULT_LEGACY_WIDGETSET){
                                inherits.push(widgetset)
                            }
                        }
                    }
                }
            }
        }

        // Custom inherits
        if (configuration.extraInherits) {
            inherits.addAll(configuration.extraInherits)
        }

        substitutions['inherits'] = inherits

        //###################################################################

        substitutions['sourcePaths'] = configuration.sourcePaths

        //###################################################################

        def configurationProperties = [:]
        configurationProperties['devModeRedirectEnabled'] = true

        substitutions['configurationProperties'] = configurationProperties

        //###################################################################

        def properties = [:]

        def ua = 'ie8,ie9,gecko1_8,safari'
        if (!configuration.userAgent) {
            if (Util.isOperaUserAgentSupported(project)) {
                ua += ',opera'
            }
            if (Util.isIE10UserAgentSupported(project)) {
                ua += ',ie10'
            }
        } else {
            ua = configuration.userAgent
        }
        properties.put('user.agent', ua)

        if (configuration.profiler) {
            properties.put('vaadin.profiler', true)
        }

        if (!configuration.logging) {
            properties.put('gwt.logging.enabled', false)
        }

        substitutions['properties'] = properties

        //###################################################################

        String name, pkg, filename
        if (configuration.widgetsetGenerator == null) {

            name = widgetsetFQN.tokenize(DOT).last()
            pkg = widgetsetFQN.replace(DOT + name, '') + '.client.ui'
            filename = name + "Generator.java"

        } else {
            name = configuration.widgetsetGenerator.tokenize(DOT).last()
            pkg = configuration.widgetsetGenerator.replace(DOT + name, '')
            filename = name + JAVA_FILE_POSTFIX
        }

        if(Util.getMainSourceSet(project).srcDirs.isEmpty()){
            throw new GradleException('No source sets was found.')
        }

        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File f = new File(new File(javaDir, TemplateUtil.convertFQNToFilePath(pkg)), filename)
        if (f.exists() || configuration.widgetsetGenerator != null) {
            substitutions['widgetsetGenerator'] = "${pkg}.${StringUtils.removeEnd(filename, JAVA_FILE_POSTFIX)}"
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

        substitutions['collapsePermutations'] = configuration.collapsePermutations

        //###################################################################

        // Write widgetset file
        TemplateUtil.writeTemplate('Widgetset.xml', widgetsetFile.parentFile, widgetsetFile.name, substitutions, true)
    }
}