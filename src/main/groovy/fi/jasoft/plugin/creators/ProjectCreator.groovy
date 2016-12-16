package fi.jasoft.plugin.creators

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
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
    private CompileWidgetsetConfiguration widgetsetConfiguration
    private boolean pushSupported = true
    private boolean addonStylesSupported = true
    private boolean groovyProject = false
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
    def makeUIClass() {

        uiSubstitutions[APPLICATION_NAME_KEY] = applicationName
        uiSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage

        // Imports
        if (  pushSupported ) {
            uiImports.add('com.vaadin.annotations.Push')
        }

        if (  addonStylesSupported ) {
            uiImports.add('com.vaadin.annotations.Theme')
        }

        uiSubstitutions['imports'] = uiImports

        // Annotations
        if (  pushSupported ) {
            uiAnnotations.add('Push')
        }

        if (  addonStylesSupported ) {
            if (  groovyProject ) {
                uiAnnotations.add("Theme('${applicationName}')")
            } else {
                uiAnnotations.add("Theme(\"${applicationName}\")")
            }
        }

        uiSubstitutions['annotations'] = uiAnnotations

        if (  groovyProject ) {
            TemplateUtil.writeTemplate("$templateDir/MyUI.groovy",
                    UIDir, "${applicationName}UI.groovy", uiSubstitutions)
        } else {
            TemplateUtil.writeTemplate("$templateDir/MyUI.java",
                    UIDir, "${applicationName}UI.java", uiSubstitutions)
        }
    }

    @PackageScope
    def makeServletClass() {

        servletSubstitutions[APPLICATION_NAME_KEY] = applicationName
        servletSubstitutions[APPLICATION_PACKAGE_KEY] = applicationPackage
        servletSubstitutions['asyncEnabled'] = pushSupported

        Map<String,String> initParams = ['ui' : "$applicationPackage.${applicationName}UI"]

        if (  widgetsetFQN ) {
            if (  widgetsetConfiguration.widgetsetCDN ) {
                initParams.put(WIDGETSET_KEY, "${widgetsetFQN.replaceAll('[^a-zA-Z0-9]+','')}")
            } else {
                initParams.put(WIDGETSET_KEY, "$widgetsetFQN")
            }
        }

        servletSubstitutions['initParams'] = initParams

        if (  groovyProject ) {
            TemplateUtil.writeTemplate("$templateDir/MyServlet.groovy",
                    UIDir, applicationName + 'Servlet.groovy', servletSubstitutions)
        } else {
            TemplateUtil.writeTemplate("$templateDir/MyServlet.java",
                    UIDir, applicationName + 'Servlet.java', servletSubstitutions)
        }
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
