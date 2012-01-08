var xitrum = {
  antiCSRFToken: function() {
    return $("meta[name='csrf-token']").attr("content");
  },

  urlFor: function(actionClassName, params) {
    var find = function() {
      for (var i = 0; i < XITRUM_ROUTES.length; i++) {
        var xs = XITRUM_ROUTES[i];
        if (xs[1] === actionClassName) return xs[0];
      }
      throw "[urlFor] No route for: " + actionClassName;
    };

    var compiledRoute = find();

    var ret = XITRUM_BASE_URL;
    for (var i = 0; i < compiledRoute.length; i++) {
      var xs       = compiledRoute[i];
      var token    = xs[0];
      var constant = xs[1];
      if (constant) {
        ret += "/" + token;
      } else {
        var s = params[token];
        if (s) {
          ret += "/" + s;
        } else {
          throw "[urlFor] Missing key: " + token + ", for: " + actionClassName;
        }
      }
    }

    if (ret.length === 0) {
      return "/";
    } else {
      return ret;
    }
  },

  postback: function(event) {
    var target1 = $(event.target);

    var confirmMsg = target1.attr("data-confirm");
    if (confirmMsg && !confirm(confirmMsg)) return false;

    var action = target1.attr("action");
    var data   = "";

    // data may come from "extra" data
    // http://api.jquery.com/data/
    var extraParams = target1.data("extra");
    if (extraParams) data = extraParams + "&";

    // or come from extra form
    var extraFormSelector = target1.attr("data-extra");
    if (extraFormSelector) {
      var extraForm = $(extraFormSelector);
      if (extraForm && extraForm[0].tagName === "FORM" && extraForm.valid())
        data = data + extraForm.serialize() + "&";
    }

    // or come from this element itself
    if (target1[0].tagName === "FORM") {
       if (!target1.valid()) return false;
       data = data + target1.serialize();
    }

    target1.hide();
    target1.after('<img src="' + XITRUM_BASE_URL + '/resources/public/xitrum/ajax-loader.gif" />');

    $.ajax({
      type: "POST",
      url: action,
      data: data,
      error: function(jqxhr) {
        var contentType = jqxhr.getResponseHeader('Content-Type');
        if (contentType && contentType.indexOf('javascript') != -1) {
          try {
            eval(jqxhr.responseText);
          } catch (err) {
            alert("Could not connect to server or server error.");
          }
        } else {
          alert("Could not connect to server or server error.");
        }
      },
      complete: function() {
        target1.show();
        target1.next().remove();
      }
    });

    var after = target1.attr("data-after");
    if (after) {
      var f = eval('(function() {' + after + '})');
      f();
    }

    return false;
  },

  flash: function(msg) {
    var div =
      '<div class="flash">' +
        '<a class="flash_close" href="javascript:">Ｘ</a>' +
        '<div class="flash_msg">' + msg + '</div>' +
      '</div>';
    $("#flash").append($(div).hide().fadeIn(1000));
  },

  cometGet: function(channel, lastTimestamp, callback) {
    var url = XITRUM_COMET_GET_URL.replace(":channel", channel).replace(":lastTimestamp", lastTimestamp);
    if (lastTimestamp == 0) url += "?" + Math.random();
    $.ajax({
      type: "GET",
      url: url,
      error: function() {
        // Wait for some time before the next retry
        setTimeout(function() { xitrum.cometGet(channel, lastTimestamp, callback) }, 10000);
      },
      success: function(message) {
        var timestamps = message.timestamps;
        var bodies     = message.bodies;
        var length     = timestamps.length;
        for (var i = 0; i < length; i++) {
          callback(channel, timestamps[i], bodies[i]);
        }
        xitrum.cometGet(channel, timestamps[length - 1], callback);
      }
    });
  },

  isScrollAtBottom: function(selector) {
    return ($(selector).scrollTop() + $(selector).height() === $(selector)[0].scrollHeight);
  },

  scrollToBottom: function(selector) {
    $(selector).scrollTop($(selector)[0].scrollHeight);
  },

  escapeHtml: function(html) {
    return $('<div/>').text(html).html();
  }
};

$(function() {
  $(document).ajaxSend(function(e, req, options) {
    if (options.type != "GET") {
      options.data += (options.data.length > 0 ? "&" : "") + "csrf-token=" + xitrum.antiCSRFToken();
    }
  });

  $(".flash_close").live("click", function(event) {
    var parent = $(event.target).parent();
    parent.fadeOut(1000, function() { parent.remove() });
  });

  // jQuery Validation plugin only works on forms and the forms must be
  // validated once, before any rules("add", rules) can be called
  //
  // We must iterate manually, $("form").validate() only works for the first
  // form, not all forms when there are multiple form in a page
  $("form").each(function(index, form) { $(form).validate() });

  $("[data-postback]").each(function(index, elem) {
    var eventType = $(elem).attr("data-postback");
    $(elem).bind(eventType, xitrum.postback);
  });
});
