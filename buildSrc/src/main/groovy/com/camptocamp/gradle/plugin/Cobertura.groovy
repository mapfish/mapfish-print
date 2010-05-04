package com.camptocamp.gradle.plugin;

import org.gradle.api.*;

class Cobertura implements Plugin<Project> {  

    def void apply(Project project) {
        def cobSerFile="${project.buildDir}/cobertura.ser"
        def srcOriginal="${project.sourceSets.main.classesDir}"
        def srcCopy="${srcOriginal}-copy"
        def version = '1.9.3'
        
        def ant = project.ant
        
        project.configurations {
            coberturaConf
        }

        project.dependencies {
            coberturaConf "net.sourceforge.cobertura:cobertura:$version"
            testRuntime "net.sourceforge.cobertura:cobertura:$version"
        }

        project.test.doFirst  {            
            // delete data file for cobertura, otherwise coverage would be added
            ant.delete(file:cobSerFile, failonerror:false)
            // delete copy of original classes
            ant.delete(dir: srcCopy, failonerror:false)
            // import cobertura task, so it is available in the script
            ant.taskdef(resource:'tasks.properties', classpath: project.configurations.coberturaConf.asPath)
            // create copy (backup) of original class files
            ant.copy(todir: srcCopy) {
                fileset(dir: srcOriginal)
            }
            
                // instrument the relevant classes in-place
            ant.'cobertura-instrument'(datafile:cobSerFile) {
                    fileset(dir: srcOriginal,
                           includes:"**/*.class",
                           excludes:"**/*Test.class")
            }
            
        }

        project.test {
            options.systemProperties['net.sourceforge.cobertura.datafile']="${cobSerFile}"
        }

        project.test.doLast {
            if (new File(srcCopy).exists()) {
                // replace instrumented classes with backup copy again
                ant.delete(file: srcOriginal)
                ant.move(file: srcCopy,
                         tofile: srcOriginal)
                // create cobertura reports
                ant.'cobertura-report'(destdir:"$project.buildDir/reports/test-coverage",
                     format:'html', srcdir:"src/main/java", datafile:cobSerFile)
            }
        }
    }
}