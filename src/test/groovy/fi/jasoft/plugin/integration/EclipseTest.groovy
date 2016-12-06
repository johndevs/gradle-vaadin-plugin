package fi.jasoft.plugin.integration

import groovy.transform.Memoized
import groovy.util.logging.Log
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.Node
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren
import org.junit.Test

import java.nio.file.Paths

import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals

/**
 * Created by john on 12/6/16.
 */
@Log
class EclipseTest extends IntegrationTest {

    @Override
    void setup() {
        super.setup()
        buildFile << "apply plugin: 'eclipse-wtp'\n"
    }

    @Test void 'Default facets are applied'() {

        runWithArguments('eclipse')

        List<Node> fixedFacets = facetedProject.childNodes().findAll { Node node -> node.name() == 'fixed' }
        assertEquals 'There should be two installed facets', 2, fixedFacets.size()
        assertEquals 'First should be Java facet', 'jst.java', fixedFacets[0].attributes()['facet']
        assertEquals 'Second should be Web facet', 'jst.web', fixedFacets[1].attributes()['facet']

        List<Node> installedFacets = facetedProject.childNodes().findAll { Node node -> node.name() == 'installed' }
        assertEquals 'There should be three installed facets', 3, installedFacets.size()
    }

    @Test void 'Preserve custom facets'() {
        buildFile << "eclipse { wtp { facet { facet name: 'wst.jsdt.web', version: '1.0' } } }"

        runWithArguments('eclipse')

        List<Node> installedFacets = facetedProject.childNodes().findAll { Node node ->
            node.name() == 'installed' &&
            node.attributes()['facet'] == 'wst.jsdt.web' &&
            node.attributes()['version'] == '1.0'
        }
        assertEquals 'The facet should still be installed', 1, installedFacets.size()
    }

    @Memoized
    private GPathResult getFacetedProject() {
        def settingsDir = new File(projectDir.root, '.settings')
        def projectFacetConfigFile = new File(settingsDir, 'org.eclipse.wst.common.project.facet.core.xml')
        new XmlSlurper().parse(projectFacetConfigFile)
    }
}
