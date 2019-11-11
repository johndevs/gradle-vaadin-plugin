/*
 * Copyright 2018 John Ahlroos
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
package com.devsoap.plugin.tasks

import com.devsoap.plugin.ProjectType
import com.devsoap.plugin.TemplateUtil
import com.devsoap.plugin.Util
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * Creates a new Vaadin Design
 *
 * @author John Ahlroos
 * @since 1.1
 */
class CreateDesignTask extends DefaultTask{

    static final String NAME = 'vaadinCreateDesign'

    private static final String DESIGN_PACKAGE_KEY = 'designPackage'
    private static final String DESIGN_NAME_KEY = 'designName'
    private static final String DESIGN_HTML_FILE = 'Design.html'

    /**
     * The design class name
     */
    @Input
    @Option(option = 'name', description = 'The name of the design')
    String designName = 'BasicView'

    /**
     * The package where the design should be put
     */
    @Input
    @Option(option = 'package', description = 'The package of the design')
    String designPackage = "com.example.${designName.toLowerCase()}"

    /**
     * Should a companion java file be created
     */
    @Input
    @Option(option = 'companionFile', description = 'Create the companion file for the design')
    boolean createCompanionFile = true

    /**
     * Should a companion implementation file be created
     */
    @Input
    @Option(option = 'implementationFile', description = 'Create implementation file for the design')
    boolean createImplementationFile = true

    /**
     * Should we output the templates available to the console instead of creating a design.
     */
    @Input
    @Option(option = 'templates', description =
            'Lists the available templates. Add your templates to .vaadin/designer/templates to use them here.')
    boolean listTemplates = false

    /**
     * The template to use for creating a design
     */
    @Input
    @Option(option = 'template', description = "The selected template to use. Must be included in --templates")
    String template = null

    CreateDesignTask() {
        description = 'Creates a new design file'
    }

    /**
     * Creates a new design or lists the templates
     */
    @TaskAction
    void run() {
        if ( listTemplates ) {
            project.logger.lifecycle("Available templates:")
            templates.each { String name, File file ->
                project.logger.printf("%-30.30s  %-30.30s%n", name, "($file.name)")
            }
            return
        }

        makeDesignFile()

        if ( !template ) {
            // TODO add support for generating companion files for any template

            if ( createCompanionFile ) {
                makeDesignCompanionFile()
            }

            if ( createImplementationFile ) {
                makeDesignImplementationFile()
            }
        }
    }

    private makeDesignFile() {
        File resourcesDir = project.sourceSets.main.resources.srcDirs.first()
        File designDir = new File(resourcesDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        if ( template ) {
            if ( !templates.containsKey(template) ) {
                throw new GradleException("Template with name $template could not be found.")
            }
            TemplateUtil.writeTemplateFromString(templates[template].text, designDir, designName + DESIGN_HTML_FILE)
        } else {
            TemplateUtil.writeTemplate('MyDesign.html', designDir, designName + DESIGN_HTML_FILE)
        }
    }

    private makeDesignCompanionFile() {
        File javaDir = Util.getMainSourceSet(project, true).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        Map substitutions = [:]
        substitutions[DESIGN_PACKAGE_KEY] = designPackage
        substitutions[DESIGN_NAME_KEY] = designName

        TemplateUtil.writeTemplate('MyDesign.java', designDir, designName + 'Design.java', substitutions)
    }

    private makeDesignImplementationFile() {
        File javaDir = Util.getMainSourceSet(project).srcDirs.first()
        File designDir = new File(javaDir, TemplateUtil.convertFQNToFilePath(designPackage))
        designDir.mkdirs()

        Map substitutions = [:]
        substitutions[DESIGN_PACKAGE_KEY] = designPackage
        substitutions[DESIGN_NAME_KEY] = designName

        switch (Util.getProjectType(project)) {
            case ProjectType.JAVA:
                TemplateUtil.writeTemplate('MyDesignImpl.java', designDir,
                        designName + '.java', substitutions)
                break
            case ProjectType.KOTLIN:
                TemplateUtil.writeTemplate('MyDesignImpl.kt', designDir,
                        designName + '.kt', substitutions)
                break
            case ProjectType.GROOVY:
                TemplateUtil.writeTemplate('MyDesignImpl.groovy', designDir,
                        designName + '.groovy', substitutions)
                break
        }

    }

    private Map<String, File> getTemplates() {
        File templatesDir = Paths.get(System.getProperty("user.home"), '.vaadin', 'designer', 'templates').toFile()
        Map templateMap = [:]
        templatesDir.eachFile { File file ->
            if ( !file.isDirectory() && file.name.toLowerCase().endsWith('.html') ) {
                String templateName = file.name.take(file.name.lastIndexOf('.'))
                templateMap[templateName] = file
            }
        }

        templateMap
    }

}
