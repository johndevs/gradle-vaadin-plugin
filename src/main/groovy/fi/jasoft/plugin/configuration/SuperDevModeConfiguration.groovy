package fi.jasoft.plugin.configuration

/**
 * Configuration for the SuperDevMode task
 */
@PluginConfiguration
class SuperDevModeConfiguration extends ApplicationServerConfiguration {

    /**
     * Should the internal server be used.
     */
    boolean noserver = false

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

    /**
     * The log level. Possible levels NONE,DEBUG,TRACE,INFO
     */
    String logLevel = "INFO"
}
