Introduction
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s1.jpg

Xitrum ("Smurf" in Vietnamese) is a lightweight Scala web framework and web
server on top of Netty:

* It fills the gap between `Scalatra <https://github.com/scalatra/scalatra>`_ and `Lift <http://liftweb.net/>`_:
  More powerful than Scalatra but less complex than Lift.
  You can easily define URL routings (Scalatra style) and POSTbacks (Lift style).
* Asynchronous, in the spirit of Netty.
* Typesafe, in the spirit of Scala.
* Stateless, by default session is stored in cookie.

Xitrum is not a full-stack web framework like `Rails <http://rubyonrails.org/>`_
because it does not provide M (in MVC). However, it does provide :doc:`validation </validation>`
feature at Action (Controller) and View. You can use any database access library
you want, while still can validate user input easily.

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

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s9.jpg
.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s12.jpg
.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s11.jpg

