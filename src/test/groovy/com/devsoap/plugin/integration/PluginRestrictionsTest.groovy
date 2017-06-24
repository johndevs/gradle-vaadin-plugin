package com.devsoap.plugin.integration

import static org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by john on 1/6/15.
 */
class PluginRestrictionsTest extends IntegrationTest {

    @Test void 'No Vaadin 6 support'() {
        buildFile << """
            vaadin {
                version '6.8.0'
            }
        """.stripMargin()

        assertTrue runFailureExpected().contains('Plugin no longer supports Vaadin 6')
    }
}
