package xitrum.view

import com.codahale.jerkson.Json
import org.fusesource.scalate.filter.CoffeeScriptCompiler

import xitrum.Controller
import xitrum.routing.Route

/** Support for Knockout.js */
trait Knockout {
  this: Controller =>

  def koApplyBindingsCs(model: Any, syncRoute: Route, cs: String) {
    val js = CoffeeScriptCompiler.compile(cs).right.get
    koApplyBindingsJs(model, syncRoute, js)
  }

  def koApplyBindingsCs(model: Any, scopeSelector: String, syncRoute: Route, cs: String) {
    val js = CoffeeScriptCompiler.compile(cs).right.get
    koApplyBindingsJs(model, scopeSelector, syncRoute, js)
  }

  def koApplyBindingsJs(model: Any, syncRoute: Route, js: String) {
    koApplyBindingsJs(model, null, syncRoute, js)
  }

  def koApplyBindingsJs(model: Any, scopeSelector: String, syncRoute: Route, js: String) {
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector == null) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + syncRoute.url + """', {model: ko.mapping.toJSON(model)}, function(data) {
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
