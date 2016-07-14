/*
* Copyright 2016 John Ahlroos
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

        Logger.getLogger("").getHandlers()[0].setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system.tools.deployment").setLevel(logLevel);
        Logger.getLogger("javax.enterprise.system").setLevel(logLevel);

        try {

            BootstrapProperties bootstrap = new BootstrapProperties();

            GlassFishRuntime runtime = GlassFishRuntime.bootstrap(bootstrap, PayaraServerRunner.class.getClass().getClassLoader());

            GlassFishProperties glassfishProperties = new GlassFishProperties();
            glassfishProperties.setPort("http-listener", port);

            GlassFish glassfish = runtime.newGlassFish(glassfishProperties);
            glassfish.start();

            Deployer deployer = glassfish.getDeployer();
            ScatteredArchive archive = new ScatteredArchive(
                    name,
                    ScatteredArchive.Type.WAR,
                    new File(webAppDir));
            archive.addClassPath(new File(classesDir));
            archive.addClassPath(new File(resourcesDir));

            for(String dependency : dependencies){
                archive.addClassPath(new File(dependency));
            }

            String tmp = System.getProperty("java.io.tmpdir");
            System.setProperty("java.io.tmpdir", workdir);
            URI archiveURI = archive.toURI();
            System.setProperty("java.io.tmpdir", tmp);

            deployer.deploy(archiveURI, "--contextroot=");
        } catch (Exception ex){
            Logger.getLogger(PayaraServerRunner.class.getName()).log(Level.SEVERE,
                    null, ex);
        }

    }
}
