package scalafix
package nsc

import scala.meta.Defn
import scala.meta.Type
import scala.meta.internal.scalahost.ScalahostPlugin
import scala.meta.internal.scalahost.v1.online.Mirror
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.config.ScalafixConfig
import scalafix.rewrite.ScalafixMirror

import java.io.File

import metaconfig.Configured

class ScalafixNscPlugin(val global: Global) extends Plugin {
  val workingDir = new File(sys.props("user.dir"))
  var config: ScalafixConfig =
    ScalafixConfig.auto(workingDir, None).getOrElse(ScalafixConfig.default)
  var getRewrites: ScalafixMirror => List[Rewrite] = { mirror =>
    ScalafixConfig
      .auto(workingDir, Some(mirror))
      .getOrElse(ScalafixConfig.default)
      .rewrites
  }
  private val scalafixComponent =
    new ScalafixNscComponent(this, global, () => config, getRewrites)
  // IMPORTANT. This needs to happen before we create ScalahostPlugin in order to hijack the
  // original global.analyzer instead of the Scalahost hijacked analyzer.
  // It seems warn-unused-imports still uses the old g.analyzer to collect import infos.
  scalafixComponent.hijackImportInfos()
  // manually configure scalahost plugin. We could force users to pass in -Xplugin/scalahost.jar,
  // but that would complicate integrations and give us less control over in which order
  // hijacking happens.
  private val scalahostPlugin = new ScalahostPlugin(global) // let scalahost hijack global
  global.settings.plugin.appendToValue("scalahost")

  val mirror = new Mirror(global)
  val name = "scalafix"
  val description = "Refactoring tool."
  val component: ScalafixNscComponent = scalafixComponent
  val components: List[PluginComponent] = component :: Nil

  override def init(options: List[String], error: (String) => Unit): Boolean = {
    options match {
      case Nil => true
      case file :: Nil =>
        ScalafixConfig.fromFile(new File(file)) match {
          case Configured.NotOk(msg) =>
            error(msg.toString())
            false
          case Configured.Ok(userConfig) =>
            config = userConfig
            true
        }
      case els =>
        super.init(els, error)
    }
  }
}
