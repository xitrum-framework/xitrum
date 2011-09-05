package xitrum.i18n

// msgid_plural in po file is for translator.
// We do not need to store it here because it is
// available in the translation methods of class Po.
class Translation(
  val ctxo:     Option[String],
  val singular: String,
  val strs:     Array[String])
