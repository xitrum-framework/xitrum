var xt = {
  postback: function(event) {
    var e = event.target;

    var action1 = $(e).attr("action");
    var action2 = (!action1) ? window.location : action1;

    var form1 = $(e).attr("form");
    var form2 = (!form1) ? e : form1;

    var data = $(form2).serialize() + "&_method=postback";

    $.ajax({
      type: "POST",
//      url:  action2,
      data: data,
      error: function(xhr) {
        alert("Could not connect to server or server error.");
      },
      complete: function() {
        $(form2).show();
        $(form2).next().remove();
      }
    });

    $(form2).hide();
    $(form2).after('<img src="/resources/public/xt/ajax-loader.gif" />');

    return false;
  }
};

$(function() {
  $("[postback]").each(function(i, e) {
    var eventType = $(e).attr("postback");
    $(e).bind(eventType, xt.postback);
  });
});
