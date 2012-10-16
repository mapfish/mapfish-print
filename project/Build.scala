import sbt._

object PrintBuild extends Build {
    lazy val root = Project(id = "mapfish-print",
                            base = file("."))
                            
  val springVersion = SettingKey[String]("spring-version", "The version of Spring.")
}