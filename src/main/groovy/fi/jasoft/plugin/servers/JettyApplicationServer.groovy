package fi.jasoft.plugin.servers

/**
 * Created by john on 1.1.2016.
 */
class JettyApplicationServer extends ApplicationServer {

    public static final String NAME = 'jetty'

    JettyApplicationServer(Object project, Object browserParameters) {
        super(project, browserParameters)
    }

    @Override
    String getServerRunner() {
        'fi.jasoft.plugin.JettyServerRunner'
    }

    @Override
    String getServerName() {
        'jetty'
    }

    @Override
    String getSuccessfullyStartedLogToken() {
        'org.eclipse.jetty.server.Server - Started'
    }
}
