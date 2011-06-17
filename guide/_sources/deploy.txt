Deploy to production server
===========================

You may run Xitrum directly:

::

  Browser ------ Xitrum instance

Or behind a load balancer like HAProxy, or reverse proxy like Nginx:

::

  Browser ------ Load balancer/Reverse proxy -+---- Xitrum instance1
                                              +---- Xitrum instance2

If you use WebSocket or SockJS feature in Xitrum and want to run Xitrum behind
Nginx 1.2, you must install additional module like
`nginx_tcp_proxy_module <https://github.com/yaoweibin/nginx_tcp_proxy_module>`_.
Nginx 1.3+ supports WebSocket natively.

HAProxy is much easier to use. It suits Xitrum because as mentioned in
:doc:`the section about caching </cache>`, Xitrum serves static files
`very fast <https://gist.github.com/3293596>`_. You don't need to use static file
serving feature in Nginx.

HAProxy
-------

To config HAProxy for SockJS, see `this example <https://github.com/sockjs/sockjs-node/blob/master/examples/haproxy.cfg>`_.

To have HAProxy reload config file without restarting, see `this discussion <http://serverfault.com/questions/165883/is-there-a-way-to-add-more-backend-server-to-haproxy-without-restarting-haproxy>`_.

Package directory
-----------------

Run ``sbt/sbt xitrum-package`` to prepare ``target/xitrum`` directory, ready to
deploy to production server:

::

  target/xitrum
    bin
      runner
      runner.bat
    config
      [config files]
    public
      [static public files]
    lib
      [dependencies and packaged project file]

Customize xitrum-package
------------------------

By default ``sbt/sbt xitrum-package`` command simply copies ``config`` and ``public``
directories to ``target/xitrum``. If you want it to copy additional files
and directories (README, INSTALL, doc etc.), config ``build.sbt`` like this:

::

  TODO

Start Xitrum in production mode
-------------------------------

``bin/runner`` (for *nix) and ``bin/runner.bat`` (for Windows) are the script to
run any object with ``main`` method. Use it to start the web server in production
environment.

::

  bin/runner quickstart.Boot

You may want to modify ``runner`` and ``runner.bat`` to tune JVM settings. Also see ``config/xitrum.conf``.

To start Xitrum in background when the system starts, `daemontools <http://cr.yp.to/daemontools.html>`_
is a very good tool. To install it on CentOS, see
`this instruction <http://whomwah.com/2008/11/04/installing-daemontools-on-centos5-x86_64/>`_.

Tune Linux for many connections
-------------------------------

Note that on Mac, `JDKs suffer from a serious problem with IO (NIO) performance <https://groups.google.com/forum/#!topic/spray-user/S-SNR2m0BWU>`_.

Good read:

* `Ipsysctl tutorial <http://www.frozentux.net/ipsysctl-tutorial/chunkyhtml/>`_
* `Iptables tutorial <http://www.frozentux.net/iptables-tutorial/chunkyhtml/>`_
* `TCP variables <http://www.frozentux.net/ipsysctl-tutorial/chunkyhtml/tcpvariables.html>`_

Increase open file limit
~~~~~~~~~~~~~~~~~~~~~~~~

Each connection is seen by Linux as an open file.
The default maximum number of open file is 1024.
To increase this limit, modify /etc/security/limits.conf:

::

  *  soft  nofile  1024000
  *  hard  nofile  1024000

You need to logout and login again for the above config to take effect.
To confirm, run ``ulimit -n``.

Tune kernel
~~~~~~~~~~~

As instructed in the article
`A Million-user Comet Application with Mochiweb <http://www.metabrew.com/article/a-million-user-comet-application-with-mochiweb-part-1>`_,
modify /etc/sysctl.conf:

::

  # General gigabit tuning
  net.core.rmem_max = 16777216
  net.core.wmem_max = 16777216
  net.ipv4.tcp_rmem = 4096 87380 16777216
  net.ipv4.tcp_wmem = 4096 65536 16777216

  # This gives the kernel more memory for TCP
  # which you need with many (100k+) open socket connections
  net.ipv4.tcp_mem = 50576 64768 98152

  # Backlog
  net.core.netdev_max_backlog = 2048
  net.core.somaxconn = 1024
  net.ipv4.tcp_max_syn_backlog = 2048
  net.ipv4.tcp_syncookies = 1

Run ``sudo sysctl -p`` to apply.
No need to reboot, now your kernel should be able to handle a lot more open connections.

Note about backlog
~~~~~~~~~~~~~~~~~~

TCP does the 3-way handshake for making a connection.
When a remote client connects to the server,
it sends SYN packet, and the server OS replies with SYN-ACK packet,
then again that remote client sends ACK packet and the connection is established.
Xitrum gets the connection when it is completely established.

According to the article
`Socket backlog tuning for Apache <https://sites.google.com/site/beingroot/articles/apache/socket-backlog-tuning-for-apache>`_,
connection timeout happens because of SYN packet loss which happens because
backlog queue for the web server is filled up with connections sending SYN-ACK
to slow clients.

According to the
`FreeBSD Handbook <http://www.freebsd.org/doc/en_US.ISO8859-1/books/handbook/configtuning-kernel-limits.html>`_,
the default value of 128 is typically too low for robust handling of new
connections in a heavily loaded web server environment. For such environments,
it is recommended to increase this value to 1024 or higher.
Large listen queues also do a better job of avoiding Denial of Service (DoS) attacks.

The backlog size of Xitrum is set to 1024 (memcached also uses this value),
but you also need to tune the kernel as above.

To check the backlog config:

::

  cat /proc/sys/net/core/somaxconn

Or:

::

  sysctl net.core.somaxconn

To tune temporarily, you can do like this:

::

  sudo sysctl -w net.core.somaxconn=1024

Deploy to Heroku
----------------

You may run Xitrum at `Heroku <https://www.heroku.com/>`_.

Sign up and create repository
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Following the `Official Document <https://devcenter.heroku.com/articles/quickstart>`_,
sign up and create git repository.

Create Procfile
~~~~~~~~~~~~~~~

Create Procfile and save it at project root directory. Heroku reads this file and
executes on start. Port number is ginven by Heroku automatically as ``$PORT``.

::

  web: target/xitrum/bin/runner <YOUR_PACKAGE.YOUR_MAIN_CLASS> $PORT

Change port setting
~~~~~~~~~~~~~~~~~~~~

Because Heroku assigns port automatically, you need to do like this:

Main (boot) class:

::

  import util.Properties

  object Boot {
    def main(args: Array[String]) {
      val port = Properties.envOrElse("PORT", "8000")
      System.setProperty("xitrum.port.http", port)
      Server.start()
    }
  }

config/xitrum.conf:

::

  port {
    http              = 8000
    # https             = 4430
    # flashSocketPolicy = 8430  # flash_socket_policy.xml will be returned
  }

If you want to use SSL, you need `add on <https://addons.heroku.com/ssl>`_.

See log level
~~~~~~~~~~~~~

config/logback.xml:

::

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>

Tail log from Heroku command:

::

  heroku logs -tail

Create alias for ``xitrum-package``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At deploy time, Heroku runs ``sbt clean compile stage``. So you need to add alias
for ``xitrum-package``.

build.sbt:

::

  addCommandAlias("stage", ";xitrum-package")


Push to Heroku
~~~~~~~~~~~~~~

Deploy process is hooked by git push.

::

  git push heroku master


See also `Official document for Scala <https://devcenter.heroku.com/articles/getting-started-with-scala>`_.
