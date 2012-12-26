package xitrum.view

import org.fusesource.scalate.filter.CoffeeScriptCompiler

import xitrum.Controller
import xitrum.controller.Action
import xitrum.util.Json

/** Support for Knockout.js */
trait Knockout {
  this: Controller =>

  def koApplyBindingsCs(model: AnyRef, syncAction: Action, cs: String) {
    val js = CoffeeScriptCompiler.compile(cs).right.get
    koApplyBindingsJs(model, syncAction, js)
  }

  def koApplyBindingsCs(model: AnyRef, scopeSelector: String, syncAction: Action, cs: String) {
    val js = CoffeeScriptCompiler.compile(cs).right.get
    koApplyBindingsJs(model, scopeSelector, syncAction, js)
  }

  def koApplyBindingsJs(model: AnyRef, syncAction: Action, js: String) {
    koApplyBindingsJs(model, null, syncAction, js)
  }

  def koApplyBindingsJs(model: AnyRef, scopeSelector: String, syncAction: Action, js: String) {
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector == null) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + syncAction.url + """', {model: ko.mapping.toJSON(model)}, function(data) {
          // jQuery automatically detects and converts the response based on the content type header
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
