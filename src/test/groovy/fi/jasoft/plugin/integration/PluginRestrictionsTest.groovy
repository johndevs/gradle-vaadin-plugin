package fi.jasoft.plugin.integration

import fi.jasoft.plugin.GradleVaadinPlugin
import org.gradle.api.ProjectConfigurationException
import org.gradle.plugins.ide.internal.tooling.GradleBuildBuilder
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * Created by john on 1/6/15.
 */
class PluginRestrictionsTest {

    @Test(expected = ProjectConfigurationException)
    void 'No Vaadin 6 support'() {
        def project = ProjectBuilder.builder().build().with { project ->
            apply plugin: GradleVaadinPlugin

            vaadin {
                version '6.8.0'
            }

            evaluate()
            project
        }
    }
}
