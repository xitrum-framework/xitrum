package xitrum.view

import com.codahale.jerkson.Json
import xitrum.Action

/** Support for Knockout.js */
trait Knockout {
  this: Action =>

  def koApplyBindings(model: Any, syncPath: String, js: String) {
    koApplyBindings(model, null, syncPath, js)
  }

  def koApplyBindings(model: Any, scopeSelector: String, syncPath: String, js: String) {
    val prepareModel =
      "var model = ko.mapping.fromJS(" + Json.generate(model) + ");\n" +
      (if (scopeSelector == null) "ko.applyBindings(model);\n" else "ko.applyBindings(model, " + scopeSelector + "[0]);\n")
    val prepareSync =
      "var sync = function() {\n" +
        "$.post('" + syncPath + """', {model: ko.mapping.toJSON(model)}, function(data) {
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
