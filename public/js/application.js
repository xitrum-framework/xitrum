$(function() {
  $("textarea.tinymce").tinymce({
    // Location of TinyMCE script
    script_url: "/public/tinymce-3.3.8/tiny_mce.js",

    // General options
    theme: "advanced",
    plugins: "pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,inlinepopups,insertdatetime,preview,media,searchreplace,print,contextmenu,paste,directionality,fullscreen,noneditable,visualchars,nonbreaking,xhtmlxtras,template,advlist",

    // Theme options
    theme_advanced_buttons1: "bold,italic,underline,strikethrough,|,cite,abbr,acronym,del,ins,|,justifyleft,justifycenter,justifyright,justifyfull,formatselect",
    theme_advanced_buttons2: "pastetext,pasteword,|,search,replace,|,bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,code,|,preview,|,forecolor,backcolor",
    theme_advanced_buttons3: "tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,iespell,media,advhr,|,fullscreen",
    theme_advanced_toolbar_location: "top",
    theme_advanced_toolbar_align: "left",
    theme_advanced_statusbar_location: "bottom",
    theme_advanced_resizing: true,
  });
});
