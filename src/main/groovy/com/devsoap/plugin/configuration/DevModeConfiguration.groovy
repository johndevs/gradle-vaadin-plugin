package com.devsoap.plugin.configuration

import com.devsoap.plugin.configuration.PluginConfiguration
import com.devsoap.plugin.tasks.DevModeTask

/**
 * Created by john on 4/2/17.
 *
 * @deprecated
 */
@Deprecated
@PluginConfiguration
@PluginConfigurationName(DevModeTask.NAME)
class DevModeConfiguration extends SuperDevModeConfiguration {
    /*
     * To allow configuration of the deprecated devmode task. Will be removed later
     */

    /**
     * To what port should development mode bind itself to.
     */
    int codeServerPort = 9997
}
