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