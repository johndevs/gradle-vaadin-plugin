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

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class JettyServerRunner {

    // Usage: 'JettyServerRunner [port] [webbappdir] [classesdir] [resourcesdir] [LogLevel]'
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        String webAppDir = args[1];
        String classesDir = args[2];
        String resourcesDir = args[3];
        String logLevel = args[4];

        List<String> resources = new ArrayList<>();
        if (new File(webAppDir).exists()){
            resources.add(webAppDir);
        }

        System.setProperty("org.eclipse.jetty.LEVEL", logLevel);

        Server server = new Server(port);
    
        // Setup JMX
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);

        // Static file handler
        WebAppContext handler = new WebAppContext();
        server.setHandler(handler);

        handler.setContextPath("/");
        handler.setBaseResource(Resource.newResource(webAppDir));
        handler.setParentLoaderPriority(true);
        handler.setExtraClasspath(classesDir+";"+resourcesDir);
        handler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/build/classes/.*");
        handler.setConfigurations(new Configuration[]{
                new WebXmlConfiguration(),
                new WebInfConfiguration(),
                new PlusConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new AnnotationConfiguration(),
                new JettyWebXmlConfiguration()
        });
        handler.setClassLoader(new WebAppClassLoader(JettyServerRunner.class.getClassLoader(), handler));

        server.start();
        server.join();
    }
}
