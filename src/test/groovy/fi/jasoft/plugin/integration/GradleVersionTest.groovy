package fi.jasoft.plugin.integration

import org.gradle.testkit.runner.GradleRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Smoke test for different gradle versions
 */
@RunWith(Parameterized)
class GradleVersionTest extends IntegrationTest {

    final String gradleVersion

    GradleVersionTest(String gradleVersion) {
        this.gradleVersion = gradleVersion
    }

    @Parameterized.Parameters(name = "Gradle {0}")
    static Collection<String> getGradleVersions() {
        [ '3.0', '3.1', '3.2', '3.3', '3.4']
    }

    @Override
    protected GradleRunner setupRunner(File projectDir = this.projectDir.root) {
        return super.setupRunner(projectDir).withGradleVersion(gradleVersion)
    }

    IntegrationTest setupTest(IntegrationTest test) {
        test.projectDir = projectDir
        test.buildFile = buildFile
        test.settingsFile = settingsFile
        test.startTime = startTime
        test
    }

    @Test void 'Run Project'() {
        setupTest(new RunTaskTest('payara'){
            protected GradleRunner setupRunner(File projectDir) {
                return GradleVersionTest.this.setupRunner(projectDir)
            }
        }).'Run server'()
    }

    @Test void 'Compile Widgetset'() {
        setupTest(new CompileWidgetsetTest(){
            protected GradleRunner setupRunner(File projectDir) {
                return GradleVersionTest.this.setupRunner(projectDir)
            }
        }).'Widgetset defined, manual widgetset detected and compiled'()
    }

    @Test void 'Compile Theme'() {
        setupTest(new CreateThemeTest('vaadin'){
            protected GradleRunner setupRunner(File projectDir) {
                return GradleVersionTest.this.setupRunner(projectDir)
            }
        }).'Create default theme'()
    }

    @Test void 'Ensure dependencies are correct'() {
        setupTest(new ProjectDependenciesTest(){
            protected GradleRunner setupRunner(File projectDir) {
                return GradleVersionTest.this.setupRunner(projectDir)
            }
        }).'Project has Vaadin configurations'()
    }
}
