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

        def conf = DependencyListener.createConfiguration(project, DependencyListener.Configuration.GROOVY, [
                'org.codehaus.groovy:groovy-all:2.3.4'
        ], project.configurations.compile)

        def sources = project.sourceSets.main
        sources.compileClasspath += [conf]

        def testSources = project.sourceSets.test
        testSources.compileClasspath += [conf]
        testSources.runtimeClasspath += [conf]
    }
}
