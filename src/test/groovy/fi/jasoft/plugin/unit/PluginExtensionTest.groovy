/*
* Copyright 2017 John Ahlroos
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.jasoft.plugin.unit

import fi.jasoft.plugin.configuration.AddonConfiguration
import fi.jasoft.plugin.configuration.CompileWidgetsetConfiguration
import fi.jasoft.plugin.configuration.DevelopmentModeConfiguration
import fi.jasoft.plugin.configuration.GWTConfiguration
import fi.jasoft.plugin.configuration.SuperDevModeConfiguration
import fi.jasoft.plugin.configuration.VaadinPluginConfiguration
import fi.jasoft.plugin.configuration.VaadinPluginExtension
import org.junit.Test

/**
 * Tests that the plugin extensions are correctly configured
 */
class PluginExtensionTest extends PluginTestBase {

    @Test
    void isVaadinExtensionPresent() {
        assert project.extensions.vaadin instanceof VaadinPluginExtension
    }

    @Test
    void areVaadinPropertiesConfigured() {
        VaadinPluginExtension vaadin = project.extensions.vaadin
        assert vaadin.version == null
        assert vaadin.manageDependencies == true
        assert vaadin.manageRepositories == true
        assert vaadin.devmode instanceof DevelopmentModeConfiguration
        assert vaadin.plugin instanceof VaadinPluginConfiguration
        assert vaadin.addon instanceof AddonConfiguration
        assert vaadin.gwt instanceof GWTConfiguration
        assert vaadin.mainSourceSet == null
        assert vaadin.push == false
    }

    @Test
    void testVaadinClosure() {
        project.vaadin{
            widgetset 'com.example.Widgetset'
            widgetsetGenerator 'com.example.generator'
            version  "7+"
            manageWidgetset true
            manageDependencies true
            mainSourceSet null
            push false
        }
        assert project.extensions.vaadin.widgetset == 'com.example.Widgetset'
    }

    @Test
    void areGWTPropertiesConfigured() {
        CompileWidgetsetConfiguration conf = project.vaadinCompile
        assert conf instanceof CompileWidgetsetConfiguration
        assert conf.style == "OBF"
        assert conf.optimize == 0
        assert conf.logLevel == "INFO"
        assert conf.localWorkers == Runtime.getRuntime().availableProcessors()
        assert conf.draftCompile == true
        assert conf.strict == true
        assert conf.userAgent == null
        assert conf.jvmArgs == null
        assert conf.extraArgs == null
        assert conf.sourcePaths == ['client', 'shared'] as String[]
        assert conf.collapsePermutations == true
        assert conf.outputDirectory == null
    }

    @Test
    void testGWTClosure() {
        project.vaadinCompile {
                style "PRETTY"
                optimize 0
                logLevel "INFO"
                localWorkers Runtime.getRuntime().availableProcessors()
                draftCompile false
                strict false
                userAgent "ie8,ie9,gecko1_8,safari,opera"
                jvmArgs null
                extraArgs null
                sourcePaths (['client', 'shared'] as String[])
                collapsePermutations false
        }
        assert project.extensions.vaadinCompile.style == "PRETTY"
    }

    @Test
    void areDevModePropertiesConfigured() {
        SuperDevModeConfiguration conf = project.extensions.vaadinSuperDevMode
        assert conf instanceof SuperDevModeConfiguration
        assert conf.noserver == false
        assert conf.bindAddress == '127.0.0.1'
        assert conf.codeServerPort == 9997
    }

    @Test
    void testDevModeClosure() {
        project.vaadinDevMode {
            noserver false
            bindAddress '0.0.0.0'
            codeServerPort 9997
        }
        assert project.extensions.vaadinDevMode.bindAddress == '0.0.0.0'
    }

    @Test
    void areVaadinPluginConfigurationPropertiesConfigured() {
        VaadinPluginExtension vaadin = project.extensions.vaadin
        assert vaadin.plugin instanceof VaadinPluginConfiguration
        assert vaadin.logToConsole == false
    }

    @Test
    void testPluginConfigurationClosure() {
        assert project.extensions.vaadin.logToConsole == false
        project.vaadin.plugin {
            logToConsole true
        }
        assert project.extensions.vaadin.logToConsole == true
    }

    @Test
    void areAddonPropertiesConfigured() {
        VaadinPluginExtension vaadin = project.extensions.vaadin
        assert vaadin.addon instanceof AddonConfiguration
        assert vaadin.addon.author == ''
        assert vaadin.addon.license == ''
        assert vaadin.addon.title == ''
    }

    @Test
    void testAddonClosure() {
        project.vaadin.addon {
            author 'Testing person'
            license ''
            title ''
        }
        assert project.extensions.vaadin.addon.author == 'Testing person'
    }

}
