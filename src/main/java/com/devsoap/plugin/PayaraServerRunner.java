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

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;

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
            PayaraMicro micro = PayaraMicro.getInstance();
            micro.setHttpPort(port);
            LOGGER.log(Level.INFO, "Running on port "+port);

            File work = new File(workdir);
            File explodedWar = new File(work, "war");
//            PayaraMicro.setUpackedJarDir(explodedWar);

            PayaraMicroRuntime runtime = micro.bootstrap();
            runtime.deploy(explodedWar);

        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, "Failed to start Payara server", ex);
            throw ex;
        }
    }
}
