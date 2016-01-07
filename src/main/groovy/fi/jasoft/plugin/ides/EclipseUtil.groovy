package fi.jasoft.plugin.ides

import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.Util
import groovy.transform.PackageScope
import org.apache.maven.BuildFailureException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.plugins.ide.eclipse.model.EclipseWtp

/**
 * Created by john on 6.1.2016.
 */
class EclipseUtil {

    static configureEclipsePlugin(Project project) {
        project.beforeEvaluate { Project p ->
            def plugins = p.plugins
            if (plugins.findPlugin('eclipse') && !plugins.findPlugin('eclipse-wtp')) {
                throw new BuildFailureException("You are using the eclipse plugin which does not support all " +
                        "features of the Vaadin plugin. Please use the eclipse-wtp plugin instead.")
            }
        }

        project.afterEvaluate { Project p ->
            if(p.hasProperty('eclipse')){
                def cp = p.eclipse.classpath
                def wtp = p.eclipse.wtp as EclipseWtp

                // Always download sources
                cp.downloadSources = true

                // Set Eclipse's class output dir
                if (p.vaadin.plugin.eclipseOutputDir == null) {
                    cp.defaultOutputDir = p.sourceSets.main.output.classesDir
                } else {
                    cp.defaultOutputDir = p.file(project.vaadin.plugin.eclipseOutputDir)
                }

                // Configure natures
                def natures = p.eclipse.project.natures
                natures.add(0, 'org.springsource.ide.eclipse.gradle.core.nature')

                // Configure facets
                def facet = wtp.facet
                facet.facets = []
                facet.facet(name: 'jst.web', version: '3.0')
                facet.facet(name: 'jst.java', version: p.sourceCompatibility)
                facet.facet(name: 'com.vaadin.integration.eclipse.core', version: '7.0')
                facet.facet(name: 'java', version: p.sourceCompatibility)
            }
        }
    }

    static void addConfigurationToProject(Project project, String conf){
        project.afterEvaluate { Project p ->
            if(p.hasProperty('eclipse')){
                def cp = p.eclipse.classpath
                cp.plusConfigurations += [p.configurations[conf]]

                def wtp = p.eclipse.wtp as EclipseWtp
                wtp.component.plusConfigurations += [p.configurations[conf]]
            }
        }
    }
}
