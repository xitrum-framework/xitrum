Postbacks
=========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s2.jpg

Please see the following links for the idea about postback:

* http://en.wikipedia.org/wiki/Postback
* http://nitrogenproject.com/doc/tutorial.html

Xitrum's Ajax form postback is inspired by `Nitrogen <http://nitrogenproject.com/>`_.

Layout
------

AppController.scala

::

  import xitrum.Controller
  import xitrum.view.DocType

  trait AppController extends Controller {
    override def layout = DocType.html5(
      <html>
        <head>
          {antiCSRFMeta}
          {xitrumCSS}
          <title>Welcome to Xitrum</title>
        </head>
        <body>
          {renderedView}
          {jsAtBottom}
        </body>
      </html>
    )
  }

Form
----

Articles.scala

::

  import xitrum.validator._

  class Articles extends AppController {
    pathPrefix = "articles"

    val show = GET(":id") {
      val id = param("id")
      val article = Article.find(id)
      respondInlineView(
        <h1>{article.title}</h1>
        <div>{article.body}</div>
      )
    }

    val niw = first.GET("niw") {  // first: force this route to be matched before "show"
      respondInlineView(
        <form data-postback="submit" action={create.url}>
          <label>Title</label>
          <input type="text" name="title" class="required" /><br />

          <label>Body</label>
          <textarea name="body" class="required"></textarea><br />

          <input type="submit" value="Save" />
        </form>
      )
    }

    val create = POST {
      val title   = param("title")
      val body    = param("body")
      val article = Article.save(title, body)

      flash("Article has been saved.")
      jsRedirectTo(show, "id" -> article.id)
    }
  }

When ``submit`` JavaScript event of the form is triggered, the form will be posted back
to ``create``.

``action`` attribute of ``<form>`` is encrypted. The encrypted URL acts as the anti-CSRF token.

Non-form
--------

Postback can be set on any element, not only form.

An example with link:

::

  <a href="#" data-postback="click" action={AuthenticateController.logout.postbackurl}>Logout</a>

Clicking the link above will trigger the postback to logout action of AuthenticateController.

Confirmation dialog
-------------------

If you want to display a confirmation dialog:

::

  <a href="#" data-postback="click"
              action={AuthenticateController.logout.postbackurl}
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
     action={Articles.destroy.url("id" -> item.id)}
     data-extra="_method=delete"
     data-confirm={"Do you want to delete %s?".format(item.name)}>Delete</a>

You may also put extra params in a separate form:

::

  <form id="myform" data-postback="submit" action={Site.search.url}>
    Search:
    <input type="text" name="keyword" />

    <a class="pagination"
       href="#"
       data-postback="click"
       data-extra="#myform"
       action={Site.search.url("page" -> page)}>{page}</a>
  </form>

``#myform`` is the jQuery selector to select the form that contains extra params.
