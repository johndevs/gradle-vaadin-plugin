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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * Creates a new Vaadin Design
 *
 * @author John Ahlroos
 */
class CreateDesignTask extends DefaultTask{

    static final String NAME = 'vaadinCreateDesign'

    static final String DESIGN_PACKAGE_KEY = 'designPackage'
    static final String DESIGN_NAME_KEY = 'designName'
    static final String DESIGN_HTML_FILE = 'Design.html'

    @Option(option = 'name', description = 'The name of the design')
    def String designName = 'Basic'

    @Option(option = 'package', description = 'The package of the design')
    def String designPackage = "com.example.${designName.toLowerCase()}"

    @Option(option = 'companionFile', description = 'Create the companion file for the design')
    def boolean createCompanionFile = true

    @Option(option = 'implementationFile', description = 'Create implemenation file for the design')
    def boolean createImplementationFile = true

    @Option(option = 'templates', description =
            'Lists the available templates. Add your templates to .vaadin/designer/templates to use them here.')
    def boolean listTemplates = false

    @Option(option = 'template', description = "The selected tempalte to use. Must be included in --templates")
    def String template = null

    public CreateDesignTask() {
        description = 'Creates a new design file'
    }

    @TaskAction
    def run() {
        if (  listTemplates ) {
            project.logger.lifecycle("Available templates:")
            templates.each { String name, File file ->
                project.logger.printf("%-30.30s  %-30.30s%n", name, "($file.name)")
            }
            return
        }

        makeDesignFile()

        if ( !template ) {
            // TODO add support for generating companion files for any template

            if (  createCompanionFile ) {
                makeDesignCompanionFile()
            }

            if (  createImplementationFile ) {
                makeDesignImplementationFile()
            }
        }
    }

    @PackageScope
    def makeDesignFile() {
        File resourcesDir = project.sourceSets.main.resources.srcDirs.first()
        File designDir = new File(resourcesDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        if (  template ) {
            if ( !templates.containsKey(template) ) {
                throw new GradleException("Template with name $template could not be found.")
            }
            TemplateUtil.writeTemplateFromString(templates[template].text, designDir, designName + DESIGN_HTML_FILE)
        } else {
            TemplateUtil.writeTemplate('MyDesign.html', designDir, designName + DESIGN_HTML_FILE)
        }
    }

    @PackageScope
    def makeDesignCompanionFile() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        def substitutions = [:]
        substitutions[DESIGN_PACKAGE_KEY] = designPackage
        substitutions[DESIGN_NAME_KEY] = designName

        TemplateUtil.writeTemplate('MyDesign.java', designDir, designName + 'Design.java', substitutions)
    }

    @PackageScope
    def makeDesignImplementationFile() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        def substitutions = [:]
        substitutions[DESIGN_PACKAGE_KEY] = designPackage
        substitutions[DESIGN_NAME_KEY] = designName

        TemplateUtil.writeTemplate('MyDesignImpl.java', designDir, designName + '.java', substitutions)
    }

    @PackageScope
    def Map<String, File> getTemplates() {
        def templatesDir = Paths.get(System.getProperty("user.home"), '.vaadin', 'designer', 'templates').toFile()
        def templateMap = [:]
        templatesDir.eachFile { File file ->
            if ( !file.isDirectory() && file.name.toLowerCase().endsWith('.html') ) {
                def templateName = file.name.take(file.name.lastIndexOf('.'))
                templateMap[templateName] = file
            }
        }

        templateMap
    }

}
