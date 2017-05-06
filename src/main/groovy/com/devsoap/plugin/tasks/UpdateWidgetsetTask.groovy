/*
* Copyright 2017 John Ahlroos
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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util
import com.devsoap.plugin.configuration.CompileWidgetsetConfiguration
import groovy.transform.PackageScope
import groovy.util.logging.Log
import org.apache.commons.lang.StringUtils

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task

import java.util.jar.Attributes
import java.util.jar.JarInputStream


/**
 * Updates the GWT module XML file with correct imports
 *
 * @author John Ahlroos
 */
@Log
class UpdateWidgetsetTask extends DefaultTask {

    public static final String NAME = 'vaadinUpdateWidgetset'

    static final String PUBLIC_FOLDER = 'public'
    static final String CSS_FILE_POSTFIX = 'css'
    static final String SCSS_FILE_POSTFIX = 'scss'
    static final String GWT_MODULE_XML_POSTFIX = '.gwt.xml'
    static final String DEFAULT_WIDGETSET = 'com.vaadin.DefaultWidgetSet'
    static final String DEFAULT_LEGACY_V6_WIDGETSET = 'com.vaadin.terminal.gwt.DefaultWidgetSet'
    static final String DEFAULT_LEGACY_V7_WIDGETSET = 'com.vaadin.v7.Vaadin7WidgetSet'
    static final String DOT = '.'
    static final String JAVA_FILE_POSTFIX = ".java"

    public UpdateWidgetsetTask() {
        description = "Updates the widgetset xml file"
        onlyIf { Task task ->
            task.project.vaadinCompile.manageWidgetset &&
                    !task.project.vaadinCompile.widgetsetCDN &&
                    Util.getWidgetset(task.project)
        }
    }

    @TaskAction
    void run() {
       ensureWidgetPresent(project)
    }

    @PackageScope
    static File ensureWidgetPresent(Project project, String widgetsetFQN=Util.getWidgetset(project)) {
        if (!project.vaadinCompile.manageWidgetset ||
                project.vaadinCompile.widgetsetCDN ||
                !widgetsetFQN) {
            return null
        }

        File widgetsetFile = Util.resolveWidgetsetFile(project)

        if ( !widgetsetFile ) {
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
        substitutions['inherits'] = getInherits(project, configuration)
        substitutions['sourcePaths'] = configuration.sourcePaths
        substitutions['configurationProperties'] = getConfigurationProperties()
        substitutions['properties'] = getGWTProperties(project, configuration)
        substitutions['linkers'] = getLinkers(project)
        substitutions['stylesheets'] = getClientStylesheets(project)
        substitutions['collapsePermutations'] = configuration.collapsePermutations

        String widgetsetGenerator = getWidgetsetGenerator(project, configuration, widgetsetFQN)
        if ( widgetsetGenerator ) {
            substitutions['widgetsetGenerator'] = widgetsetGenerator
        }

        // Write widgetset file
        TemplateUtil.writeTemplate('Widgetset.xml', widgetsetFile.parentFile, widgetsetFile.name, substitutions, true)
    }

    /**
     * Scans the child projects of the given project for GWT inherits
     * @param project
     *      The root project to scan, inherits from this project will *NOT* be included
     * @param scannedProjects
     *      the scanned projects, includes the root project
     * @return
     */
    @PackageScope
    static Set<String> findInheritsInDependencies(Project project, List<Project> scannedProjects = []) {
        Set<String> inherits = []

        // Scan child projects for their source inherits
        if ( scannedProjects.size() > 0 ) {
            inherits.addAll(findInheritsInProject(project))
        }

        scannedProjects << project

        // Scan child projects for their addon inherits
        def attribute = new Attributes.Name('Vaadin-Widgetsets')
        project.configurations.all.each { Configuration conf ->
            conf.allDependencies.each { Dependency dependency ->
                if ( dependency in ProjectDependency ) {
                    Project dependentProject = ((ProjectDependency) dependency).dependencyProject
                    if ( !(dependentProject in scannedProjects) ) {
                        inherits.addAll(findInheritsInDependencies(dependentProject, scannedProjects))
                    }
                } else if (Util.isResolvable(project, conf)) {
                    conf.files(dependency).each { File file ->
                        if ( file.file && file.name.endsWith('.jar') ) {
                            file.withInputStream { InputStream stream ->
                                def jarStream = new JarInputStream(stream)
                                jarStream.with {
                                    def mf = jarStream.getManifest()
                                    def attributes = mf?.mainAttributes
                                    def widgetsetsValue = attributes?.getValue(attribute)
                                    if ( widgetsetsValue && !dependency.name.startsWith('vaadin-client') ) {
                                        List<String> widgetsets = widgetsetsValue?.split(',')?.collect { it.trim() }
                                        widgetsets?.each { String widgetset ->
                                            if ( widgetset != DEFAULT_WIDGETSET &&
                                                    widgetset != DEFAULT_LEGACY_V6_WIDGETSET &&
                                                    widgetset != DEFAULT_LEGACY_V7_WIDGETSET) {
                                                inherits.add(widgetset)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        inherits
    }

    /**
     * Scans the project sources sets and searches for GWT module XML files and converts their paths
     * into inherit statements
     *
     * @param project
     *  the project to scan
     * @return
     *      a list of inherit statements
     */
    private static Set<String> findInheritsInProject(Project project) {
        if ( !project.hasProperty('vaadin') ) {
            return []
        }

        Set<String> inherits = []

        def scan = { File srcDir ->
            if ( srcDir.exists() ) {
                project.fileTree(srcDir.absolutePath)
                        .include("**/*/*$GWT_MODULE_XML_POSTFIX")
                        .each { File file ->
                    if ( file.exists() && file.isFile() ) {
                        def path = file.absolutePath.substring(srcDir.absolutePath.size()+1)
                        def widgetset = TemplateUtil.convertFilePathToFQN(path, GWT_MODULE_XML_POSTFIX)
                        inherits.add(widgetset)
                    }
                }
            }
        }

        Util.getMainSourceSet(project).srcDirs.each(scan)

        project.sourceSets.main.resources.srcDirs.each(scan)

        inherits
    }

    private static Map<String, Object> getGWTProperties(Project project, CompileWidgetsetConfiguration configuration) {
        Map<String, Object> properties = [:]

        def ua = 'gecko1_8,safari'
        if ( !configuration.userAgent ) {
            if ( Util.isOperaUserAgentSupported(project) ) {
                ua += ',opera'
            }
            if ( Util.isIE10UserAgentSupported(project) ) {
                ua += ',ie10'
            }
        } else {
            ua = configuration.userAgent
        }
        properties.put('user.agent', ua)

        if ( configuration.profiler ) {
            properties.put('vaadin.profiler', true)
        }

        if ( !configuration.logging ) {
            properties.put('gwt.logging.enabled', false)
        }
        properties
    }

    private static Set<String> getInherits(Project project, CompileWidgetsetConfiguration configuration) {
        Set<String> inherits
        if(Util.isLegacyVaadin8Project(project)) {
            inherits = [DEFAULT_LEGACY_V7_WIDGETSET]
        } else {
            inherits = [DEFAULT_WIDGETSET]
        }

        // Scan classpath for Vaadin addons and inherit their widgetsets
        inherits.addAll(findInheritsInDependencies(project))

        // Custom inherits
        if ( configuration.extraInherits ) {
            inherits.addAll(configuration.extraInherits)
        }

        inherits
    }

    private static Map<String, Object> getConfigurationProperties() {
        Map<String, Object> configurationProperties = [:]
        configurationProperties['devModeRedirectEnabled'] = true
        configurationProperties
    }

    private static String getWidgetsetGenerator(Project project,
                                                CompileWidgetsetConfiguration configuration,
                                                String widgetsetFQN) {
        String name, pkg, filename
        if ( configuration.widgetsetGenerator == null ) {
            name = widgetsetFQN.tokenize(DOT).last()
            pkg = widgetsetFQN.replace(DOT + name, '')
            filename = name + "Generator.java"

        } else {
            name = configuration.widgetsetGenerator.tokenize(DOT).last()
            pkg = configuration.widgetsetGenerator.replace(DOT + name, '')
            filename = name + JAVA_FILE_POSTFIX
        }

        if ( Util.getMainSourceSet(project).srcDirs.isEmpty() ) {
            throw new GradleException('No source sets was found.')
        }

        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File f = new File(new File(javaDir, TemplateUtil.convertFQNToFilePath(pkg)), filename)
        if ( f.exists() || configuration.widgetsetGenerator != null ) {
            return  "${pkg}.${StringUtils.removeEnd(filename, JAVA_FILE_POSTFIX)}"
        }
        null
    }

    private static Map<String, Object> getLinkers(Project project) {
        Map<String, Object> linkers = [:]

        File[] clientSCSS = TemplateUtil.getFilesFromPublicFolder(project, SCSS_FILE_POSTFIX)
        if ( clientSCSS.length > 0 ) {
            linkers.put('scssintegration', 'com.vaadin.sass.linker.SassLinker')
        }
        linkers
    }

    private static List<String> getClientStylesheets(Project project) {
        List<String> stylesheets = []

        File[] clientSCSS = TemplateUtil.getFilesFromPublicFolder(project, SCSS_FILE_POSTFIX)
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

        stylesheets
    }
}