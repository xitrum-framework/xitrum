package xitrum

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.Locale

object ComponentTest {
  class A extends Action {
    def execute(): Unit = {}
  }

  class C extends Component
}

class ComponentTest extends AnyFlatSpec with Matchers {
  behavior of "Component"

  "newComponent" should "return a component whose language is in sync with the language of its parent action" in {
    import ComponentTest._

    val a = new A
    val c = a.newComponent[C]()

    // Default language should be English
    a.language shouldBe "en"
    c.language shouldBe "en"
    a.locale shouldBe Locale.forLanguageTag("en")
    c.locale shouldBe Locale.forLanguageTag("en")

    a.t("Hello World") shouldBe "Hello World"
    c.t("Hello World") shouldBe "Hello World"
    a.t("Hello %s", "World") shouldBe "Hello World"
    c.t("Hello %s", "World") shouldBe "Hello World"

    // Set a language, c language should be affected
    a.language = "ru"
    c.language shouldBe "ru"
    a.locale shouldBe Locale.forLanguageTag("ru")
    c.locale shouldBe Locale.forLanguageTag("ru")

    // See src/test/resources/i18n/ru.po
    a.t("Hello World") shouldBe "привет мир"
    c.t("Hello World") shouldBe "привет мир"
    a.t("Hello %s", "мир") shouldBe "привет мир"
    c.t("Hello %s", "мир") shouldBe "привет мир"

    // Set c language, a language should be affected
    c.language = "ja"
    a.language shouldBe "ja"
    a.locale shouldBe Locale.forLanguageTag("ja")
    c.locale shouldBe Locale.forLanguageTag("ja")

    // t for unknown language should take no effect (there's no ja.po)
    a.t("Hello World") shouldBe "Hello World"
    c.t("Hello World") shouldBe "Hello World"
    a.t("Hello %s", "World") shouldBe "Hello World"
    c.t("Hello %s", "World") shouldBe "Hello World"
  }
}
