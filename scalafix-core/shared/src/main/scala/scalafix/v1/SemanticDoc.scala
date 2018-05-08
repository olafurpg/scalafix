package scalafix.v1

import java.nio.file.Files
import java.util
import org.langmeta.io.Classpath
import org.langmeta.io.RelativePath
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scalafix.internal.util.SymbolTable
import scalafix.util.MatchingParens
import scalafix.util.TokenList
import scala.meta.internal.{semanticdb3 => s}
import scalafix.internal.rule.DeprecatedPatchOps
import scalafix.internal.util.TreeOps
import scalafix.internal.rule.DocOps._

final class SemanticDoc private[scalafix] (
    val doc: Doc,
    // privates
    private[scalafix] val sdoc: s.TextDocument,
    private[scalafix] val symtab: SymbolTable
) extends DeprecatedPatchOps {

  // =============
  // Syntactic API
  // =============
  def tree: Tree = doc.tree
  def tokens: Tokens = doc.tokens
  def input: Input = doc.input
  def matching: MatchingParens = doc.matching
  def tokenList: TokenList = doc.tokenList
  def comments: AssociatedComments = doc.comments

  // ============
  // Semantic API
  // ============
  def symbol(tree: Tree): Sym = {
    val pos = TreeOps.symbolPos(tree)
    val result = occurrences.getOrDefault(
      s.Range(
        startLine = pos.startLine,
        startCharacter = pos.startColumn,
        endLine = pos.endLine,
        endCharacter = pos.endColumn
      ),
      Nil
    )
    if (result.isEmpty) Sym.None
    else Sym(result.head) // Discard multi symbols
  }

  def info(tree: Tree): Sym.Info = info(symbol(tree))
  def info(sym: Sym): Sym.Info = {
    if (sym.isNone) {
      Sym.Info.empty
    } else if (sym.isLocal) {
      new Sym.Info(locals.getOrElse(sym.value, s.SymbolInformation()))
    } else {
      new Sym.Info(symtab.info(sym.value).getOrElse(s.SymbolInformation()))
    }
  }

  // ========
  // Privates
  // ========
  private[scalafix] val locals = sdoc.symbols.iterator.collect {
    case info
        if info.symbol.startsWith("local") ||
          info.symbol.contains("$anon") // NOTE(olafur) workaround for a semanticdb-scala issue.
        =>
      info.symbol -> info
  }.toMap

  private[scalafix] val occurrences: util.Map[s.Range, Seq[String]] = {
    val result = new util.HashMap[s.Range, ListBuffer[String]]()
    sdoc.occurrences.foreach { o =>
      if (o.range.isDefined) {
        val key = o.range.get
        var buffer = result.get(key)
        if (buffer == null) {
          buffer = ListBuffer.empty[String]
          result.put(key, buffer)
        }
        buffer += o.symbol
      }
    }
    result.asInstanceOf[util.Map[s.Range, Seq[String]]]
  }

}

object SemanticDoc {
  def fromPath(
      path: RelativePath,
      classpath: Classpath,
      symtab: SymbolTable,
      dialect: Dialect
  ): SemanticDoc = {
    val reluri = path.toRelativeURI.toString
    classpath.resolveSemanticdb(path) match {
      case Some(abspath) =>
        val in = Files.newInputStream(abspath.toNIO)
        val sdocs =
          try s.TextDocuments.parseFrom(in).documents
          finally in.close()
        val sdoc = sdocs.find(_.uri == reluri).getOrElse {
          throw new IllegalArgumentException(
            s"No TextDocument for relative path $reluri in $abspath")
        }
        val input = Input.VirtualFile(abspath.toString(), sdoc.text)
        val tree = (dialect, input).parse[Source].get
        val doc = Doc(tree)
        new SemanticDoc(doc, sdoc, symtab)
      case _ =>
        throw new IllegalArgumentException(
          s"No SemanticDB $path in classpath $classpath")
    }
  }
}
