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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

class AddSecurityProxy extends ProjectLayout {
    public static final def TASK_NAME = "addSecurityProxy"
    
    def proxyDir = System.getProperty('proxyDir') ?: 'security-proxy'
    
    def AddSecurityProxy() {
        // this is to layout the config with the standard options
        // see ProjectLayout properties
        baseDir = proxyDir+'/config'
        directories = [ ]
    }

    def basicLayout() {

        def settingsFile = new File("$project.projectDir/settings.gradle")
        settingsFile.append("\ninclude ':$proxyDir', ':$proxyDir:cas', ':$proxyDir:config', ':$proxyDir:core'")

        gitSubmoduleAddProxy()
       gitCloneConfig()
        
        super.basicLayout()
    }
    
    def gitSubmoduleAddProxy() {
        def url = 'git@github.com:jesseeichar/security-proxy.git'
//        url = "/Users/jeichar/Local_Projects/security-proxy"
        project.logger.quiet("cloning project: $url")
        
        project.ant.exec(executable: 'git', failonerror: true, logError: true) {
            arg(value: 'clone')
            arg(value: '--recursive')
            arg(value: url)
            arg(value: "$proxyDir")
        }
    }
    
    def gitCloneConfig() {
        def url = 'git@github.com:jesseeichar/proxy-config.git'
 //       url = "/Users/jeichar/Local_Projects/security-proxy/config"
        project.logger.quiet("cloning project: $url")
        project.ant.exec(executable: 'git', failonerror: true, logError: true) {
            arg(value: 'clone')
            arg(value: url)
            arg(value: "$proxyDir/config")
        }
        
        
        project.ant.delete(dir: "$proxyDir/config/.git", failonerror: true)                

    }
}