package fi.jasoft.plugin.server;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.annotations.AbstractDiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.annotations.AnnotationParser.DiscoverableAnnotationHandler;
import org.eclipse.jetty.annotations.ClassNameResolver;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.FragmentDescriptor;
import org.eclipse.jetty.webapp.WebAppContext;


/**
 * Since the original design for AnnotationConfiguration only scan WEB-INF/classes , WEB-INF/libs.
 * We create a specific implemenation for Run-Jetty-Run application to have a better support.
 * @author TonyQ
 *
 */
public class RJRAnnotationConfiguration extends AnnotationConfiguration {
	private static Logger logger =  Log.getLogger(RJRAnnotationConfiguration.class);

    private final List<String> scannableClasspaths;

    public RJRAnnotationConfiguration(List<String> classpath){
        scannableClasspaths = classpath;
    }

	public void parseWebInfClasses(final WebAppContext context,
			final AnnotationParser parser) throws Exception {
		
		if (logger.isDebugEnabled()) logger.debug("Scanning classes in WEB-INF/classes");
		
        List<FragmentDescriptor> frags = context.getMetaData().getFragments();
        
        //email from Rajiv Mordani jsrs 315 7 April 2010
        //jars that do not have a web-fragment.xml are still considered fragments
        //they have to participate in the ordering
        ArrayList<URI> webInfUris = new ArrayList<URI>();
        
        List<Resource> jars = context.getMetaData().getOrderedWebInfJars();
        
        //No ordering just use the jars in any order
        if (jars == null || jars.isEmpty())
            jars = context.getMetaData().getWebInfJars();

        if(jars == null){
        	jars = new ArrayList<Resource>();
        }
        //Hacked , add RJR classpaths
        for (String path : scannableClasspaths) {
			File file = new File(path);
			if (file.isDirectory()) {
				if (logger.isDebugEnabled()) logger.debug("scanning RJR classes for annotation:" + file.getAbsolutePath());
				FileResource folder = (new FileResource(file.toURI().toURL()));
				if(folder.isDirectory()){
					parseClasses(context, folder, parser); //load annotation from classes file first
				}else{
					jars.add(new FileResource(file.toURI().toURL()));
				}
			}
		}
        
        for (Resource r : jars)
        {          
            //for each jar, we decide which set of annotations we need to parse for
            parser.clearHandlers();
            URI uri  = r.getURI();
            FragmentDescriptor f = getFragmentFromJar(r, frags);
           
            //if its from a fragment jar that is metadata complete, we should skip scanning for @webservlet etc
            // but yet we still need to do the scanning for the classes on behalf of  the servletcontainerinitializers
            //if a jar has no web-fragment.xml we scan it (because it is not excluded by the ordering)
            //or if it has a fragment we scan it if it is not metadata complete
            if (f == null || !isMetaDataComplete(f) || _classInheritanceHandler != null ||  !_containerInitializerAnnotationHandlers.isEmpty())
            {
                //register the classinheritance handler if there is one
                parser.registerHandler(_classInheritanceHandler);
                
                //register the handlers for the @HandlesTypes values that are themselves annotations if there are any
                parser.registerHandlers(_containerInitializerAnnotationHandlers);
                
                //only register the discoverable annotation handlers if this fragment is not metadata complete, or has no fragment descriptor
                if (f == null || !isMetaDataComplete(f))
                {
                    for (DiscoverableAnnotationHandler h:_discoverableAnnotationHandlers)
                    {
                        if (h instanceof AbstractDiscoverableAnnotationHandler)
                            ((AbstractDiscoverableAnnotationHandler)h).setResource(r);
                    }
                    parser.registerHandlers(_discoverableAnnotationHandlers);
                }

                parser.parse(uri, 
                             new ClassNameResolver()
                             {
                                 public boolean isExcluded (String name)
                                 {    
                                     if (context.isSystemClass(name)) return true;
                                     if (context.isServerClass(name)) return false;
                                     return false;
                                 }

                                 public boolean shouldOverride (String name)
                                 {
                                    //looking at webapp classpath, found already-parsed class of same name - did it come from system or duplicate in webapp?
                                    if (context.isParentLoaderPriority())
                                        return false;
                                    return true;
                                 }
                             });   
            }
        }		
	}
	
    /**
     * Scan classes in WEB-INF/classes
     * 
     * @param context
     * @param parser
     * @throws Exception
     */
    public void parseClasses (final WebAppContext context,Resource classesDir , final AnnotationParser parser)
    throws Exception
    {
        //LOG.debug("Scanning classes in  classes folders (hack by RJR)");
        if (classesDir.exists())
        {
            parser.clearHandlers();
            for (DiscoverableAnnotationHandler h:_discoverableAnnotationHandlers)
            {
                if (h instanceof AbstractDiscoverableAnnotationHandler)
                    ((AbstractDiscoverableAnnotationHandler)h).setResource(null); //
            }
            parser.registerHandlers(_discoverableAnnotationHandlers);
            parser.registerHandler(_classInheritanceHandler);
            parser.registerHandlers(_containerInitializerAnnotationHandlers);
            
            parser.parse(classesDir, 
                         new ClassNameResolver()
            {
                public boolean isExcluded (String name)
                {
                    if (context.isSystemClass(name)) return true;
                    if (context.isServerClass(name)) return false;
                    return false;
                }

                public boolean shouldOverride (String name)
                {
                    //looking at webapp classpath, found already-parsed class of same name - did it come from system or duplicate in webapp?
                    if (context.isParentLoaderPriority())
                        return false;
                    return true;
                }
            });
        }
    }
}
