package com.devsoap.plugin.tests

import com.devsoap.plugin.GradleVaadinPlugin
import com.devsoap.plugin.tasks.CreateProjectTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.nio.file.Paths
import java.util.jar.JarFile

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

/**
 * Created by john on 4/29/17.
 */
@RunWith(Parameterized)
class SpringBootTest extends IntegrationTest {

    private String springBootVersion

    SpringBootTest(String springBootVersion) {
        this.springBootVersion = springBootVersion
    }

    @Parameterized.Parameters(name = "Spring Boot {0}")
    static Collection<String> getSpringBootVersions() {
        [ '1.5.3.RELEASE', '2.0.0.M7']
    }

    @Override
    protected void applyBuildScriptRepositories(File buildFile) {
        super.applyBuildScriptRepositories(buildFile)
        buildFile << """
                maven { url 'https://plugins.gradle.org/m2/'}
                maven { url 'https://repo.spring.io/libs-snapshot'}
        """.stripMargin()
    }

    @Override
    protected void applyBuildScriptClasspathDependencies(File buildFile) {
        super.applyBuildScriptClasspathDependencies(buildFile)
        buildFile << "classpath 'org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}'\n"
    }

    @Override
    protected void applyThirdPartyPlugins(File buildFile) {
        super.applyThirdPartyPlugins(buildFile)
        buildFile << "apply plugin: 'org.springframework.boot'\n"
    }

    @Test void 'Jar is built by default'() {
        def output = runFailureExpected('--info','build')

        assertTrue output.contains(':jar')
        assertFalse output.contains(':war')

        assertTrue output.contains('Applying JavaPluginAction')
        assertTrue output.contains('Applying SpringBootAction')
        assertFalse output.contains('Applying WarPluginAction')
        assertTrue output.contains('Applying VaadinPluginAction')

        assertTrue output.contains('Spring boot present, not applying WAR plugin by default.')
    }

    @Test void 'War is built if applied'() {
        buildFile << "apply plugin: 'war'\n"

        def output = runFailureExpected('--info','build')

        assertTrue output.contains(':war')
        assertFalse output.contains(':jar')

        assertTrue output.contains('Applying JavaPluginAction')
        assertTrue output.contains('Applying SpringBootAction')
        assertTrue output.contains('Applying WarPluginAction')
        assertTrue output.contains('Applying VaadinPluginAction')

        assertTrue output.contains('Spring boot present, not applying WAR plugin by default.')
    }

    @Test void 'Spring Boot Vaadin starter is included'() {
        String dependencyInfo = runWithArguments('dependencyInsight',
                '--configuration', GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT,
                '--dependency', 'vaadin-spring-boot-starter')
        assertTrue dependencyInfo.contains('com.vaadin:vaadin-spring-boot-starter:3.')
    }

    @Test void 'Use custom Spring Boot starter version'() {
        buildFile << "vaadinSpringBoot.starterVersion = '2.0.0'\n"

        String dependencyInfo = runWithArguments('dependencyInsight',
                '--configuration', GradleVaadinPlugin.CONFIGURATION_SPRING_BOOT,
                '--dependency', 'vaadin-spring-boot-starter')
        assertTrue dependencyInfo.contains('com.vaadin:vaadin-spring-boot-starter:2.0.0')
    }

    @Test void 'Validate Spring Boot executable jar'() {
        buildFile << "vaadinCompile.widgetset = 'com.example.springboottest.MyWidgetset'\n"

        configureSpringBootProject()

        JarFile jar = getSpringBootJar()

        // Libs
        assertJarContents(jar, 'vaadin-server not found in jar', 'BOOT-INF/lib/vaadin-server')
        assertJarContents(jar, 'spring-boot-starter not found in jar', 'BOOT-INF/lib/spring-boot-starter')
        assertJarContents(jar, 'vaadin-spring-boot not found in jar', 'BOOT-INF/lib/vaadin-spring-boot')

        // Static resources
        assertJarContents(jar, 'Widgetset not found in jar',
                'BOOT-INF/classes/VAADIN/widgetsets/com.example.springboottest.MyWidgetset/')
        assertJarContents(jar, 'Theme not found in jar', 'BOOT-INF/classes/VAADIN/themes/SpringBootTest/')

        // Classes
        assertJarContents(jar, 'UI not found in jar',
                'BOOT-INF/classes/com/example/springboottest/MyAppUI.class')

        assertJarContents(jar, 'App not found in jar',
                'BOOT-INF/classes/com/example/springboottest/SpringBootApplication.class')
        assertJarContents(jar, 'Spring Boot loader not found in jar', 'org/springframework/boot/loader/')
    }

    @Test void 'Vaadin push dependencies are included'() {
        configureSpringBootProject()
        JarFile jar = getSpringBootJar()
        assertNull 'vaadin-push should not be found in jar',
                jar.entries().find { it.name.startsWith('BOOT-INF/lib/vaadin-push')}

        buildFile << "vaadin.push = true\n"

        jar = getSpringBootJar()
        assertJarContents(jar, 'vaadin-push not found in jar','BOOT-INF/lib/vaadin-push')
    }

    @Test void 'Vaadin compile dependencies are included'() {
        configureSpringBootProject()

        buildFile << """
        dependencies {
            vaadinCompile 'commons-lang:commons-lang:2.6'
        }
        """.stripIndent()

        JarFile jar = getSpringBootJar()
        assertJarContents(jar, 'vaadinCompile dependency not found in jar','BOOT-INF/lib/commons-lang-2.6')
    }

    private static void assertJarContents(JarFile jar, String message, String path) {
        assertNotNull message + "\n" + jar.entries().collect {it.name}.collect().join("\n"),
                jar.entries().find { it.name.startsWith(path)}
    }

    private JarFile getSpringBootJar() {
        runWithArguments('clean', springBoot1 ? 'bootRepackage': 'assemble')

        File jarFile = Paths.get(projectDir.root.canonicalPath,
                'build', 'libs', projectDir.root.name+'.jar').toFile()
        assertTrue 'jar did not exist', jarFile.exists()

        new JarFile(jarFile)
    }

    private void configureSpringBootProject() {
        runWithArguments(CreateProjectTask.NAME, '--name=SpringBootTest')

        File packageDir = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'com', 'example', 'springboottest').toFile()

        File servlet = new File(packageDir, 'SpringBootTestServlet.java')
        servlet.delete()

        File ui = new File(packageDir, 'SpringBootTestUI.java')
        ui.delete()

        File appUI = new File(packageDir, 'MyAppUI.java')
        appUI.text = getClass().getResource('/templates/SpringBootUI.java.template').text

        File app = new File(packageDir, 'SpringBootApplication.java')
        app.text = getClass().getResource('/templates/SpringBootApplication.java.template').text

        if(springBoot1){
            buildFile << "springBoot.mainClass = 'com.example.springboottest.SpringBootApplication'\n"
        } else {
            buildFile << "bootJar.mainClassName = 'com.example.springboottest.SpringBootApplication'\n"
        }
    }

    private boolean isSpringBoot1() {
        springBootVersion.startsWith("1")
    }
}
