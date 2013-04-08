package xitrum.view

import xitrum.{Cache, ActionEnv}
import xitrum.routing.Routes
import xitrum.util.Json

/** Support for Knockout.js */
trait Knockout {
  this: ActionEnv =>

  def koApplyBindings(model: AnyRef, syncAction: Class[_ <: ActionEnv], js: String) {
    koApplyBindings(model, None, syncAction, js)
  }

  def koApplyBindings(model: AnyRef, scopeSelector: String, syncAction: Class[_ <: ActionEnv], js: String) {
    koApplyBindings(model, Some(scopeSelector), syncAction, js)
  }

  //----------------------------------------------------------------------------

  private def koApplyBindings(model: AnyRef, scopeSelector: Option[String], syncAction: Class[_ <: ActionEnv], js: String) {
    // jQuery automatically converts Ajax response based on content type header
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector.isEmpty) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + Routes.routes.reverseMappings(syncAction).url() + """', {model: ko.mapping.toJSON(model)}, function(data) {
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
