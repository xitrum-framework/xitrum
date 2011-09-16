package xitrum

import scala.collection.mutable.{HashMap => MHashMap, MultiMap, Set => MSet}

import scala.tools.nsc
import nsc.Global
import nsc.Phase
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

// http://www.scala-lang.org/node/140
class Xgettext(val global: Global) extends Plugin {
  import global._

  val name = "scaposer-xgettext"
  val description = "This Scala compiler plugin extracts and creates gettext.pot file"
  val components = List[PluginComponent](MapComponent, ReduceComponent)

  val msgidToLines = new MHashMap[String, MSet[(String, Int)]] with MultiMap[String, (String, Int)]
  var reduced = false

  private object MapComponent extends PluginComponent {
    val global: Xgettext.this.global.type = Xgettext.this.global

    val runsAfter = List("refchecks")

    val phaseName = "scaposer-xgettext-map"

    def newPhase(_prev: Phase) = new MapPhase(_prev)

    class MapPhase(prev: Phase) extends StdPhase(prev) {
      override def name = phaseName

      def apply(unit: CompilationUnit) {
        for (tree @ Apply(Select(x1, x2), list) <- unit.body) {
          if ((x1.tpe <:< definitions.getClass("xitrum.Action").tpe) && x2.toString == "t") {
            val pos = tree.pos  // scala.tools.nsc.util.OffsetPosition

            val msgid = list(0)
            msgidToLines.addBinding(msgid.toString, (relPath(pos.source.path), pos.line))
          }
        }
      }

      private def relPath(absPath: String) = {
        val curDir   = System.getProperty("user.dir")
        val relPath  = absPath.substring(curDir.length)
        val unixPath = relPath.replace("\\", "/")  // Windows uses '\' to separate
        ".." + unixPath  // po file will be put in src/main/resources/i18n directory
      }
    }
  }


  private object ReduceComponent extends PluginComponent {
    val global: Xgettext.this.global.type = Xgettext.this.global

    val runsAfter = List("jvm")

    val phaseName = "scaposer-xgettext-reduce"

    def newPhase(_prev: Phase) = new ReducePhase(_prev)

    class ReducePhase(prev: Phase) extends StdPhase(prev) {
      override def name = phaseName

      def apply(unit: CompilationUnit) {
        if (!reduced) {
          for ((msgid, lines) <- msgidToLines) {
            for ((srcPath, lineNo) <- lines) {
              println("#: " + srcPath + ":" + lineNo)
            }
            println("msgid: " + msgid)
            println("msgstr: \"\"")
            println
          }
          reduced = true
        }
      }
    }
  }
}
