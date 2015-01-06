package fi.jasoft.plugin.integration

import fi.jasoft.plugin.GradleVaadinPlugin
import fi.jasoft.plugin.TaskListener
import fi.jasoft.plugin.tasks.CreateDirectoryZipTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.internal.changedetection.rules.TaskStateChanges
import org.gradle.api.internal.tasks.TaskStateInternal
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
import org.gradle.plugins.ide.eclipse.model.EclipseWtp
import org.gradle.plugins.ide.eclipse.model.EclipseWtpFacet
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.jar.Manifest

import static fi.jasoft.plugin.DependencyListener.Configuration.*
import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertNull
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Created by john on 1/6/15.
 */
class TaskConfigurationsTest {

    @Rule
    public final TemporaryFolder projectDir = new TemporaryFolder()

    def Project project

    @Before void setup(){
        projectDir.create()

        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build().with { project ->
            apply plugin: GradleVaadinPlugin
            apply plugin: 'eclipse-wtp'
            apply plugin: 'idea'
            evaluate()
            project
        }
    }

    def runTask(String task) {
        def listener = new TaskListener(project)
        listener.beforeExecute(project.tasks[task])
        listener.afterExecute(project.tasks[task], new TaskStateInternal())
    }

    @Test void 'Eclipse default configuration'() {

        runTask('eclipseClasspath')

        def classpath = project.eclipse.classpath

        assertTrue classpath.downloadSources

        assertNull project.vaadin.plugin.eclipseOutputDir
        assertEquals(classpath.defaultOutputDir, project.sourceSets.main.output.classesDir)

        def confs = project.configurations
        assertTrue confs.getByName(SERVER.caption) in classpath.plusConfigurations
        assertTrue confs.getByName(CLIENT.caption) in classpath.plusConfigurations
        assertTrue confs.getByName(JETTY9.caption) in classpath.plusConfigurations

        def natures = project.eclipse.project.natures
        assertTrue 'org.springsource.ide.eclipse.gradle.core.nature' in natures
    }

    @Test void 'Eclipse configuration with custom output dir'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin
            apply plugin: 'eclipse-wtp'

            vaadin {
                plugin {
                    eclipseOutputDir 'custom/dir'
                }
            }

            evaluate()
            project
        }

        runTask('eclipseClasspath')

        def classpath = project.eclipse.classpath
        assertEquals(classpath.defaultOutputDir, project.file('custom/dir'))
    }

    @Test void 'Eclipse configuration with Testbench enabled'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin
            apply plugin: 'eclipse-wtp'

            vaadin {
                testbench {
                    enabled true
                }
            }

            evaluate()
            project
        }

        runTask('eclipseClasspath')

        def classpath = project.eclipse.classpath
        def confs = project.configurations
        assertTrue confs.getByName(TESTBENCH.caption) in classpath.plusConfigurations
    }

    @Test void 'Eclipse WTP default configuration'() {

        runTask('eclipseWtpComponent')

        def confs = project.configurations
        def server = confs.getByName(SERVER.caption)

        def EclipseWtp wtp = project.eclipse.wtp
        assertTrue server in wtp.component.plusConfigurations

        runTask('eclipseWtpFacet')

        def facets = wtp.facet.facets
        def JavaVersion javaVersion = project.sourceCompatibility
        assertEquals '7.0', facets.find { it.name=='com.vaadin.integration.eclipse.core'}.version
        assertEquals '3.0', facets.find { it.name=='jst.web'}.version
        assertEquals javaVersion.toString(), facets.find { it.name=='java'}.version
    }

    @Test void 'IDEA default configuration'() {

        runTask('ideaModule')

        def conf = project.configurations
        def module = project.idea.module

        assertEquals project.name, module.name
        //assertTrue module.inheritOutputDirs as Boolean
        assertEquals project.sourceSets.main.output.classesDir, module.outputDir
        assertEquals project.sourceSets.test.output.classesDir, module.testOutputDir

        assertTrue module.downloadJavadoc
        assertTrue module.downloadSources

        def scopes = module.scopes
        assertTrue conf.getByName(SERVER.caption) in scopes.COMPILE.plus
        assertTrue conf.getByName(CLIENT.caption) in scopes.COMPILE.plus
        assertTrue conf.getByName(JETTY9.caption) in scopes.PROVIDED.plus
    }

    @Test void 'IDEA configuration with Testbench'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin
            apply plugin: 'idea'

            vaadin {
                testbench {
                    enabled true
                }
            }

            evaluate()
            project
        }

        runTask('ideaModule')

        def conf = project.configurations
        def module = project.idea.module
        def scopes = module.scopes
        assertTrue conf.getByName(TESTBENCH.caption) in scopes.TEST.plus
    }

    @Test void 'IDEA configuration with push'() {
        project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin
            apply plugin: 'idea'

            vaadin {
                push true
            }

            evaluate()
            project
        }

        runTask('ideaModule')

        def conf = project.configurations
        def module = project.idea.module
        def scopes = module.scopes
        assertTrue conf.getByName(PUSH.caption) in scopes.COMPILE.plus
    }

    @Test void 'Update widgetset generator before compile'() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build().with { project ->
            apply plugin: GradleVaadinPlugin

            vaadin {
                widgetset 'com.example.Widgetset'
                widgetsetGenerator 'com.example.WidgetsetGenerator'
            }

            evaluate()
            project
        }

        runTask('compileJava')

        def generatorFile = new File(projectDir.root.canonicalPath + '/src/main/java/com/example/WidgetsetGenerator.java')
        assertTrue generatorFile.exists()
    }

    @Test void 'Addon Metadata'() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.root).build().with { project ->
            apply plugin: GradleVaadinPlugin

            version '1.2.3'

            vaadin {
                widgetset 'com.example.Widgetset'
                addon {
                    title 'test-addon'
                    license 'my-license'
                    author 'test-author'
                }
            }

            evaluate()
            project
        }

        runTask('jar')

        def attributes = project.tasks.jar.manifest.attributes
        assertEquals 'com.example.Widgetset', attributes['Vaadin-Widgetsets']
        assertEquals 'test-addon', attributes['Implementation-Title']
        assertEquals project.version, attributes['Implementation-Version']
        assertEquals 'test-author', attributes['Implementation-Vendor']
        assertEquals 'my-license', attributes['Vaadin-License-Title']
        assertEquals 1, attributes['Vaadin-Package-Version']

        runTask(CreateDirectoryZipTask.NAME)

        def manifestFile = project.file('build/tmp/zip/META-INF/MANIFEST.MF')
        assertTrue manifestFile.exists()

        def manifest = new Manifest()
        manifest.read(new ByteArrayInputStream(manifestFile.text.bytes))

        attributes = manifest.mainAttributes

        assertEquals 'test-addon', attributes.getValue('Implementation-Title')
        assertEquals 'my-license', attributes.getValue('Vaadin-License-Title')
        assertEquals project.version, attributes.getValue('Implementation-Version')
        assertEquals 'test-author', attributes.getValue('Implementation-Vendor')
        //assertEquals "libs/${project.jar.archiveName}", attributes.getValue('Vaadin-Addon')
    }
}
