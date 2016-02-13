package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.TemplateUtil
import fi.jasoft.plugin.Util
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

/**
 * Created by john on 13.2.2016.
 */
class CreateDesignTask extends DefaultTask{

    public static final String NAME = 'vaadinCreateDesign'

    @Option(option = 'name', description = 'The name of the design')
    def designName = 'Basic'

    @Option(option = 'package', description = 'The package of the design')
    def designPackage = "com.example.${designName.toLowerCase()}"

    @Option(option = 'companionFile', description = 'Create the companion file for the design')
    def createCompanionFile = true

    @Option(option = 'implementationFile', description = 'Create implemenation file for the design')
    def createImplementationFile = true

    public CreateDesignTask(){
        description = 'Creates a new design file'
    }

    @TaskAction
    def run(){

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

}
