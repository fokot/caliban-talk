ThisBuild / scalaVersion     := "2.13.3"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

val zioVersion = "1.0.3"
val circeVersion = "0.13.0"
val doobieVersion = "0.9.2"
val calibanVersion = "0.9.2"
val sttpVersion = "2.2.9"
val zioConfigVersion = "1.0.0-RC28"

enablePlugins(CodegenPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

val zioTestFramework = new TestFramework("zio.test.sbt.ZTestFramework")

lazy val root = (project in file("."))
  .settings(
    name := "caliban-talk",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-interop-cats" % "2.2.0.1",
      "com.github.ghostdogpr" %% "caliban" % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-client" % calibanVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres-circe" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-quill" % doobieVersion,
      "com.softwaremill.sttp.client" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % sttpVersion,
      "com.softwaremill.sttp.client" %% "circe" % sttpVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "com.dimafeng" %% "testcontainers-scala" % "0.38.4",
      "org.testcontainers" % "postgresql" % "1.14.3",
      "org.flywaydb" % "flyway-core" % "7.0.2",
    ),
    testFrameworks += zioTestFramework
  )