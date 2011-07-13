var xitrum = {
  postback: function(event) {
    var target1 = $(event.target);

    var confirmMsg = target1.attr("confirm");
    if (confirmMsg && !confirm(confirmMsg)) return false;

    var action = target1.attr("action");
    var data   = "";

    // data may come from "extra" data
    // http://api.jquery.com/data/
    var extraParams = target1.data("extra");
    if (extraParams) data = extraParams + "&";

    // or come from extra form
    var extraFormSelector = target1.attr("extra");
    if (extraFormSelector) {
      var extraForm = $(extraFormSelector);
      if (extraForm && extraForm[0].tagName == "FORM" && extraForm.valid())
        data = data + extraForm.serialize() + "&";
    }

    // or come from this element itself
    if (target1[0].tagName == "FORM") {
       if (!target1.valid()) return false;
       data = data + target1.serialize();
    }

    target1.hide();
    target1.after('<img src="' + XITRUM_BASE_URI + '/resources/public/xitrum/ajax-loader.gif" />');

    $.ajax({
      type: "POST",
      url: action,
      data: data,
      error: function() {
        alert("Could not connect to server or server error.");
      },
      complete: function() {
        target1.show();
        target1.next().remove();
      }
    });

    return false;
  },

  flash: function(msg) {
    var div =
      '<div class="flash">' +
        '<a class="flash_close" href="javascript:">Ｘ</a>' +
        '<div class="flash_msg">' + msg + '</div>' +
      '</div>';
    $("#flash").append($(div).hide().fadeIn(1000));
  }
};

$(function() {
  $(".flash_close").live("click", function(event) {
    var parent = $(event.target).parent();
    parent.fadeOut(1000, function() { parent.remove() });
  });

  // jQuery Validation plugin only works on forms and the form must be validated once
  $("form").validate();

  $("[postback]").each(function(i, e) {
    var eventType = $(e).attr("postback");
    $(e).bind(eventType, xitrum.postback);
  });
});
