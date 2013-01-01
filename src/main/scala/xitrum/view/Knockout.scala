package xitrum.view

import org.fusesource.scalate.filter.CoffeeScriptCompiler

import xitrum.{Cache, Controller}
import xitrum.controller.Action
import xitrum.util.Json

/** Support for Knockout.js */
trait Knockout {
  this: Controller =>

  def koApplyBindingsCs(model: AnyRef, syncAction: Action, cs: String) {
    koApplyBindingsCs(model, None, syncAction, cs)
  }

  def koApplyBindingsCs(model: AnyRef, scopeSelector: String, syncAction: Action, cs: String) {
    koApplyBindingsCs(model, Some(scopeSelector), syncAction, cs)
  }

  def koApplyBindingsJs(model: AnyRef, syncAction: Action, js: String) {
    koApplyBindingsJs(model, None, syncAction, js)
  }

  def koApplyBindingsJs(model: AnyRef, scopeSelector: String, syncAction: Action, js: String) {
    koApplyBindingsJs(model, Some(scopeSelector), syncAction, js)
  }

  //----------------------------------------------------------------------------

  private def koApplyBindingsCs(model: AnyRef, scopeSelector: Option[String], syncAction: Action, cs: String) {
    val js = Cache.getAs[String](cs) match {
      case None =>
        val compiled = CoffeeScriptCompiler.compile(cs).right.get
        Cache.put(cs, compiled)
        compiled

      case Some(compiled) =>
        compiled
    }
    koApplyBindingsJs(model, scopeSelector, syncAction, js)
  }

  private def koApplyBindingsJs(model: AnyRef, scopeSelector: Option[String], syncAction: Action, js: String) {
    // jQuery automatically converts Ajax response based on content type header
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector.isEmpty) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + syncAction.url + """', {model: ko.mapping.toJSON(model)}, function(data) {
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
