package xitrum.util

object TypeCheck {
  /**
   * Checks if the value is an instance of the class. The class can be a
   * primitive class (java.lang.Integer, java.lang.Long etc.).
   */
  def isInstance(klass: Class[_], value: Any) = {
    // http://docs.oracle.com/javase/7/docs/api/java/lang/Class.html#isPrimitive()
    // http://stackoverflow.com/questions/16825927/classtag-based-pattern-matching-fails-for-primitives
    if (klass.isPrimitive) {
      val vClass = value.getClass
      (vClass == classOf[java.lang.Boolean]   && klass == java.lang.Boolean  .TYPE) ||
      (vClass == classOf[java.lang.Character] && klass == java.lang.Character.TYPE) ||
      (vClass == classOf[java.lang.Byte]      && klass == java.lang.Byte     .TYPE) ||
      (vClass == classOf[java.lang.Short]     && klass == java.lang.Short    .TYPE) ||
      (vClass == classOf[java.lang.Integer]   && klass == java.lang.Integer  .TYPE) ||
      (vClass == classOf[java.lang.Long]      && klass == java.lang.Long     .TYPE) ||
      (vClass == classOf[java.lang.Float]     && klass == java.lang.Float    .TYPE) ||
      (vClass == classOf[java.lang.Double]    && klass == java.lang.Double   .TYPE) ||
      (vClass == classOf[java.lang.Void]      && klass == java.lang.Void     .TYPE)
    } else {
      klass.isInstance(value)
    }
  }
}
