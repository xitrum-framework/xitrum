package xitrum.i18n

import scala.util.parsing.combinator.JavaTokenParsers

// http://www.gnu.org/software/hello/manual/gettext/PO-Files.html
object PoParser extends JavaTokenParsers {
  private def mergeStrs(quoteds: List[String]): String = {
    // Removes the first and last quote (") character of strings
    // and concats them
    val unquoted = quoteds.foldLeft("") { (acc, quoted) =>
      acc + quoted.substring(1, quoted.length - 1)
    }

    // Unescape
    unquoted.
    replace("\\n",  "\n").
    replace("\\r",  "\r").
    replace("\\t",  "\t").
    replace("\\\\", "\\")
  }

  // Scala regex is single line by default
  private def comment = rep(regex("^#.*".r))

  private def msgctxt = "msgctxt" ~ rep(stringLiteral) ^^ {
    case _ ~ quoteds => mergeStrs(quoteds)
  }

  private def msgid = "msgid" ~ rep(stringLiteral) ^^ {
    case _ ~ quoteds => mergeStrs(quoteds)
  }

  private def msgidPlural = "msgid_plural" ~ rep(stringLiteral) ^^ {
    case _ ~ quoteds => mergeStrs(quoteds)
  }

  private def msgstr = "msgstr" ~ rep(stringLiteral) ^^ {
    case _ ~ quoteds => mergeStrs(quoteds)
  }

  private def msgstrN = "msgstr[" ~ wholeNumber ~ "]" ~ rep(stringLiteral) ^^ {
    case _ ~ number ~ _ ~ quoteds => (number.toInt, mergeStrs(quoteds))
  }

  private def singular =
    (opt(comment) ~ opt(msgctxt) ~
     opt(comment) ~ msgid ~
     opt(comment) ~ msgstr ~ opt(comment)) ^^ {
    case _ ~ ctxo ~ _ ~ id ~ _ ~ str ~ _ =>
      new Translation(ctxo, id, Array(str))
  }

  private def plural =
    (opt(comment) ~ opt(msgctxt) ~
     opt(comment) ~ msgid ~
     opt(comment) ~ msgidPlural ~
     opt(comment) ~ rep(msgstrN) ~ opt(comment)) ^^ {
    case _ ~ ctxo ~ _ ~ id ~ _ ~ _ ~ _ ~ n_strs ~ _ =>
      val strs = n_strs.sorted.map { case (n, str) => str }
      new Translation(ctxo, id, strs.toArray)
  }

  private def exp = rep(singular | plural)

  def parsePo(po: String): Option[Po] = {
    val parseRet = parseAll(exp, po)
    if (parseRet.successful) {
      val translations = parseRet.get
      val body         = translations.foldLeft(
          Map[(Option[String], String), Array[String]]()) { (acc, t) =>
        val item = (t.ctxo, t.singular) -> t.strs
        acc + item
      }
      Some(new Po(body))
    } else {
      None
    }
  }
}
