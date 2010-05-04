package com.camptocamp.gradle.plugin;

import org.gradle.api.Project;

class CamptocampConvention {
    String filterResourcesIn
    String filterResourcesOut

    String filterWebappIn
    String filterWebappOut

    def  CamptocampConvention(Project project) {
        filterResourcesIn = project.file("src/main/filter-resources")
        filterResourcesOut = project.file("$project.buildDir/classes/main/")

        filterWebappIn = project.file("src/main/filter-webapp")
        filterWebappOut = project.file("$project.buildDir/filtered/webapp")
    }
}
