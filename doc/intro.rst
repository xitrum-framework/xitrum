Introduction
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s1.jpg

Xitrum ("Smurf" in Vietnamese) is a lightweight Scala web framework and web
server on top of `Netty <http://www.jboss.org/netty>`_:

* It fills the gap between `Scalatra <https://github.com/scalatra/scalatra>`_
  and `Lift <http://liftweb.net/>`_: more powerful than Scalatra but less complex than Lift.
  You can easily define RESTful routings (Scalatra style) and postbacks (Lift style).
* Asynchronous, in the spirit of Netty.
* Typesafe, in the spirit of Scala.
* Stateless, by default session is stored in cookie.

Xitrum is not a full-stack web framework like `Rails <http://rubyonrails.org/>`_
because it does not provide M (in MVC). However, it does provide :doc:`validation </validation>`
feature at Action (Controller) and View. You can use any database access library
you want, while still can validate user input easily.

Xitrum is inspired by `Nitrogen <http://nitrogenproject.com/>`_.

::

  +-----------------+
  |    Your app     |
  +-----------------+
  |      Xitrum     |
  | +-------------+ |
  | | Action/View | |
  | +-------------+ |
  +-----------------+
  |      Netty      |
  | +-------------+ |
  | | HTTP Server | |
  | +-------------+ |
  +-----------------+
