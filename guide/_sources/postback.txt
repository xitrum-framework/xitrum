Postbacks
=========

There are 2 main use cases of web applications:

* To serve machines: you need to create RESTful APIs for smartphones, web services
  for other web sites.
* To serve human users: you need to create interactive web pages.

As a web framework, Xitrum aims to support you to solve these use cases easily.
To solve the 1st use case, you use :doc:`RESTful actions </restful>`.
To solve the 2nd use case, you can use the Ajax form postback feature in Xitrum.
Please see the following links for the idea about postback:

* http://en.wikipedia.org/wiki/Postback
* http://nitrogenproject.com/doc/tutorial.html

Xitrum's postback feature is inspired by `Nitrogen <http://nitrogenproject.com/>`_.

Layout
------

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          {jsDefaults}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsForView}
        </body>
      </html>
    )
  }

Form
----

Articles.scala

::

  import xitrum.annotation.{GET, POST, First}
  import xitrum.validator._

  @GET("articles/:id")
  class ArticlesShow extends AppAction {
    def execute() {
      val id = param("id")
      val article = Article.find(id)
      respondInlineView(
        <h1>{article.title}</h1>
        <div>{article.body}</div>
      )
    }
  }

  @First  // Force this route to be matched before "show"
  @GET("articles/new")
  class ArticlesNew extends AppAction {
    def execute() {
      respondInlineView(
        <form data-postback="submit" action={url[ArticlesCreate]}>
          <label>Title</label>
          <input type="text" name="title" class="required" /><br />

          <label>Body</label>
          <textarea name="body" class="required"></textarea><br />

          <input type="submit" value="Save" />
        </form>
      )
    }
  }

  @POST("articles")
  class ArticlesCreate extends AppAction {
    def execute() {
      val title   = param("title")
      val body    = param("body")
      val article = Article.save(title, body)

      flash("Article has been saved.")
      jsRedirectTo(show, "id" -> article.id)
    }
  }

When ``submit`` JavaScript event of the form is triggered, the form will be posted back
to ``ArticlesCreate``.

``action`` attribute of ``<form>`` is encrypted. The encrypted URL acts as the anti-CSRF token.

Non-form
--------

Postback can be set on any element, not only form.

An example with link:

::

  <a href="#" data-postback="click" action={postbackUrl[LogoutAction]}>Logout</a>

Clicking the link above will trigger the postback to LogoutAction.

Confirmation dialog
-------------------

If you want to display a confirmation dialog:

::

  <a href="#" data-postback="click"
              action={postbackUrl[LogoutAction]}
              data-confirm="Do you want to logout?">Logout</a>

If the user clicks "Cancel", the postback will not be sent.

Extra params
------------

In case of form element, you can add ``<input type="hidden"...`` to send
extra params with the postback.

For other elements, you do like this:

::

  <a href="#"
     data-postback="click"
     action={postbackUrl[ArticlesDestroy]("id" -> item.id)}
     data-extra="_method=delete"
     data-confirm={"Do you want to delete %s?".format(item.name)}>Delete</a>

You may also put extra params in a separate form:

::

  <form id="myform" data-postback="submit" action={postbackUrl[SiteSearch]}>
    Search:
    <input type="text" name="keyword" />

    <a class="pagination"
       href="#"
       data-postback="click"
       data-extra="#myform"
       action={postbackUrl[SiteSearch]("page" -> page)}>{page}</a>
  </form>

``#myform`` is the jQuery selector to select the form that contains extra params.
