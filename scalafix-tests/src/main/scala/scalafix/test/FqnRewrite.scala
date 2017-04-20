package scalafix.test

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.rewrite.Rewrite
import scalafix.rewrite.RewriteCtx
import scalafix.patch.Patch
import scalafix.patch.TreePatch.AddGlobalImport

case object FqnRewrite extends Rewrite {
  override def rewrite(ctx: RewriteCtx): Patch =
    AddGlobalImport(importer"scala.meta._")
}