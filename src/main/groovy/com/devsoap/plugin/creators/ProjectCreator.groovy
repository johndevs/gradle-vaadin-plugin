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
package com.devsoap.plugin.creators

import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil
import groovy.transform.Canonical

/**
 * Creates a new Vaadin project using pre-defined templates
 *
 * @author John Ahlroos
 * @since 1.1
 */
@Canonical
class ProjectCreator implements Runnable {

    private static final String APPLICATION_NAME_KEY = 'applicationName'
    private static final String APPLICATION_PACKAGE_KEY = 'applicationPackage'
    private static final String WIDGETSET_KEY = 'widgetset'

    /**
     * The application name
     */
    String applicationName

    /**
     * The application base package
     */
    String applicationPackage

    /**
     * The fully qualified widgetset name. This will be used to determine the application package if no package has been
     * provided. If a package name has been provided then this should use that as the base package.
     */
    String widgetsetFQN

    /**
     * Is push supported in the project. By default true.
     */
    boolean pushSupported = true

    /**
     * Will the project use the Vaadin widgetset CDN. By default false.
     */
    boolean widgetsetCDN = false

    /**
     * The type of project that will be created. By default a Java project.
     */
    ProjectType projectType = ProjectType.JAVA

    /**
     * The java source directory
     */
    File javaDir

    /**
     * The resource source directory
     */
    File resourceDir

    /**
     * The tempalte directory
     */
    String templateDir

    /**
     * import statements added to the UI class
     */
    List<String> uiImports = []

    /**
     * Substitution values in the UI class
     */
    Map<String, String> uiSubstitutions = [:]

    /**
     * Annotations added to the UI class
     */
    List<String> uiAnnotations = []

    /**
     * Substitution values in the Servlet class
     */
    Map<String, String> servletSubstitutions = [:]

    /**
     * Spring boot substitutions
     */
    Map<String,String> bootSubstitutions = [:]

    /**
     * Generate spring boot files
     */
    boolean bootEnabled = false


    /**
     * Creates a new project
     */
    @Override
    void run() {

        makeUIClass()

        makeBeansXML()

        if(bootEnabled) {
            makeSpringBootApplicationClass()
        } else {
            makeServletClass()
        }
    }

    private File makeUIClass() {

        uiSubstitutions[APPLICATION_NAME_KEY] = applicationName
        uiSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage

        // Imports
        if ( pushSupported ) {
            uiImports.add('com.vaadin.annotations.Push')
        }

        uiImports.add('com.vaadin.annotations.Theme')

        if(bootEnabled) {
            uiImports.add('com.vaadin.spring.annotation.SpringUI')
        }

        uiSubstitutions['imports'] = uiImports

        // Annotations
        if ( pushSupported ) {
            uiAnnotations.add('Push')
        }

        if(bootEnabled) {
            uiAnnotations.add('SpringUI')
        }

        switch (projectType) {
            case ProjectType.GROOVY:
                uiAnnotations.add("Theme('${applicationName}')")
                break
            case ProjectType.KOTLIN:
                uiAnnotations.add("Theme(\"${applicationName}\")")
                break
            case ProjectType.JAVA:
                uiAnnotations.add("Theme(\"${applicationName}\")")
        }

        uiSubstitutions['annotations'] = uiAnnotations

        File uiClass
        switch (projectType) {
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate(
                        "$templateDir/MyUI.groovy",
                        UIDir,
                        "${applicationName}UI.groovy",
                        uiSubstitutions)
                uiClass = new File(UIDir, "${applicationName}UI.groovy")
                break

            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate(
                        "$templateDir/MyUI.kt",
                        UIDir,
                        "${applicationName}UI.kt",
                        uiSubstitutions)
                uiClass = new File(UIDir, "${applicationName}UI.kt")
                break

            case ProjectType.JAVA:
                TemplateUtil.writeTemplate(
                        "$templateDir/MyUI.java",
                        UIDir,
                        "${applicationName}UI.java",
                        uiSubstitutions)
                uiClass = new File (UIDir, "${applicationName}UI.java")
        }
        uiClass
    }

    private File makeServletClass() {

        servletSubstitutions[APPLICATION_NAME_KEY] = applicationName
        servletSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage
        servletSubstitutions['asyncEnabled'] = pushSupported

        Map<String,String> initParams = ['ui':"$applicationPackage.${applicationName}UI"]

        if ( widgetsetFQN ) {
            if ( widgetsetCDN ) {
                initParams.put(WIDGETSET_KEY, "${widgetsetFQN.replaceAll('[^a-zA-Z0-9]+', '')}")
            } else {
                initParams.put(WIDGETSET_KEY, "$widgetsetFQN")
            }
        }

        servletSubstitutions['initParams'] = initParams

        File servletClass
        switch (projectType) {
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate("$templateDir/MyServlet.groovy",
                        UIDir, "${applicationName}Servlet.groovy", servletSubstitutions)
                servletClass = new File(UIDir, "${applicationName}Servlet.groovy")
                break
            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate("$templateDir/MyServlet.kt",
                        UIDir, "${applicationName}Servlet.kt", servletSubstitutions)
                servletClass = new File(UIDir, "${applicationName}Servlet.kt")
                break
            case ProjectType.JAVA:
                TemplateUtil.writeTemplate("$templateDir/MyServlet.java",
                        UIDir, "${applicationName}Servlet.java", servletSubstitutions)
                servletClass = new File(UIDir, "${applicationName}Servlet.java")
        }

        servletClass
    }

    private File makeBeansXML() {
        TemplateUtil.writeTemplate("$templateDir/beans.xml", metaInfDir, 'beans.xml')
    }

    private File makeSpringBootApplicationClass() {

        bootSubstitutions[APPLICATION_NAME_KEY] = applicationName
        bootSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage

        File applicationClass
        switch (projectType) {
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate("$templateDir/SpringBootApplication.groovy",
                        UIDir, "${applicationName}Application.groovy", bootSubstitutions)
                applicationClass = new File(UIDir, "${applicationName}Application.groovy")
                break
            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate("$templateDir/SpringBootApplication.kt",
                        UIDir, "${applicationName}Application.kt", bootSubstitutions)
                applicationClass = new File(UIDir, "${applicationName}Application.kt")
                break
            case ProjectType.JAVA:
                TemplateUtil.writeTemplate("$templateDir/SpringBootApplication.java",
                        UIDir, "${applicationName}Application.java", bootSubstitutions)
                applicationClass = new File(UIDir, "${applicationName}Application.java")
        }

        applicationClass
    }

    private File getUIDir() {
        File uidir = new File(javaDir, TemplateUtil.convertFQNToFilePath(applicationPackage))
        uidir.mkdirs()
        uidir
    }

    private File getMetaInfDir() {
        File metaInf = new File(resourceDir, 'META-INF')
        metaInf.mkdirs()
        metaInf
    }
}
