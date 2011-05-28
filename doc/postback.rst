Writing POSTbacks
=================

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s2.jpg

Please see the following links for the idea about POSTback:

* http://en.wikipedia.org/wiki/Postback
* http://nitrogenproject.com/doc/tutorial.html

Xitrum supports Ajax form postback with additional features:

* Anti CSRF token, as in Rails
* Validation, as in Nitrogen

If you are loyal to this style, for web sites without APIs, you only need to
annotate GET routes. You only need to do SEO on GET routes because these are
the only pages that search engines follow.

::

  // first: This route will be matched before others, like /articles/:id
  @GET(value="/articles/new", first=true)
  class ArticleNewCreate extends Action {
    beforeFilters("authenticate") = () => session.contains("user")

    override def execute {
      renderView(
        <form post2="submit" action={urlForPostback[ArticleCreate]}>  <-- The URL is encrypted, the encrypted URL acts like an anti CSRF token
          Title:
          {<input type="text" name="title" />.validate(new Required)}<br />

          Body:
          {textarea name="body"></textarea>.validate(new Required)}<br />

          <input type="submit" value="OK" />
        </form>
      )
    }

    override def postback {
      val title = param("title")
      val body  = param("body")
      val user  = session("user").asInstanceOf[User]

      Article.save(user.id, title, body)
      jsRedirectTo[ArticleIndex]
    }
  }

To make a postback, you need to know:

* The event that triggers postback
* The element where the event occurs
* The action destination on the server
* Parameters to send with the postback

General case
------------

::

  <tag1 id="form">
    <!-- form elements in this tag will be posted back -->
  </tag1>

  <tag2 postback="event" action="/url/to/action" form="form" />

Special case 1: action is the same with the current page URL
------------------------------------------------------------

You can leave out "action".

::

  <tag1 id="form">
    <!-- form elements in this tag will be posted back -->
  </tag1>

  <tag2 postback="event" form ="form" />

Special case 2: tag1 and tag2 are the same
------------------------------------------

::

  <tag1 postback="event" action="/url/to/action">
    <!-- form elements in this tag will be posted back -->
  </tag1>

Special case 3: combination of the above
----------------------------------------

::

  <tag1 postback="event">
    <!-- form elements in this tag will be posted back -->
  </tag1>
