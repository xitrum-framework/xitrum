var xitrum = {
  postback: function(event) {
    var e = $(event.target);

    var confirmMsg = e.attr("confirm");
    if (confirmMsg && !confirm(confirmMsg)) return false;

    var action1 = e.attr("action");
    var action2 = (!action1) ? window.location.href : action1;

    var form1 = e.attr("form");
    var form2 = (!form1) ? e : $("#" + form1);

    var data = ""
    if (form2[0].tagName == "FORM") {
       if (!form2.valid()) return false;
       data += form2.serialize();
    }

    $(form2).hide();
    $(form2).after('<img src="/resources/public/xitrum/ajax-loader.gif" />');

    $.ajax({
      type: "POST",
      url:  action2,
      data: data,
      error: function(xhr) {
        alert("Could not connect to server or server error.");
      },
      complete: function(xhr) {
        $(form2).show();
        $(form2).next().remove();
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
