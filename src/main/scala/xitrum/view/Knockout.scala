package xitrum.view

import xitrum.{Cache, ActionEnv}
import xitrum.util.Json

/** Support for Knockout.js */
trait Knockout {
  this: ActionEnv =>

  def koApplyBindings(model: AnyRef, syncAction: ActionEnv, js: String) {
    koApplyBindings(model, None, syncAction, js)
  }

  def koApplyBindings(model: AnyRef, scopeSelector: String, syncAction: ActionEnv, js: String) {
    koApplyBindings(model, Some(scopeSelector), syncAction, js)
  }

  //----------------------------------------------------------------------------

  private def koApplyBindings(model: AnyRef, scopeSelector: Option[String], syncAction: ActionEnv, js: String) {
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
