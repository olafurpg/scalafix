package scalafix.sbt

import scala.meta.scalahost.sbt.ScalahostSbtPlugin._
import scala.meta.scalahost.sbt.ScalahostSbtPlugin.autoImport._
import scala.meta.scalahost.sbt.ScalahostSbtPlugin

import sbt.Keys._
import sbt.ScopeFilter.ScopeFilter
import sbt._
import sbt.plugins.JvmPlugin

trait ScalafixKeys {
  val scalafix: TaskKey[Unit] = taskKey[Unit]("Run scalafmt")
  val scalafixConfig: SettingKey[Option[File]] =
    settingKey[Option[File]](
      ".scalafix.conf file to specify which scalafix rules should run.")
  val scalafixEnabled: SettingKey[Boolean] =
    settingKey[Boolean]("Is scalafix enabled?")
  val scalafixInternalJar: TaskKey[Option[File]] =
    taskKey[Option[File]]("Path to scalafix-nsc compiler plugin jar.")
}

object ScalafixPlugin extends AutoPlugin with ScalafixKeys {
  object autoImport extends ScalafixKeys
  private val Version = "2\\.(\\d\\d)\\..*".r
  private val scalafixVersion = _root_.scalafix.Versions.version
  private val disabled = sys.props.contains("scalafix.disable")
  private def jar(report: UpdateReport): File =
    report.allFiles
      .find { x =>
        x.getAbsolutePath.matches(
          // publishLocal produces jars with name `VERSION/scalafix-nsc_2.11.jar`
          // while the jars published with publishSigned to Maven are named
          // `scalafix-nsc_2.11-VERSION.jar`
          s".*scalafix-nsc_2.1[12](-$scalafixVersion)?.jar$$")
      }
      .getOrElse {
        throw new IllegalStateException(
          s"Unable to resolve scalafix-nsc compiler plugin. Report: $report"
        )
      }

  private def stub(version: String) = {
    val Version(id) = version
    Project(id = s"scalafix-$id", base = file(s"project/scalafix/$id"))
      .settings(
        description :=
          """Serves as a caching layer for extracting the jar location of the
            |scalafix-nsc compiler plugin. If the dependency was added to all
            |projects, the (slow) update task will be re-run for every project.""".stripMargin,
        // Only needed when using snapshot versions.
        publishLocal := {},
        publish := {},
        publishArtifact := false,
        publishMavenStyle := false, // necessary to support intransitive dependencies.
        scalaVersion := version,
        libraryDependencies := Nil, // remove injected dependencies from random sbt plugins.
//        scalacOptions := Nil, // remove injected options/compiler flags
        libraryDependencies +=
          ("ch.epfl.scala" %% "scalafix-nsc" % scalafixVersion).intransitive()
      )
      .disablePlugins(ScalahostSbtPlugin)
  }
  private val scalafix211 = stub("2.11.8")
  private val scalafix212 = stub("2.12.1")
  private def scalahostAggregateFilter: Def.Initialize[Task[ScopeFilter]] =
    Def.task {
//      val currentProject = Project.extract(state.value).currentProject
//      val projects = inProjects(currentProject.aggregate: _*)
//      println(s"current PROJECT: $currentProject")
//      println(s"current PROJECT: ${currentProject.aggregate}")
//      println(s"PROJECTS: $projects")
      ScopeFilter(
//        projects,
        configurations = inConfigurations(Compile, Test, IntegrationTest)
      )
    }
  private val scalahostSourcepath: Def.Initialize[Task[Seq[Seq[File]]]] =
    Def.taskDyn(sourceDirectories.toTask.all(scalahostAggregateFilter.value))
  private val scalahostClasspath: Def.Initialize[Task[Seq[Classpath]]] =
    Def.taskDyn(fullClasspath.all(scalahostAggregateFilter.value))
  private val scalafixRewrites =
    Project("scalafix-rewrites", file("project/scalafix/rewrites"))
      .settings(
        scalaVersion := "2.11.8",
        resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
        libraryDependencies := Nil, // remove inject dependencies
        mainClass := Some("scalafix.cli.Cli"),
        libraryDependencies += "ch.epfl.scala" %% "scalafix-cli" % scalafixVersion,
        fork in run := true
      )
      .disablePlugins(ScalahostSbtPlugin)

  override def extraProjects: Seq[Project] = Seq(
    scalafix211,
    scalafix212,
    scalafixRewrites
  )

  override def requires = JvmPlugin && ScalahostSbtPlugin
  override def trigger: PluginTrigger = AllRequirements


  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    scalafixConfig := Option(file(".scalafix.conf")).filter(_.isFile)
  )
  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      scalafix := Def.taskDyn {
        val sourcepath =
          scalahostSourcepath.value
            .flatMap(_.map(_.getAbsolutePath))
            .mkString(java.io.File.pathSeparator)
        val classpath =
          scalahostClasspath.value
            .flatMap(_.files.map(_.getAbsolutePath))
            .mkString(java.io.File.pathSeparator)
        val args = List(
          s"--mirror-sourcepath=$sourcepath",
          s"--mirror-classpath=$classpath"
        )
        println(s"ARGS: $args")
        runMain
          .in(Compile)
          .in(scalafixRewrites)
          .toTask(args.mkString(" scalafix.cli.Cli ", " ", ""))
      }.value,
      scalafixInternalJar :=
        Def
          .taskDyn[Option[File]] {
            scalaVersion.value match {
              case Version("11") =>
                Def.task(Option(jar((update in scalafix211).value)))
              case Version("12") =>
                Def.task(Option(jar((update in scalafix212).value)))
              case _ =>
                Def.task(None)
            }
          }
          .value,
      scalafixEnabled in Global := false,
      scalacOptions ++= {
        // scalafix should not affect compilations outside of the scalafix task.
        // The approach taken here is the same as scoverage uses, see:
        // https://github.com/scoverage/sbt-scoverage/blob/45ac49583f5a32dfebdce23b94c5336da4906e59/src/main/scala/scoverage/ScoverageSbtPlugin.scala#L70-L83
        if (!(scalafixEnabled in Global).value) {
          Nil
        } else {
          val config =
            scalafixConfig.value.map { x =>
              if (!x.isFile)
                streams.value.log.warn(s"File does not exist: $x")
              s"-P:scalafix:${x.getAbsolutePath}"
            }
          scalafixInternalJar.value
            .map { jar =>
              Seq(
                Some(s"-Xplugin:${jar.getAbsolutePath}"),
                Some("-Yrangepos"),
                config
              ).flatten
            }
            .getOrElse(Nil)
        }
      }
    )
}
