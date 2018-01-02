/*
 * Copyright 2018 John Ahlroos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.plugin;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runner for payara
 *
 * @author John Ahlroos
 */
public class PayaraServerRunner {

    private static final Logger LOGGER = Logger.getLogger(PayaraServerRunner.class.getName());

    // Usage: 'PayaraServerRunner [port] [webbappdir] [classesdir] [resourcesdir] [LogLevel] [name] [workdir]'
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        Level logLevel = Level.parse(args[4]);
        String workdir = args[6];

        LOGGER.log(Level.CONFIG, "Configuring logger log levels to "+logLevel);

        Logger.getLogger("").getHandlers()[0].setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system.tools.deployment").setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system").setLevel(logLevel);

        LOGGER.log(Level.INFO, "Starting Payara web server...");

        try {

            BootstrapProperties bootstrap = new BootstrapProperties();

            GlassFishRuntime runtime = GlassFishRuntime.bootstrap(bootstrap,
                    PayaraServerRunner.class.getClass().getClassLoader());

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.setPort("http-listener", port);
            LOGGER.log(Level.INFO, "Running on port "+port);

            GlassFish glassfish = runtime.newGlassFish(glassfishProperties);
            glassfish.start();

            Deployer deployer = glassfish.getDeployer();

            File work = new File(workdir);
            File explodedWar = new File(work, "war");

            deployer.deploy(explodedWar, "--contextroot=");

        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, "Failed to start Payara server", ex);
            throw ex;
        }
    }
}
