package com.devsoap.plugin.tests

import com.devsoap.plugin.tasks.CompileWidgetsetTask
import com.devsoap.plugin.tasks.CreateProjectTask
import groovy.json.JsonSlurper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockserver.integration.ClientAndProxy
import java.nio.file.Paths
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertEquals
import static org.mockserver.integration.ClientAndProxy.startClientAndProxy

/**
 * Created by john on 1/19/17.
 */
class ProxyTest extends IntegrationTest {

    ClientAndProxy proxy

    @Before
    void startProxy() {
        proxy = startClientAndProxy(1090)
    }

    @After
    void stopProxy() {
        proxy.stop()
        proxy = null
    }

    @Test
    void 'Test widgetset CDN behind proxy'() {
        buildFile << """
            dependencies {
                compile 'org.vaadin.addons:qrcode:+'
            }

            vaadinCompile {
                widgetsetCDN true
                widgetsetCDNConfig {
                    proxyEnabled true
                    proxyHost 'localhost'
                    proxyScheme 'http'
                    proxyPort ${proxy.port}
                }
            }
        """

        runWithArguments(CreateProjectTask.NAME)

        String result = runWithArguments('--info', CompileWidgetsetTask.NAME)
        assertTrue result, result.contains('Querying widgetset for')
        assertTrue result, result.contains('Widgetset is available, downloading...')
        assertTrue result, result.contains('Extracting widgetset')
        assertTrue result, result.contains('Generating AppWidgetset')

        File appWidgetset = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'java', 'AppWidgetset.java').toFile()
        assertTrue 'AppWidgetset.java was not created', appWidgetset.exists()

        File widgetsetFolder = Paths.get(projectDir.root.canonicalPath,
                'src', 'main', 'webapp', 'VAADIN', 'widgetsets').toFile()
        assertTrue 'Widgetsets folder did not exist', widgetsetFolder.exists()
        assertTrue 'Widgetsets folder did not contain widgetset',
                widgetsetFolder.listFiles().size() == 1

        Map request = getRequest('/api/compiler/download')
        assertTrue 'Vaadin version was not right',  request.vaadinVersion.startsWith('8')
        assertEquals 'Compile style not correct', request.compileStyle, 'OBF'

        Map qrCodeAddon = request.addons[0]
        assertEquals qrCodeAddon.groupId, 'org.vaadin.addons'
        assertEquals qrCodeAddon.artifactId, 'qrcode'
        assertEquals qrCodeAddon.version, '2.1'
    }

    private Map getRequest(String path) {
        (Map) new JsonSlurper().parseText(parseRequests()
                .find { req -> req.path == path }.body)
    }

    private List<Map> parseRequests() {
        String json = proxy.retrieveAsJSON(null)
        (List<Map>) new JsonSlurper().parseText(json)
    }
}
