package com.camptocamp.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

class ProjectLayout extends DefaultTask { 
    
    public static final def TASK_NAME = "layout"
    
    def projectName = "$project.name"
    def baseDir = "."
    def devHost = "$projectName-dev.int.lsn.camptocamp.com"
    def demoHost = "$projectName-demo.dmz.lsn.camptocamp.com"
    def prodHost = "${projectName}.camptocamp.com"
    def filterFile = "resource.filter"
    
    def filterFiles = [
        dev: {"$baseDir/filters/dev/$filterFile"},
        demo: {"$baseDir/filters/demo/$filterFile"},
        prod: {"$baseDir/filters/prod/$filterFile"},
        local: {"$baseDir/filters/local/$filterFile"}/*,
        global: {"$baseDir/filters/global-$filterFile"}*/
    ]
    def directories = [
        srcJava: {"$baseDir/$baseDir/src/main/java"},
        srcResources: {"$baseDir/src/main/resources"},
        testJava: {"$baseDir/src/test/java"},
        filteredResource: {"$baseDir/src/main/filter-resources"},
        filteredWebapp: {"$baseDir/src/main/filter-webapp"},
        testResources: {"$baseDir/src/test/resources"}
    ]
    
    def filterFile(String publicHost) {
"""# This file contains key-value pairs that will be used to resolve @key@ strings in the filtered resources

public_host=$publicHost
"""
    }
    
    @TaskAction
    def basicLayout() { 
       filterFiles.each { path ->
            def file = new File(path.value.call())
            file.getParentFile().mkdirs()
            logger.info('creating '+file)
            if (! file.exists() ){
               if(properties.containsKey(path.key+"Host")) {
                   file.write(filterFile(this[path.key+"Host"]))
               } else {
                   file.write("")
               }
            }
        }
       
        directories.each {
            logger.info('creating '+it.value.call())
            new File(it.value.call()).mkdirs()
        }
   }
}