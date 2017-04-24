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
package fi.jasoft.plugin;

import org.glassfish.embeddable.*;
import org.glassfish.embeddable.archive.ScatteredArchive;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PayaraServerRunner {

    private static final Logger LOGGER = Logger.getLogger(PayaraServerRunner.class.getName());

    // Usage: 'PayaraServerRunner [port] [webbappdir] [classesdir] [resourcesdir] [LogLevel] [name] [workdir]'
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String webAppDir = args[1];
        String classesDir = args[2];
        String resourcesDir = args[3];
        Level logLevel = Level.parse(args[4]);
        String name = args[5];
        String workdir = args[6];

        String[] dependencies = new String(Files.readAllBytes(Paths.get(workdir + "/classpath.txt")), StandardCharsets.UTF_8).split(";");

        LOGGER.log(Level.CONFIG, "Configuring logger log levels to "+logLevel);

        Logger.getLogger("").getHandlers()[0].setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system.tools.deployment").setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system").setLevel(logLevel);

        LOGGER.log(Level.INFO, "Starting Payara web server...");

        try {

            BootstrapProperties bootstrap = new BootstrapProperties();

            GlassFishRuntime runtime = GlassFishRuntime.bootstrap(bootstrap, PayaraServerRunner.class.getClass().getClassLoader());

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.setPort("http-listener", port);
            LOGGER.log(Level.INFO, "Running on port "+port);

            GlassFish glassfish = runtime.newGlassFish(glassfishProperties);
            glassfish.start();

            LOGGER.log(Level.INFO, "Payara started, assembling web application");

            Deployer deployer = glassfish.getDeployer();
            ScatteredArchive archive = new ScatteredArchive(
                    name,
                    ScatteredArchive.Type.WAR,
                    new File(webAppDir));

            File classes = new File(classesDir);
            if(classes.exists()){
                archive.addClassPath(new File(classesDir));
                LOGGER.log(Level.INFO, "Added "+ classesDir);
            }

            File resources = new File(resourcesDir);
            if(resources.exists()) {
                archive.addClassPath(new File(resourcesDir));
                LOGGER.log(Level.INFO, "Added "+ resourcesDir);
            }

            for(String dependency : dependencies){
                File jar = new File(dependency);
                if(jar.exists()){
                    archive.addClassPath(new File(dependency));
                }
            }
            LOGGER.log(Level.INFO, "Added dependencies listed in "+ Paths.get(workdir + "/classpath.txt"));

            String tmp = System.getProperty("java.io.tmpdir");
            System.setProperty("java.io.tmpdir", workdir);
            URI archiveURI = archive.toURI();
            System.setProperty("java.io.tmpdir", tmp);

            deployer.deploy(archiveURI, "--contextroot=");

            LOGGER.log(Level.INFO, "Web application located at "+archiveURI+" deployed.");

        } catch (Exception ex){
            LOGGER.log(Level.SEVERE, "Failed to start Payara server", ex);
            throw ex;
        }
    }
}
