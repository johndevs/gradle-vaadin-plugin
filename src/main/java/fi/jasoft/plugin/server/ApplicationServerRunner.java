/*
* Copyright 2014 John Ahlroos
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
package fi.jasoft.plugin.server;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.deploy.DeploymentManager;
import org.eclipse.jetty.deploy.PropertiesConfigurationManager;
import org.eclipse.jetty.deploy.providers.WebAppProvider;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the embedded jetty server with provided paramaters
 *
 */
public class ApplicationServerRunner {

    /**
     *
     * @param args
     *      param 1: The webapp folder path
     *      param 2: Path to precompiled classes
     *      *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        int port = Integer.parseInt(args[0]);
        String webapp = args[1];
        String classes = args[2];

        List<String> resources = new ArrayList<String>();
        List<Resource> resources2 = new ArrayList<Resource>();
        if(new File(webapp).exists()){
            resources.add(webapp);
            resources2.add(new FileResource(new File(webapp).toURI()));
        }
        if(new File(classes).exists()){
            resources.add(classes);
            resources2.add(new FileResource(new File(classes).toURI()));
        }

        // For debugging
        //System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");

        Server server = new Server(port);
    
        // Setup JMX
        MBeanContainer mbContainer=new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addBean(mbContainer);

        // Static file handler
        WebAppContext handler = new WebAppContext();
        handler.setConfigurations(new Configuration[] {
                new WebXmlConfiguration(),
                new WebInfConfiguration(),
                new PlusConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new AnnotationConfiguration(),
                new JettyWebXmlConfiguration(),
        });
        handler.setContextPath("/");
        handler.setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));
        handler.setParentLoaderPriority(true);
        handler.setExtraClasspath(classes);
        handler.setClassLoader(new WebAppClassLoader(ApplicationServerRunner.class.getClassLoader(), handler));
        handler.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",".*/build/classes/.*");
        server.setHandler(handler);

        server.start();
        server.join();
    }
}
