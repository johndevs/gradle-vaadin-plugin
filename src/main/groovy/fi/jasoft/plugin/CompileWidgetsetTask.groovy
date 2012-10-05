package fi.jasoft.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.JavaExec;
import fi.jasoft.plugin.VaadinPlugin;
import fi.jasoft.plugin.ui.TemplateUtil;
import org.gradle.api.plugins.WarPluginConvention;
import org.gradle.api.file.FileCollection;

class CompileWidgetsetTask extends JavaExec {
   
    @Override
    public void exec(){
    	if(project.vaadin.widgetset == null){
    		println "No widgetset defined. Please add a widgetset to your Vaadin configuration."
    		return;	
    	}

    	File webAppDir = project.convention.getPlugin(WarPluginConvention).webAppDir
    	File targetDir = new File(webAppDir.canonicalPath+'/VAADIN/widgetsets')
    	getOutputs().dir(targetDir)

    	// Create a widgetset if needed
    	ensureWidgetPresent()

		/* Monitor changes in dependencies since upgrading a
		* dependency should also trigger a recompile of the widgetset
		*/
    	getInputs().files(project.configurations.compile)

    	// Monitor changes in client side classes
		project.sourceSets.main.java.srcDirs.each{
			getInputs().files(project.fileTree(it.absolutePath).include('**/*/client/**/*.java'))
			getInputs().files(project.fileTree(it.absolutePath).include('**/*/shared/**/*.java'))
		} 
    	
        FileCollection classpath = project.files(project.sourceSets.main.runtimeClasspath)
        classpath = project.files(classpath, project.configurations.compile.asPath)
        classpath = project.files(classpath, project.configurations.providedCompile.asPath)
        project.sourceSets.main.java.srcDirs.each{
            classpath = project.files(classpath, it)
        }
        setClasspath(classpath)
        setMain('com.google.gwt.dev.Compiler')
        setArgs(['-style', project.vaadin.gwtStyle, '-optimize', project.vaadin.gwtOptimize, '-war', targetDir.canonicalPath, project.vaadin.widgetset])
        
        super.exec();

        /*
         * Compiler generates an extra WEB-INF folder into the widgetsets folder. Remove it.
         */
         new File(targetDir.canonicalPath+"/WEB-INF").deleteDir()
    }

    private void ensureWidgetPresent(){
    	File widgetsetFile = new File('src/main/java/'+project.vaadin.widgetset.replaceAll(/\./,'/')+".gwt.xml")
    	inputs.file widgetsetFile

    	File widgetsetDir = new File(widgetsetFile.parent)
    	if(!widgetsetFile.exists()){
    		TemplateUtil.writeTemplate(project, 'Widgetset.xml', widgetsetDir, project.vaadin.widgetset.tokenize('.').last()+".gwt.xml")
    		println "Create widgetset file in "+widgetsetFile
    	}
    }

}
