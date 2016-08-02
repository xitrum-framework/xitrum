package xitrum

import java.util.Locale

import org.scalatest.{FlatSpec, Matchers}

object ComponentTest {
  class A extends Action { def execute() {} }
  class C extends Component
}

class ComponentTest extends FlatSpec with Matchers {
  behavior of "Component"

  "newComponent" should "return a component whose language is in sync with the language of its parent action" in {
    import ComponentTest._

    val a = new A
    val c = a.newComponent[C]()

    // Default language should be English
    a.t("Hello World") shouldBe "Hello World"
    c.t("Hello World") shouldBe "Hello World"

    a.locale = Locale.forLanguageTag("ru")
    c.locale shouldBe Locale.forLanguageTag("ru")

    // See src/test/resources/i18n/ru.po
    a.t("Hello World") shouldBe "привет мир"
    c.t("Hello World") shouldBe "привет мир"

    c.locale = Locale.JAPAN
    a.locale shouldBe Locale.JAPAN

    // Unknown language should take no effect
    a.t("Hello World") shouldBe "Hello World"
    c.t("Hello World") shouldBe "Hello World"
  }
}
