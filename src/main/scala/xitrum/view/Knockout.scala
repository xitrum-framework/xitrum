package xitrum.view

import xitrum.{Action, Config}
import xitrum.util.{CoffeeScriptCompiler, Json}

/** Support for Knockout.js */
trait Knockout {
  this: Action =>

  def koApplyBindingsCs(model: AnyRef, syncActionClass: Class[_ <: Action], cs: String) {
    koApplyBindingsCs(model, None, syncActionClass, cs)
  }

  def koApplyBindingsCs(model: AnyRef, scopeSelector: String, syncActionClass: Class[_ <: Action], cs: String) {
    koApplyBindingsCs(model, Some(scopeSelector), syncActionClass, cs)
  }

  def koApplyBindingsJs(model: AnyRef, syncActionClass: Class[_ <: Action], js: String) {
    koApplyBindingsJs(model, None, syncActionClass, js)
  }

  def koApplyBindingsJs(model: AnyRef, scopeSelector: String, syncActionClass: Class[_ <: Action], js: String) {
    koApplyBindingsJs(model, Some(scopeSelector), syncActionClass, js)
  }

  //----------------------------------------------------------------------------

  private def koApplyBindingsCs(model: AnyRef, scopeSelector: Option[String], syncActionClass: Class[_ <: Action], cs: String) {
    val js = CoffeeScriptCompiler.compile(cs).get
    koApplyBindingsJs(model, scopeSelector, syncActionClass, js)
  }

  private def koApplyBindingsJs(model: AnyRef, scopeSelector: Option[String], syncActionClass: Class[_ <: Action], js: String) {
    // jQuery automatically converts Ajax response based on content type header
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector.isEmpty) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + Config.routesReverseMappings(syncActionClass).url(Map()) + """', {model: ko.mapping.toJSON(model)}, function(data) {
          if (typeof(data) === 'object') {
            model = ko.mapping.fromJS(data);
            ko.applyBindings(model);
          }
        });
        return false;
      };
      var syncIfValid = function(formSelector) {
        return (function() {
          if (formSelector.valid()) sync();
          return false;
        });
      };"""
    jsAddToView(
      "(function () {\n" +
        prepareModel +
        prepareSync +
        js +
      "})();"
    )
  }
}
