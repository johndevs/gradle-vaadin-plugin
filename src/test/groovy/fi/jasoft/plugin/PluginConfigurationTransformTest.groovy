package fi.jasoft.plugin

import fi.jasoft.plugin.configuration.PluginConfigurationTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.tools.ast.TransformTestHelper
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Created by john on 8/8/14.
 */
class PluginConfigurationTransformTest {

    @Test
    public void testPluginTransform() {

        def file = 'src/main/groovy/fi/jasoft/plugin/configuration/VaadinPluginExtension.groovy' as File
        assertTrue('File '+file.canonicalPath+' does not exist', file.exists())

        def invoker = new TransformTestHelper(new PluginConfigurationTransformation(), CompilePhase.CANONICALIZATION)
        def clazz = invoker.parse(file)
        def vaadin = clazz.newInstance()

        // Test direct properties
        assertEquals("43211", vaadin.version("43211"))
        assertEquals(9999, vaadin.serverPort(9999))

        // Test delegated properties
        vaadin.gwt.style = "INFO"
        assertEquals("INFO",vaadin.gwt.style)
    }
}
