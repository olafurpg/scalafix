package scalafix.internal.rule

import scala.meta.Importee
import scala.meta.Importer
import scala.meta.Symbol
import scala.meta.Token
import scala.meta.Tree
import scala.meta.tokens.Tokens
import scalafix.LintMessage
import scalafix.Patch
import scalafix.patch.PatchOps
import scalafix.util.SemanticdbIndex
import DeprecatedPatchOps.DeprecationMessage
import scalafix.v1.SemanticDoc

trait DeprecatedPatchOps extends PatchOps {
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeImportee(importee: Importee): Patch =
    Patch.removeImportee(importee)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addGlobalImport(importer: Importer): Patch =
    Patch.addGlobalImport(importer)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceToken(token: Token, toReplace: String): Patch =
    Patch.replaceToken(token, toReplace)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeTokens(tokens: Tokens): Patch =
    Patch.removeTokens(tokens)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeTokens(tokens: Iterable[Token]): Patch =
    Patch.removeTokens(tokens)
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeToken(token: Token): Patch =
    Patch.removeToken(token)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceTree(from: Tree, to: String): Patch =
    Patch.replaceTree(from, to)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addRight(tok: Token, toAdd: String): Patch =
    Patch.addRight(tok, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addRight(tree: Tree, toAdd: String): Patch =
    Patch.addRight(tree, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addLeft(tok: Token, toAdd: String): Patch =
    Patch.addLeft(tok, toAdd)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addLeft(tree: Tree, toAdd: String): Patch =
    Patch.addLeft(tree, toAdd)

  // Semantic patch ops.
  @deprecated(DeprecationMessage, "0.6.0")
  final def removeGlobalImport(symbol: Symbol)(
      implicit doc: SemanticDoc): Patch =
    Patch.removeGlobalImport(symbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def addGlobalImport(symbol: Symbol)(implicit doc: SemanticDoc): Patch =
    Patch.addGlobalImport(symbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit doc: SemanticDoc): Patch =
    Patch.replaceSymbol(fromSymbol, toSymbol)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbols(toReplace: (String, String)*)(
      implicit doc: SemanticDoc): Patch =
    Patch.replaceSymbols(toReplace: _*)
  @deprecated(DeprecationMessage, "0.6.0")
  final def replaceSymbols(toReplace: Seq[(String, String)])(
      implicit noop: DummyImplicit,
      doc: SemanticDoc): Patch =
    Patch.replaceSymbols(toReplace)
  @deprecated(DeprecationMessage, "0.6.0")
  final def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit doc: SemanticDoc): Patch =
    Patch.renameSymbol(fromSymbol, toName)
  @deprecated(DeprecationMessage, "0.6.0")
  final def lint(msg: LintMessage): Patch =
    Patch.lint(msg)
}

object DeprecatedPatchOps {
  private[scalafix] final val DeprecationMessage = "Use scalafix.Patch instead"
}