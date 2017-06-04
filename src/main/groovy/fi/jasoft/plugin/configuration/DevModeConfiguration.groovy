package fi.jasoft.plugin.configuration

import fi.jasoft.plugin.tasks.DevModeTask

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
}
