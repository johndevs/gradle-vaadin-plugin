package fi.jasoft.plugin

import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState

/**
 * Created by john on 7/20/14.
 */
class GroovyDependencyListener implements ProjectEvaluationListener  {

    @Override
    void beforeEvaluate(Project project) {

    }

    @Override
    void afterEvaluate(Project project, ProjectState projectState) {

        if (!project.hasProperty('vaadin-groovy') || !project.vaadin.manageDependencies) {
            return
        }

        def conf = 'vaadin-groovy'
        def dependencies = project.dependencies
        if (!project.configurations.hasProperty(conf)) {
            project.configurations.create(conf)
            dependencies.add(conf, 'org.codehaus.groovy:groovy-all:2.3.4')
        }

        def sources = project.sourceSets.main
        def testSources = project.sourceSets.test

        sources.compileClasspath += [project.configurations[conf]]
        testSources.compileClasspath += [project.configurations[conf]]
        testSources.runtimeClasspath += [project.configurations[conf]]

        // Add groovy libs to war
        project.war.classpath(project.configurations[conf])

    }
}
