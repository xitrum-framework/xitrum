Postbacks
=========

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s2.jpg

Please see the following links for the idea about postback:

* http://en.wikipedia.org/wiki/Postback
* http://nitrogenproject.com/doc/tutorial.html

Xitrum supports Ajax form postback, with additional features:

* `Anti-CSRF <http://guides.rubyonrails.org/security.html#cross-site-request-forgery-csrf>`_
* :doc:`Validation </validation>`

Layout
------

AppAction.scala

::

  import xitrum.Action
  import xitrum.view.DocType

  trait AppAction extends Action {
    override def layout = DocType.xhtmlTransitional(
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

ArticleShow.scala

::

  import xitrum.annotation.GET

  @GET("/articles/:id")
  class ArticleShow extends AppAction {
    override def execute {
      val id = param("id")
      val article = Article.find(id)
      renderView(
        <h1>{article.title}</h1>
        <div>{article.body}</div>
      )
    }
  }

ArticleNew.scala

::

  import xitrum.annotation.{First, GET}
  import xitrum.validation._

  // @First: force this route to be matched before "/articles/:id"
  @First
  @GET("/articles/new")
  class ArticleNew extends AppAction {
    override def execute {
      renderView(
        <form postback="submit" action={urlForPostbackThis}>
          <label>Title</label>
          <input type="text" name={validate("title", Required)} /><br />

          <label>Body</label>
          <textarea name={validate("body", Required)}></textarea><br />

          <input type="submit" value="Save" />
        </form>
      )
    }

    override def postback {
      val title   = param("title")
      val body    = param("body")
      val article = Article.save(title, body)

      flash("Article has been saved.")
      jsRedirectTo[ArticleShow]("id" -> article.id)
    }
  }

When ``submit`` JavaScript event of the form is triggered, the form will be posted back
to the current Xitrum action.

``action`` attribute of ``<form>`` is encrypted. The encrypted URL acts as the anti-CSRF token.

Non-form
--------

Postback can be set on any element, not only form.

An example with link:

::

  <a href="#" postback="click" action={urlForPostback[LogoutAction]}>Logout</a>

Clicking the link above will trigger the postback to LogoutAction.

Confirmation dialog
-------------------

If you want to display a confirmation dialog:

::

  <a href="#" postback="click"
              action={urlForPostback[LogoutAction]}
              confirm="Do you want to logout?">Logout</a>

If the user clicks "Cancel", the postback will not be sent.

Extra params
------------

In case of form element, you can add ``<input type="hidden"...`` to send
extra params with the postback.

For other elements, you do like this:

::

  <a href="#"
     postback="click"
     action={urlForPostbackThis("itemId" -> item.id)}
     confirm={"Do you want to delete %s?".format(item.name)}>Delete</a>

You may also put extra params in a separate form:

::

  <form id="myform" postback="submit" action={urlForPostbackThis}>
    Search:
    <input type="text" name={validate("keyword")} />

    <a class="pagination"
       href="#"
       postback="click"
       extra="#myform"
       action={urlForPostbackThis("page" -> page)}>{page}</a>
  </form>

``#myform`` is the jQuery selector to select the form that contains extra params.
