organization := "org.mapfish.print"

name := "mapfish-print"

version := "1.2-SNAPSHOT"

springVersion := "3.1.0.RELEASE"

javacOptions ++= Seq ("-source", "1.5","-Xlint:deprecation", "-Xlint:unchecked")

compileOrder := CompileOrder.JavaThenScala

mainClass := Some("org.mapfish.print.ShellMapPrinter")

libraryDependencies <++= (springVersion) { springVersion => 
  Seq(
    "org.springframework" % "spring-context" % springVersion,
    "org.springframework" % "spring-web" % springVersion,
    "com.lowagie" % "itext" % "2.1.5",
    "xerces" % "xercesImpl" % "2.4.0",
    "org.json" % "json" % "20080701",
    "org.jyaml" % "jyaml" % "1.3",
    "ch.thus" % "pvalsecc" % "0.9.2",
    "xalan" % "xalan" % "2.7.0",
    "log4j" % "log4j" % "1.2.14",
    "com.vividsolutions" % "jts" % "1.8",
    "org.mapfish.geo" % "mapfish-geo-lib" % "1.2-SNAPSHOT",
    "commons-httpclient" % "commons-httpclient" % "3.1",
    "org.geotools" % "gt-epsg-hsql" % "2.6.5",
    "org.apache.pdfbox" % "pdfbox" % "1.6.0",
    "javax.media" % "jai_core" % "1.1.3",
    "javax.media" % "jai_imageio" % "1.1",
    "javax.media" % "jai_codec" % "1.1.3",
    "org.apache.xmlgraphics" % "batik-transcoder" % "1.7" exclude (org="org.apache.xmlgraphics", name="fop"),
    "javax.servlet" % "servlet-api" % "2.5" % "provided",
    "junit" % "junit" % "4.8" % "test")
  }
  
resolvers ++= Seq(
  "Mapfish" at "http://dev.mapfish.org/maven/repository",
  "OSGeo" at "http://download.osgeo.org/webdav/geotools",
  "IBiblio" at "http://www.ibiblio.org/maven2")
  
  
// --------- Web plugin settings ---------
seq(webSettings :_*)

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

// --------- FindBugs plugin settings ---------

seq(de.johoop.findbugs4sbt.FindBugs.findbugsSettings : _*)

// --------- Proguard plugin settings ---------

seq(ProguardPlugin.proguardSettings :_*)

