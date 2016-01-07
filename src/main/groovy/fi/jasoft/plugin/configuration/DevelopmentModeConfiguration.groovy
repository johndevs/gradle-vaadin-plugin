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

/**
 * Configuration for Development Mode
 */
@PluginConfiguration
class DevelopmentModeConfiguration {

    /**
     * Should the internal server be used.
     */
    boolean noserver = false

    /**
     * Should super devmode be available
     *
     * @deprecated
     */
    @Deprecated
    void setSuperDevMode(boolean sdm){
        isSuperDevMode()
    }
    @Deprecated
    void superDevMode(boolean sdm){
       isSuperDevMode()
    }
    @Deprecated
    boolean isSuperDevMode(){
        MessageLogger.nagUserOfDiscontinuedProperty('vaadin.devmode.superDevMode',
                'This property will always be true.')
        true
    }

    /**
     * To what host or ip should development mode bind itself to. By default localhost.
     */
    String bindAddress = '127.0.0.1'

    /**
     * To what port should development mode bind itself to.
     */
    int codeServerPort = 9997

    /**
     * Extra arguments passed to the code server
     */
    String[] extraArgs = null
}
