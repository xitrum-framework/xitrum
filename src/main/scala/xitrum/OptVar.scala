package xitrum

import scala.collection.mutable.{Map => MMap}
import xitrum.util.TypeCheck

/**
 *  @define none `None`
 *  @define some [[scala.Some]]
 *  @define option [[scala.Option]]
 *  @define p `p`
 *  @define f `f`
 *  @define coll option
 *  @define Coll `Option`
 *  @define orderDependent
 *  @define orderDependentFold
 *  @define mayNotTerminateInf
 *  @define willNotTerminateInf
 *  @define collectExample
 *  @define undefinedorder
 *  @define thatinfo the class of the returned collection. In the standard library configuration, `That` is `Iterable[B]`
 *  @define bfinfo an implicit value of class `CanBuildFrom` which determines the result class `That` from the current
 *    representation type `Repr` and the new element type `B`.
 */
abstract class OptVar[+A](implicit m: Manifest[A]) {
  protected[this] val key = getClass.getName

  def getAll(implicit action: Action): MMap[String, Any]

  /**
   * App developer may change type of a SessionVar when modifying his app code. If
   * the session is not reset, the user with old session version will be stuck.
   * He will always see 500 "server error" and won't be able to recover, unless
   * he removes the session cookie.
   *
   * We clear all, instead of just removing the key, to avoid inconsistent app logic,
   * which is worse than the ClassCastException.
   */
  private def clearAllOnClassCastException(maybeA: Any)(implicit action: Action) {
    val rClass = m.runtimeClass
    if (!TypeCheck.isInstance(rClass, maybeA)) {
      action.log.warn(s"Value $maybeA of key $key can't be cast to $rClass, $this is now cleared to try to recover from ClassCastException on next call")
      getAll.clear()
      throw new ClassCastException(s"Value $maybeA of key $key can't be cast to $rClass")
    }
  }

  def get(implicit action: Action): A = {
    val a = getAll(action)(key)
    clearAllOnClassCastException(a)
    a.asInstanceOf[A]
  }

  def set[B >: A](value: B)(implicit action: Action) { getAll.update(key, value) }

  def remove()(implicit action: Action): Option[A] = {
    getAll.remove(key) match {
      case None =>
        None

      case Some(a) =>
        clearAllOnClassCastException(a)
        Some(a.asInstanceOf[A])
    }
  }

  def toOption(implicit action: Action): Option[A] = {
    getAll.get(key) match {
      case None =>
        None

      case Some(a) =>
        clearAllOnClassCastException(a)
        Some(a.asInstanceOf[A])
    }
  }

  //----------------------------------------------------------------------------
  // Methods copied from Option (can't extend Option because it's sealed).

  /** Returns true if the option is $none, false otherwise.
   */
  def isEmpty(implicit action: Action) = !getAll.isDefinedAt(key)

  /** Returns true if the option is an instance of $some, false otherwise.
   */
  def isDefined(implicit action: Action) = getAll.isDefinedAt(key)

  /** Returns the option's value if the option is nonempty, otherwise
   * return the result of evaluating `default`.
   *
   *  @param default  the default expression.
   */
  @inline final def getOrElse[B >: A](default: => B)(implicit action: Action): B =
    if (isEmpty) default else this.get

  /** Returns the option's value if it is nonempty,
   * or `null` if it is empty.
   * Although the use of null is discouraged, code written to use
   * $option must often interface with code that expects and returns nulls.
   * @example {{{
   * val initalText: Option[String] = getInitialText
   * val textField = new JComponent(initalText.orNull,20)
   * }}}
   */
  @inline final def orNull[A1 >: A](implicit ev: Null <:< A1, action: Action): A1 = this getOrElse ev(null)

  /** Returns a $some containing the result of applying $f to this $option's
   * value if this $option is nonempty.
   * Otherwise return $none.
   *
   *  @note This is similar to `flatMap` except here,
   *  $f does not need to wrap its result in an $option.
   *
   *  @param  f   the function to apply
   *  @see flatMap
   *  @see foreach
   */
  @inline final def map[B](f: A => B)(implicit action: Action): Option[B] =
    if (isEmpty) None else Some(f(this.get))

  /** Returns the result of applying $f to this $option's
   *  value if the $option is nonempty.  Otherwise, evaluates
   *  expression `ifEmpty`.
   *
   *  @note This is equivalent to `$option map f getOrElse ifEmpty`.
   *
   *  @param  ifEmpty the expression to evaluate if empty.
   *  @param  f       the function to apply if nonempty.
   */
  @inline final def fold[B](ifEmpty: => B)(f: A => B)(implicit action: Action): B =
    if (isEmpty) ifEmpty else f(this.get)

  /** Returns the result of applying $f to this $option's value if
   * this $option is nonempty.
   * Returns $none if this $option is empty.
   * Slightly different from `map` in that $f is expected to
   * return an $option (which could be $none).
   *
   *  @param  f   the function to apply
   *  @see map
   *  @see foreach
   */
  @inline final def flatMap[B](f: A => Option[B])(implicit action: Action): Option[B] =
    if (isEmpty) None else f(this.get)

  def flatten[B](implicit ev: A <:< Option[B], action: Action): Option[B] =
    if (isEmpty) None else ev(this.get)

  /** Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns true. Otherwise, return $none.
   *
   *  @param  p   the predicate used for testing.
   */
  @inline final def filter(p: A => Boolean)(implicit action: Action): Option[A] =
    if (isEmpty || p(this.get)) toOption else None

  /** Returns this $option if it is nonempty '''and''' applying the predicate $p to
   * this $option's value returns false. Otherwise, return $none.
   *
   *  @param  p   the predicate used for testing.
   */
  @inline final def filterNot(p: A => Boolean)(implicit action: Action): Option[A] =
    if (isEmpty || !p(this.get)) toOption else None

  /** Returns false if the option is $none, true otherwise.
   *  @note   Implemented here to avoid the implicit conversion to Iterable.
   */
  final def nonEmpty(implicit action: Action) = isDefined

  /** Necessary to keep $option from being implicitly converted to
   *  [[scala.collection.Iterable]] in `for` comprehensions.
   */
  @inline final def withFilter(p: A => Boolean)(implicit action: Action): WithFilter = new WithFilter(p)

  /** We need a whole WithFilter class to honor the "doesn't create a new
   *  collection" contract even though it seems unlikely to matter much in a
   *  collection with max size 1.
   */
  class WithFilter(p: A => Boolean)(implicit action: Action) {
    def map[B](f: A => B): Option[B] = toOption filter p map f
    def flatMap[B](f: A => Option[B]): Option[B] = toOption filter p flatMap f
    def foreach[U](f: A => U): Unit = toOption filter p foreach f
    def withFilter(q: A => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
  }

  /** Tests whether the option contains a given value as an element.
   *
   *  @example {{{
   *  // Returns true because Some instance contains string "something" which equals "something".
   *  Some("something") contains "something"
   *
   *  // Returns false because "something" != "anything".
   *  Some("something") contains "anything"
   *
   *  // Returns false when method called on None.
   *  None contains "anything"
   *  }}}
   *
   *  @param elem the element to test.
   *  @return `true` if the option has an element that is equal (as
   *  determined by `==`) to `elem`, `false` otherwise.
   */
  final def contains[A1 >: A](elem: A1)(implicit action: Action): Boolean =
    !isEmpty && this.get == elem

  /** Returns true if this option is nonempty '''and''' the predicate
   * $p returns true when applied to this $option's value.
   * Otherwise, returns false.
   *
   *  @param  p   the predicate to test
   */
  @inline final def exists(p: A => Boolean)(implicit action: Action): Boolean =
    !isEmpty && p(this.get)

  /** Returns true if this option is empty '''or''' the predicate
   * $p returns true when applied to this $option's value.
   *
   *  @param  p   the predicate to test
   */
  @inline final def forall(p: A => Boolean)(implicit action: Action): Boolean = isEmpty || p(this.get)

  /** Apply the given procedure $f to the option's value,
   *  if it is nonempty. Otherwise, do nothing.
   *
   *  @param  f   the procedure to apply.
   *  @see map
   *  @see flatMap
   */
  @inline final def foreach[U](f: A => U)(implicit action: Action) {
    if (!isEmpty) f(this.get)
  }

  /** Returns a $some containing the result of
   * applying `pf` to this $option's contained
   * value, '''if''' this option is
   * nonempty '''and''' `pf` is defined for that value.
   * Returns $none otherwise.
   *
   *  @example {{{
   *  // Returns Some(HTTP) because the partial function covers the case.
   *  Some("http") collect {case "http" => "HTTP"}
   *
   *  // Returns None because the partial function doesn't cover the case.
   *  Some("ftp") collect {case "http" => "HTTP"}
   *
   *  // Returns None because None is passed to the collect method.
   *  None collect {case value => value}
   *  }}}
   *
   *  @param  pf   the partial function.
   *  @return the result of applying `pf` to this $option's
   *  value (if possible), or $none.
   */
  @inline final def collect[B](pf: PartialFunction[A, B])(implicit action: Action): Option[B] =
    if (!isEmpty) pf.lift(this.get) else None

  /** Returns this $option if it is nonempty,
   *  otherwise return the result of evaluating `alternative`.
   *  @param alternative the alternative expression.
   */
  @inline final def orElse[B >: A](alternative: => Option[B])(implicit action: Action): Option[B] =
    if (isEmpty) alternative else toOption

  /** Returns a singleton iterator returning the $option's value
   * if it is nonempty, or an empty iterator if the option is empty.
   */
  def iterator(implicit action: Action): Iterator[A] =
    if (isEmpty) collection.Iterator.empty else collection.Iterator.single(this.get)

  /** Returns a singleton list containing the $option's value
   * if it is nonempty, or the empty list if the $option is empty.
   */
  def toList(implicit action: Action): List[A] =
    if (isEmpty) List() else new ::(this.get, Nil)

  /** Returns a [[scala.util.Left]] containing the given
   * argument `left` if this $option is empty, or
   * a [[scala.util.Right]] containing this $option's value if
   * this is nonempty.
   *
   * @param left the expression to evaluate and return if this is empty
   * @see toLeft
   */
  @inline final def toRight[X](left: => X)(implicit action: Action) =
    if (isEmpty) Left(left) else Right(this.get)

  /** Returns a [[scala.util.Right]] containing the given
   * argument `right` if this is empty, or
   * a [[scala.util.Left]] containing this $option's value
   * if this $option is nonempty.
   *
   * @param right the expression to evaluate and return if this is empty
   * @see toRight
   */
  @inline final def toLeft[X](right: => X)(implicit action: Action) =
    if (isEmpty) Right(right) else Left(this.get)
}
