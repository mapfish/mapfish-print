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
package com.camptocamp.gradle.plugin;

import org.gradle.api.*;
import org.gradle.api.plugins.*;

class ByteCodeQuality implements Plugin<Project> {  
    String version = "1.3.9"
  
    def void apply(Project project) {
        project.configurations {
            findbugsConf
        }

        project.dependencies {
            findbugsConf "com.google.code.findbugs:findbugs:$version", "com.google.code.findbugs:findbugs-ant:$version"
        }

        project.task("findbugs", dependsOn: project.jar) << {
            gradleUserHomeDir = project.gradle.gradleUserHomeDir
            ant = project.ant
            home = new File(gradleUserHomeDir,"findbugs")

            libs = new File(home, "lib")
            findBugsJar = new File(libs,"findbugs.jar")
            if(!findBugsJar.exists()) {
                archive = "findbugs-${version}.zip"
                ant.get(src: "http://downloads.sourceforge.net/project/findbugs/findbugs/1.3.9/$archive?use_mirror=switch",
                        dest: "$gradleUserHomeDir/$archive", verbose: "on", usetimestamp: "on")
                ant.unzip(src: "$gradleUserHomeDir/$archive",
                          dest: "$gradleUserHomeDir")
                ant.move(file: "$gradleUserHomeDir/findbugs-$version",
                         tofile: "$gradleUserHomeDir/findbugs")
                ant.delete(file: "$gradleUserHomeDir/$archive")
            }

            result = ant {
                taskdef(name:'findbugs', classname:'edu.umd.cs.findbugs.anttask.FindBugsTask', 
                        classpath: project.configurations.findbugsConf.asPath)

                findbugs(home: home.path, output:'html', outputFile: "$project.buildDir/reports/findbugs.html") {
                    sourcePath(path: sourceSets.main.java)
                    "class"(location: sourceSets.main.classes.asPath)
                }
            }
        }
        
        project.build.dependsOn 'findbugs'
    }
}
