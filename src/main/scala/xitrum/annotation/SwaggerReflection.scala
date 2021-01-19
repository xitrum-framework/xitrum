package xitrum.annotation

import scala.reflect.runtime.universe._

/**
 * Utility functions to reflect scala case classes to structure that can be rendered to Swagger documentation
 */
object SwaggerReflection {
  private val _arraySymbols = Set[Name](
    symbolOf[Seq[_]].name,
    symbolOf[Set[_]].name,
    symbolOf[List[_]].name,
    symbolOf[Array[_]].name
  )

  def isArrayType(tpe: Type): Boolean = _arraySymbols.contains(tpe.typeSymbol.name)

  private[annotation] def reflectField(fld: TermSymbol): Swagger.JsonField = {
    val retType = fld.asMethod.returnType
    val isRequired = retType.typeSymbol != symbolOf[Option[_]]
    val tpe = if (isRequired) retType else retType.typeArgs.head
    Swagger.JsonField(fld.name.toString, reflect(tpe), isRequired)
  }

  private[annotation] def reflectTypeFormat(tpe: Type): (Option[String], String, String) = tpe.typeSymbol match {
    case t if t == definitions.IntClass => (None, Swagger.JsonType.tpe.integer, Swagger.JsonType.fmt.int32)
    case t if t == definitions.LongClass => (None, Swagger.JsonType.tpe.integer, Swagger.JsonType.fmt.int64)
    case t if t == definitions.FloatClass => (None, Swagger.JsonType.tpe.number, Swagger.JsonType.fmt.float)
    case t if t == definitions.DoubleClass => (None, Swagger.JsonType.tpe.number, Swagger.JsonType.fmt.double)
    case t if t == definitions.StringClass => (None, Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.none)
    case t if t == definitions.BooleanClass => (None, Swagger.JsonType.tpe.boolean, Swagger.JsonType.fmt.none)
    case t if t == symbolOf[java.util.Date] => (None, Swagger.JsonType.tpe.string, Swagger.JsonType.fmt.dateTime)
    case _ =>
      var pack = tpe.typeSymbol.owner
      while (!pack.isPackage)
        pack = pack.owner
      (Some(tpe.typeSymbol.fullName.substring(pack.fullName.length + 1)), Swagger.JsonType.tpe.obj, Swagger.JsonType.fmt.none)
  }

  def reflect(tpe: Type): Swagger.JsonType = {
    val isTpeArray = isArrayType(tpe)
    val tip = if (isTpeArray) tpe.typeArgs.head else tpe
    val fields = tip.decls.filter(_.isPublic)
      .filter(_.isTerm).map(_.asTerm)
      .filter(_.isGetter).toList.map(reflectField)
    val (name, tipe, format) = reflectTypeFormat(tip)
    Swagger.JsonType(name, tipe, format, isTpeArray, fields)
  }
}
