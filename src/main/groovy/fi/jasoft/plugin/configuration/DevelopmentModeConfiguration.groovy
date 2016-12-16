/*
* Copyright 2016 John Ahlroos
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
package fi.jasoft.plugin.configuration

import fi.jasoft.plugin.MessageLogger
import org.gradle.api.Project

/**
 * Configuration for Development Mode
 *
 * @author John Ahlroos
 */
@PluginConfiguration
@Deprecated
class DevelopmentModeConfiguration {

    @Deprecated
    transient Project project

    @Deprecated
    DevelopmentModeConfiguration(Project project) {
        this.project = project
    }

    /**
     * Should the internal server be used.
     */
    @Deprecated
    void noserver(boolean noserver) {
        project.vaadinSuperDevMode.noserver = noserver
        getNoserver()
    }
    @Deprecated
    void setNoserver(boolean noserver) {
        project.vaadinSuperDevMode.noserver = noserver
        getNoserver()
    }
    @Deprecated
    boolean getNoserver() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.noserver',
                'This property has been replaced by vaadinSuperDevMode.noserver.')
        project.vaadinSuperDevMode.noserver
    }

    /**
     * Should super devmode be available
     *
     * @deprecated
     */
    @Deprecated
    void setSuperDevMode(boolean sdm) {
        assert sdm
        isSuperDevMode()
    }
    @Deprecated
    void superDevMode(boolean sdm) {
        assert sdm
       isSuperDevMode()
    }
    @Deprecated
    boolean isSuperDevMode() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.superDevMode',
                'This property has been removed and will always be true.')
        true
    }

    /**
     * To what host or ip should development mode bind itself to. By default localhost.
     */
    @Deprecated
    void bindAddress(String address) {
        project.vaadinSuperDevMode.bindAddress = address
        getBindAddress()
    }
    @Deprecated
    void setBindAddress(String address) {
        project.vaadinSuperDevMode.bindAddress = address
        getBindAddress()
    }
    @Deprecated
    String getBindAddress() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.bindAddress',
                'This property has been replaced by vaadinSuperDevMode.bindAddress.')
        project.vaadinSuperDevMode.bindAddress
    }

    /**
     * To what port should development mode bind itself to.
     */
    @Deprecated
    void codeServerPort(Integer port) {
        project.vaadinSuperDevMode.codeServerPort = port
        getCodeServerPort()
    }
    @Deprecated
    void setCodeServerPort(Integer port) {
        project.vaadinSuperDevMode.codeServerPort = port
        getCodeServerPort()
    }
    @Deprecated
    Integer getCodeServerPort() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.codeServerPort',
                'This property has been replaced by vaadinSuperDevMode.codeServerPort.')
        project.vaadinSuperDevMode.codeServerPort
    }

    /**
     * Extra arguments passed to the code server
     */
    @Deprecated
    void extraArgs(String[] args) {
        project.vaadinSuperDevMode.extraArgs = args
        getExtraArgs()
    }
    @Deprecated
    void setExtraArgs(boolean args) {
        project.vaadinSuperDevMode.extraArgs = args
        getExtraArgs()
    }
    @Deprecated
    String[] getExtraArgs() {
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.extraArgs',
                'This property has been replaced by vaadinSuperDevMode.extraArgs.')
        project.vaadinSuperDevMode.extraArgs
    }
}
