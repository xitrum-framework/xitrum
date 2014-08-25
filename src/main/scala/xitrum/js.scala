package xitrum

import xitrum.annotation.{First, GET}

object js {
  val body =
"""var xitrum = {
  withBaseUrl: function(path) {
    var baseUrl = '""" + Config.baseUrl + """';

    if (baseUrl.length == 0) {
      if (path.length == 0) return '/';
      if (path.indexOf('/') == 0) return path;
      return '/' + path;
    } else {
      if (path.length == 0) return baseUrl;
      if (path.indexOf('/') == 0) return baseUrl + path;
      return baseUrl + '/' + path;
    }
  },

  ajaxLoadingImg: null,

  antiCsrfToken: function() {
    return $("meta[name='csrf-token']").attr('content');
  },

  postback: function(event) {
    var target1 = $(event.target);

    var confirmMsg = target1.attr('data-confirm');
    if (confirmMsg && !confirm(confirmMsg)) return false;

    var action = target1.attr('action');
    var data   = '';

    // data may come from "data-params"
    // http://api.jquery.com/data/
    var params = target1.data('params');
    if (params) data = params + '&';

    // or come from "data-form"
    var formSelector = target1.attr('data-form');
    if (formSelector) {
      var form = $(formSelector);
      if (form && form[0].tagName === 'FORM' && form.valid())
        data = data + form.serialize() + '&';
    }

    // or come from this element itself
    if (target1[0].tagName === 'FORM') {
       if (!target1.valid()) return false;
       data = data + target1.serialize();
    }

    // Hide the inputs to avoid user double submit
    target1.hide();

    // Display Ajax loading (animation) image if any
    if (this.ajaxLoadingImg != null)
      target1.after('<img src="' + ajaxLoadingImg + '" />');

    $.ajax({
      type: 'POST',
      // Need to set explicitly because sometimes jQuery sets it to "text/plain"
      contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
      url: action,
      data: data,
      error: function(jqxhr) {
        var contentType = jqxhr.getResponseHeader('Content-Type');
        if (contentType && contentType.indexOf('javascript') != -1) {
          try {
            eval(jqxhr.responseText);
          } catch (err) {
            alert('Could not connect to server or server error.');
          }
        } else {
          alert('Could not connect to server or server error.');
        }
      },
      complete: function() {
        target1.show();

        if (this.ajaxLoadingImg != null)
          target1.next().remove();
      }
    });

    var after = target1.attr('data-after');
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
    $('#flash').append($(div).hide().fadeIn(1000));
  },

  isScrollAtBottom: function(selector) {
    return ($(selector).scrollTop() + $(selector).height() === $(selector)[0].scrollHeight);
  },

  scrollToBottom: function(selector) {
    $(selector).scrollTop($(selector)[0].scrollHeight);
  },

  appendAndScroll: function(selector, text) {
    var wasScrollAtBottom = this.isScrollAtBottom(selector);
    $(selector).append(text);
    if (wasScrollAtBottom) this.scrollToBottom(selector);
  },

  escapeHtml: function(html) {
    return $('<div/>').text(html).html();
  }
};

$(function() {
  // Set CSRF token to header for all non-GET Ajax requests
  $(document).ajaxSend(function(e, req, options) {
    if (options.type != 'GET') {
      var token = xitrum.antiCsrfToken();
      if (token) req.setRequestHeader('X-CSRF-Token', token);
    }
  });

  $(document).on('click', '.flash_close', function(event) {
    var parent = $(event.target).parent();
    parent.fadeOut(1000, function() { parent.remove(); });
  });

  // jQuery Validation plugin only works on forms and the forms must be
  // validated once, before any rules('add', rules) can be called
  //
  // We must iterate manually, $('form').validate() only works for the first
  // form, not all forms when there are multiple form in a page
  $('form').each(function(index, form) { $(form).validate(); });

  $('[data-postback]').each(function(index, elem) {
    var eventType = $(elem).attr('data-postback');
    $(elem).bind(eventType, xitrum.postback);
  });
});"""
}

/** To innclude xitrum.js in your view, use: url[xitrum.js]. */
@First
@GET("xitrum/xitrum.js")
class js extends FutureAction {
  def execute() {
    setClientCacheAggressively()  // See Url#url[xitrum.js]
    respondJs(js.body)
  }
}
