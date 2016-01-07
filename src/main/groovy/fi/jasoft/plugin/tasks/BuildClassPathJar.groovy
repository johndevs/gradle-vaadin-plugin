package fi.jasoft.plugin.tasks

import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.Util
import org.gradle.api.tasks.bundling.Jar

/**
 * Builds a classpath jar used by external java processes.
 */
class BuildClassPathJar extends Jar {

    public static final String NAME = 'vaadinClassPathJar'

    BuildClassPathJar() {
        description = 'Creates a Jar with the project classpath'
        classifier = 'classpath'
        dependsOn 'classes'

        onlyIf {
            project.vaadin.plugin.useClassPathJar
        }

        project.afterEvaluate{

            def files = project.configurations[GradleVaadinPlugin.CONFIGURATION_RUN_SERVER]
                    .plus(Util.getCompileClassPath(project))
                    .filter { File file ->
                        file.isFile() && file.name.endsWith('.jar')
                    }

            inputs.files(files)

            manifest.attributes('Class-Path': files.collect { File file ->
                file.toURI().toString()
            }.join(' '))
        }
    }
}
