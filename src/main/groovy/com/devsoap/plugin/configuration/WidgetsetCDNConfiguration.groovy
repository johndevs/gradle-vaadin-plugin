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
package com.devsoap.plugin.configuration

import com.devsoap.plugin.configuration.PluginConfiguration
import groovyx.net.http.AuthConfig

/**
 * Created by john on 1/19/17.
 */
@PluginConfiguration
class WidgetsetCDNConfiguration {

    /**
     * Should the widgetset compiler use a proxy
     */
    boolean proxyEnabled = false

    /**
     * The proxy port
     */
    int proxyPort = Integer.parseInt(System.getProperty('http.proxyPort') ?: '-1')

    /**
     * The proxy scheme
     */
    String proxyScheme = System.getProperty('http.proxyScheme') ?: 'http'

    /**
     * The proxy url
     */
    String proxyHost = System.getProperty('http.proxyHost') ?: 'localhost'

    /**
     * Proxy authentication configuration
     */
    AuthConfig proxyAuth = null
}
