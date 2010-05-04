package com.camptocamp.gradle.task;

import org.apache.tools.ant.filters.ReplaceTokens

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Copy;

import java.io.File

class Filtering extends Copy {
    
    def String beginToken = "@"
    def String endToken = "@"
    private def Iterable filterFiles = []
    
    @InputFiles
     def Iterable getFilterFiles() {
         return filterFiles
     }

    def filterFile(File f) {
        if(!f.exists()) {
            project.logger.info("$f was added as a filterFile but does not exist")
        } else {
            filterFiles += f

            def properties = new Properties()
            f.withReader { properties.load(it) }
            
            def MATCH = /@.*@/
            
            def toResolve = properties.findAll { it.value =~ MATCH }
            def toRemove = []
            
            def count = 0
            def MAX_RESOLVES = 100
            
            while(!toResolve.isEmpty() && count < MAX_RESOLVES) {
                count ++
                toResolve.each { entry ->
                    def m = entry.value =~ /@(.*?)@/
                    
                    def updated = entry.value
                    m.each {
                        updated = updated.replace(it[0], properties[it[1]])
                    }
                    properties[entry.key] = updated
                    
                    if(!(updated =~ MATCH)) {
                        toRemove << entry.key
                    }
                }
                toRemove.each{toResolve.remove(it)}
            }
            
            if(count >= MAX_RESOLVES){
                String unresolved = ""
                toResolve.each(unresolved += "it.key -> $it.value\n")
                throw new AssertionError("after $MAX_RESOLVES resolves it was not possible to resolve all elements in file $f:\n"+unresolved)
            }

            filter(ReplaceTokens, tokens: properties)
        }
    }
    
    def filterFile(String fileName) {
        filterFile(project.file(fileName))
    }
    
    def filterFiles(Iterable files) {
        files.each {file ->
            if(file instanceof String) {
                filterFile ((String)(file))
            } else {
                filterFile ((File)(file))
            }
        }
    }
    
    
    def setFilterFiles(Iterable files) {
        filterFiles = []
        filterFiles(files)
    }
}