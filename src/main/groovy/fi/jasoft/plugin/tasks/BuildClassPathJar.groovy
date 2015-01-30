package fi.jasoft.plugin.tasks

import org.gradle.api.tasks.bundling.Jar

/**
 * Created by john on 1/30/15.
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
    }
}
