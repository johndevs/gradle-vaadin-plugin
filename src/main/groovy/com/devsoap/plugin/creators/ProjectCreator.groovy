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
package com.devsoap.plugin.creators

import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil

import groovy.transform.Canonical
import groovy.transform.PackageScope

/**
 * Created by john on 9/12/16.
 */
@Canonical
class ProjectCreator implements Runnable {

    private static final String APPLICATION_NAME_KEY = 'applicationName'
    private static final String APPLICATION_PACKAGE_KEY = 'applicationPackage'
    private static final String WIDGETSET_KEY = 'widgetset'

    private String applicationName
    private String applicationPackage
    private String widgetsetFQN
    private boolean pushSupported = true
    private boolean widgetsetCDN = false
    private boolean addonStylesSupported = true
    private ProjectType projectType = ProjectType.JAVA
    private File javaDir
    private File resourceDir
    private String templateDir
    private List<String> uiImports = []
    private Map<String, String> uiSubstitutions = [:]
    private List<String> uiAnnotations = []
    private Map<String, String> servletSubstitutions = [:]

    @Override
    void run() {

        makeUIClass()

        makeServletClass()

        makeBeansXML()
    }

    @PackageScope
    File makeUIClass() {

        uiSubstitutions[APPLICATION_NAME_KEY] = applicationName
        uiSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage

        // Imports
        if ( pushSupported ) {
            uiImports.add('com.vaadin.annotations.Push')
        }

        if ( addonStylesSupported ) {
            uiImports.add('com.vaadin.annotations.Theme')
        }

        uiSubstitutions['imports'] = uiImports

        // Annotations
        if ( pushSupported ) {
            uiAnnotations.add('Push')
        }

        if ( addonStylesSupported ) {
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

    @PackageScope
    File makeServletClass() {

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

    @PackageScope
    File makeBeansXML() {
        TemplateUtil.writeTemplate("$templateDir/beans.xml", metaInfDir, 'beans.xml')
    }

    @PackageScope
    File getUIDir() {
        File uidir = new File(javaDir, TemplateUtil.convertFQNToFilePath(applicationPackage))
        uidir.mkdirs()
        uidir
    }

    @PackageScope
    File getMetaInfDir() {
        File metaInf = new File(resourceDir, 'META-INF')
        metaInf.mkdirs()
        metaInf
    }
}
