var xt = {
  post2: function(event) {
    var e = $(event.target);

    var action1 = e.attr("action");
    var action2 = (!action1) ? window.location.href : action1;

    var form1 = e.attr("form");
    var form2 = (!form1) ? e : $("#" + form1);

    var data = ""

    if (form2[0].tagName == "FORM") {
       if (!form2.valid()) return false;
       data += form2.serialize() + "&";
    }

    $(form2).hide();
    $(form2).after('<img src="/resources/public/xitrum/ajax-loader.gif" />');

    data += "_method=post2" + "&_csrf_token=" + xt_csrf_token;
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
  }
};

$(function() {
  // jQuery Validation plugin only works on forms and the form must be validated once
  $("form").validate();

  $("[post2]").each(function(i, e) {
    var eventType = $(e).attr("post2");
    $(e).bind(eventType, xt.post2);
  });
});
