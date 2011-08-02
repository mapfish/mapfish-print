/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */
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
