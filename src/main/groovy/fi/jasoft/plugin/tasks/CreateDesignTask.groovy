package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import groovy.transform.PackageScope
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Created by john on 13.2.2016.
 */
class CreateDesignTask extends DefaultTask{

    public static final String NAME = 'vaadinCreateDesign'

    @Option(option = 'name', description = 'The name of the design')
    def String designName = 'Basic'

    @Option(option = 'package', description = 'The package of the design')
    def String designPackage = "com.example.${designName.toLowerCase()}"

    @Option(option = 'companionFile', description = 'Create the companion file for the design')
    def boolean createCompanionFile = true

    @Option(option = 'implementationFile', description = 'Create implemenation file for the design')
    def boolean createImplementationFile = true

    @Option(option = 'templates', description = 'Lists the available templates. Add your templates to .vaadin/designer/templates to use them here.')
    def boolean listTemplates = false

    public CreateDesignTask(){
        description = 'Creates a new design file'
    }

    @TaskAction
    def run(){
        if(listTemplates){
            project.logger.lifecycle("Available templates:")
            templates.each { String name, File file ->
                project.logger.printf("%-30.30s  %-30.30s%n", name, "($file.name)")
            }
            return
        }

        createDesignFile()

        if(createCompanionFile){
            createDesignCompanionFile()
        }

        if(createImplementationFile){
            createDesignImplementationFile()
        }
    }

    @PackageScope
    def createDesignFile() {
        File resourcesDir = project.sourceSets.main.resources.srcDirs.first()
        File designDir = new File(resourcesDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        TemplateUtil.writeTemplate('MyDesign.html', designDir, designName + 'Design.html')
    }

    @PackageScope
    def createDesignCompanionFile() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        def substitutions = [:]
        substitutions['designPackage'] = designPackage
        substitutions['designName'] = designName

        TemplateUtil.writeTemplate('MyDesign.java', designDir, designName + 'Design.java', substitutions)
    }

    @PackageScope
    def createDesignImplementationFile() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        def substitutions = [:]
        substitutions['designPackage'] = designPackage
        substitutions['designName'] = designName

        TemplateUtil.writeTemplate('MyDesignImpl.java', designDir, designName + '.java', substitutions)
    }

    @PackageScope
    def Map<String, File> getTemplates() {
        def templatesDir = Paths.get(System.getProperty("user.home"), '.vaadin', 'designer', 'templates').toFile()
        def templateMap = [:]
        templatesDir.eachFile { File file ->
            if(!file.isDirectory() && file.name.toLowerCase().endsWith('.html')){
                def templateName = file.name.take(file.name.lastIndexOf('.'))
                templateMap[templateName] = file
            }
        }

        templateMap
    }

}
