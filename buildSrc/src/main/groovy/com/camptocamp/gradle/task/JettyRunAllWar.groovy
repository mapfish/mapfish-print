package com.camptocamp.gradle.task;

import org.apache.tools.ant.filters.ReplaceTokens

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Copy;
import org.gradle.api.plugins.jetty.JettyRunWar;
import org.gradle.api.plugins.jetty.internal.JettyPluginWebAppContext;
import org.gradle.api.plugins.jetty.internal.JettyPluginServer;


import java.io.File

class JettyRunAllWar extends JettyRunWar {

    public static final def TASK_NAME = "jettyRunAllWar"

    /**
     * Projects that will also be ran but will 
     * not be the project whose sources will be scanned
     */
    def backgroundProjects = project.rootProject.allprojects.findAll {
            project.path != it.path && war(it) != null
        }.asList()
    
    public JettyRunAllWar() {
        println("running JettyRunAllWar on "+project.path)
        webApp = war(project).archivePath
        backgroundProjects.tail().each { proj -> 
            if(war(proj) == null) {
                throw AssertionError( proj.path + "does not have a war task")
            }
            
        }

    }
    
    static def war(project) {
        def tasks =  project.getTasksByName("war",false)
        return tasks.find{true}
    }
    
    public void configureWebApplication() throws Exception {
        super.configureWebApplication()
        backgroundProjects.each{ proj ->
            def warTask = war(proj)
            def context = new JettyPluginWebAppContext()
            context.setContextPath(proj.name)
            context.setWar(warTask.archivePath.absolutePath)
            context.configure()
            getServer().addWebApplication(context)
        }
        
    }

}
