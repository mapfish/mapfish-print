name := "print"

version := "1.2-SNAPSHOT"

organization := "org.mapfish.print"

logLevel := Level.Debug

ivyLoggingLevel := UpdateLogging.Full

resolvers := Seq(
  Resolver.file("localRepo", file(System.getProperty("user.home")+"/.m2/repository"))(Resolver.mavenStylePatterns), 
  MavenRepository("mapfishRepo", "http://dev.mapfish.org/maven/repository"),
  MavenRepository("ibiblio", "http://www.ibiblio.org/maven2"), 
  MavenRepository("geotools","http://download.osgeo.org/webdav/geotools")
)

libraryDependencies ++= Seq(
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
	"org.apache.pdfbox" % "pdfbox" % "1.2.1",
	"javax.media" % "jai_core" % "1.1.3",
	"javax.media" % "jai_imageio" % "1.1",
	"javax.media" % "jai_codec" % "1.1.3",
	"org.apache.xmlgraphics" % "batik-transcoder" % "1.7"
)

libraryDependencies += "javax.servlet" % "servlet-api" % "2.5"

libraryDependencies += "junit" % "junit" % "4.7" % "test"
