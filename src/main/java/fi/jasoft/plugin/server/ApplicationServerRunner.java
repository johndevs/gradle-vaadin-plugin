package fi.jasoft.plugin.server;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.plus.webapp.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.annotations.*;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnnie
 * Date: 4/16/13
 * Time: 8:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationServerRunner {

    public static void main(String[] args) throws Exception{
        int port = Integer.parseInt(args[0]);
        String webapp = args[1];
        String classes = args[2];

        List<String> resources = new ArrayList<String>();
        if(new File(webapp).exists()){
            resources.add(webapp);
        }
        if(new File(classes).exists()){
            resources.add(classes);
        }

        // For debugging
        // System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");

        Server server = new Server(port);

        // Static file handler
        WebAppContext handler = new WebAppContext();
        handler.setConfigurations(new Configuration[] {
                new RJRAnnotationConfiguration(resources), new WebXmlConfiguration(),
                new WebInfConfiguration(), new TagLibConfiguration(),
                new PlusConfiguration(), new MetaInfConfiguration(),
                new FragmentConfiguration(), new EnvConfiguration() });
        handler.setContextPath("/");

        handler.setBaseResource(new ResourceCollection(resources.toArray(new String[resources.size()])));

        handler.setParentLoaderPriority(true);
        handler.setClassLoader(Thread.currentThread().getContextClassLoader());

        server.setHandler(handler);
        server.start();
        server.join();
    }
}
