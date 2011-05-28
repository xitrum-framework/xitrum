Introduction
============

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s1.jpg

Xitrum ("Smurf" in Vietnamese) is a lightweight Scala web framework and web
server on top of Netty:

* It fills the gap between Scalatra and Lift:
  More powerful than Scalatra but less complex than Lift.
  You can easily define URL routings (Scalatra style) and POSTbacks (Lift style).
* Asynchronous, in the spirit of Netty.
* Type-safe, in the spirit of Scala.
* Stateless, by default session is stored in cookie.

::

  +-----------------+
  |    Your app     |
  +-----------------+
  |      Xitrum     |
  | +-------------+ |
  | |  Framework  | |
  | +-------------+ |
  | | Middlewares | |
  | +-------------+ |
  | | HTTP Server | |
  | +-------------+ |
  +-----------------+
  |      Netty      |
  +-----------------+

.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s7.jpg
.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s9.jpg
.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s12.jpg
.. image:: http://www.bdoubliees.com/journalspirou/sfigures6/schtroumpfs/s11.jpg

