package fi.jasoft.plugin.servers

/**
 * Created by john on 1.1.2016.
 */
class PayaraApplicationServer extends ApplicationServer {

    public static final String NAME = 'payara'

    PayaraApplicationServer(Object project, Object browserParameters) {
        super(project, browserParameters)
    }

    @Override
    String getServerRunner() {
        'fi.jasoft.plugin.PayaraServerRunner'
    }

    @Override
    String getServerName() {
        NAME
    }

    @Override
    String getSuccessfullyStartedLogToken() {
        'was successfully deployed'
    }
}
