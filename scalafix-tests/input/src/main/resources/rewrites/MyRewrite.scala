package foo.bar

import scalafix._
import scala.meta._

case object MyRewrite extends Rewrite {
  def rewrite(ctx: RewriteCtx): Patch = {
    val custom =
      ctx.config.x.dynamic.custom.asConf
        .map(_.asInstanceOf[Conf.Bool].value)
        .get
    assert(custom, "Expected `x.custom = true` in the configuration!")
    ctx.tree.collect {
      case n: scala.meta.Name => ctx.replaceTree(n, n.syntax + "1")
    }.asPatch
  }
}

case class MyRewrite2(sctx: SemanticCtx) extends SemanticRewrite(sctx) {
  def rewrite(ctx: RewriteCtx): Patch =
    ctx.addGlobalImport(importer"scala.collection.immutable.Seq")
}
