package fi.jasoft.plugin.servers

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler

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

    @Override
    def defineDependecies(DependencyHandler projectDependencies, DependencySet dependencies) {
        def payaraWebProfile = projectDependencies.create('fish.payara.extras:payara-embedded-web:4.1.152.1')
        dependencies.add(payaraWebProfile)
    }
}
