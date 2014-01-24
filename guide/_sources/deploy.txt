Deploy to production server
===========================

You may run Xitrum directly:

::

  Browser ------ Xitrum instance

Or behind a load balancer like HAProxy, or reverse proxy like Apache or Nginx:

::

  Browser ------ Load balancer/Reverse proxy -+---- Xitrum instance1
                                              +---- Xitrum instance2

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

By default ``sbt/sbt xitrum-package`` command is configured to copy directories
``config``, ``public``, and ``script`` to ``target/xitrum``. If you want it to
copy additional directories or files change ``build.sbt`` like this:

::

  XitrumPackage.copy("config", "public, "script", "doc/README.txt", "etc.")

See `xitrum-package homepage <https://github.com/ngocdaothanh/xitrum-package>`_
for more information.

Install Oracle JDK on CentOS or Ubuntu manually
-----------------------------------------------

This guide is here for convenient reference. You can certainly install Java from
a package manager.

Check installed alternatives:

::

  sudo update-alternatives --list java

Output example:

::

  /usr/lib/jvm/jdk1.7.0_15/bin/java
  /usr/lib/jvm/jdk1.7.0_25/bin/java

Check machine environment (32 bit or 64 bit):

::

  file /sbin/init

Output example:

::

  /sbin/init: ELF 64-bit LSB shared object, x86-64, version 1 (SYSV), dynamically linked (uses shared libs), for GNU/Linux 2.6.24, BuildID[sha1]=0x4efe732752ed9f8cc491de1c8a271eb7f4144a5c, stripped

Download JDK from `Oracle <http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html>`_.
There's a `trick <http://stackoverflow.com/questions/10268583/how-to-automate-download-and-instalation-of-java-jdk-on-linux>`_
to download jdk without browser:

::

  wget --no-cookies --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com" "http://download.oracle.com/otn-pub/java/jdk/7u45-b18/jdk-7u45-linux-x64.tar.gz"

Unarchive and move it:

::

  tar -xzvf jdk-7u45-linux-x64.tar.gz
  sudo mv jdk1.7.0_45 /usr/lib/jvm/jdk1.7.0_45

Register commands as an alternative:

::

  sudo update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/jdk1.7.0_45/bin/java" 1
  sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/jdk1.7.0_45/bin/javac" 1
  sudo update-alternatives --install "/usr/bin/javap" "javap" "/usr/lib/jvm/jdk1.7.0_45/bin/javap" 1
  sudo update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/jdk1.7.0_45/bin/javaws" 1

Chose new path with interactive shell:

::

  sudo update-alternatives --config java

Output example:

::

  There are 3 choices for the alternative java (providing /usr/bin/java).

    Selection    Path                               Priority   Status
  ------------------------------------------------------------
  * 0            /usr/lib/jvm/jdk1.7.0_25/bin/java   50001     auto mode
    1            /usr/lib/jvm/jdk1.7.0_15/bin/java   50000     manual mode
    2            /usr/lib/jvm/jdk1.7.0_25/bin/java   50001     manual mode
    3            /usr/lib/jvm/jdk1.7.0_45/bin/java   1         manual mode

  Press enter to keep the current choice[*], or type selection number: 3
  update-alternatives: using /usr/lib/jvm/jdk1.7.0_45/bin/java to provide /usr/bin/java (java) in manual mode

Check version:

::

  java -version

Output example:

::

  java version "1.7.0_45"
  Java(TM) SE Runtime Environment (build 1.7.0_45-b18)
  Java HotSpot(TM) 64-Bit Server VM (build 24.45-b08, mixed mode)

Do also:

::

  sudo update-alternatives --config javac
  sudo update-alternatives --config javap
  sudo update-alternatives --config javaws

Start Xitrum in production mode when the system starts
------------------------------------------------------

``script/runner`` (for *nix) and ``script/runner.bat`` (for Windows) are the script to
run any object with ``main`` method. Use it to start the web server in production
environment.

::

  script/runner quickstart.Boot

You may want to modify ``runner`` (or ``runner.bat``) to tune JVM settings. Also see ``config/xitrum.conf``.

To start Xitrum in background on Linux when the system starts, `daemontools <http://cr.yp.to/daemontools.html>`_
is a very good tool. To install it on CentOS, see
`this instruction <http://whomwah.com/2008/11/04/installing-daemontools-on-centos5-x86_64/>`_.

Or use `Supervisord <http://supervisord.org/>`_.
``/etc/supervisord.conf`` example:

::

  [program:my_app]
  directory=/path/to/my_app
  command=/path/to/my_app/script/runner quickstart.Boot
  autostart=true
  autorestart=true
  startsecs=3
  user=my_user
  redirect_stderr=true
  stdout_logfile=/path/to/my_app/log/stdout.log
  stdout_logfile_maxbytes=10MB
  stdout_logfile_backups=7
  stdout_capture_maxbytes=1MB
  stdout_events_enabled=false
  environment=PATH=/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin:/opt/aws/bin:~/bin

Set up port forwarding
----------------------

Xitrum listens on port 8000 and 4430 by default.
You can change these ports in ``config/xitrum.conf``.

You can update ``/etc/sysconfig/iptables`` with these commands to forward port
80 to 8000 and 443 to 4430:

::

  sudo su - root
  chmod 700 /etc/sysconfig/iptables
  iptables-restore < /etc/sysconfig/iptables
  iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8000
  iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 443 -j REDIRECT --to-port 4430
  iptables -t nat -I OUTPUT -p tcp -d 127.0.0.1 --dport 80 -j REDIRECT --to-ports 8000
  iptables -t nat -I OUTPUT -p tcp -d 127.0.0.1 --dport 443 -j REDIRECT --to-ports 4430
  iptables-save -c > /etc/sysconfig/iptables
  chmod 644 /etc/sysconfig/iptables

Of course for example if you have Apache running on port 80 and 443, you have to stop it:

::

  sudo /etc/init.d/httpd stop
  sudo chkconfig httpd off

Good read:

* `Iptables tutorial <http://www.frozentux.net/iptables-tutorial/chunkyhtml/>`_

Tune Linux for many connections
-------------------------------

Note that on Mac, `JDKs suffer from a serious problem with IO (NIO) performance <https://groups.google.com/forum/#!topic/spray-user/S-SNR2m0BWU>`_.

Good read:

* `Linux Performance Tuning (Riak) <http://docs.basho.com/riak/latest/ops/tuning/linux/>`_
* `AWS Performance Tuning (Riak) <http://docs.basho.com/riak/latest/ops/tuning/aws/>`_
* `Ipsysctl tutorial <http://www.frozentux.net/ipsysctl-tutorial/chunkyhtml/>`_
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

HAProxy tips
------------

To config HAProxy for SockJS, see `this example <https://github.com/sockjs/sockjs-node/blob/master/examples/haproxy.cfg>`_.

To have HAProxy reload config file without restarting, see `this discussion <http://serverfault.com/questions/165883/is-there-a-way-to-add-more-backend-server-to-haproxy-without-restarting-haproxy>`_.

HAProxy is much easier to use than Nginx. It suits Xitrum because as mentioned in
:doc:`the section about caching </cache>`, Xitrum serves static files
`very fast <https://gist.github.com/3293596>`_. You don't have to use the static file
serving feature in Nginx.

Nginx tips
----------

If you use WebSocket or SockJS feature in Xitrum and want to run Xitrum behind
Nginx 1.2, you must install additional module like
`nginx_tcp_proxy_module <https://github.com/yaoweibin/nginx_tcp_proxy_module>`_.
Nginx 1.3+ supports WebSocket natively.

Nginx by default uses HTTP 1.0 protocol for reverse proxy. If your backend server
returns chunked response, you need to tell Nginx to use HTTP 1.1 like this:

::

  location / {
    proxy_http_version 1.1;
    proxy_pass http://127.0.0.1:8000;
  }


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

  web: target/xitrum/script/runner <YOUR_PACKAGE.YOUR_MAIN_CLASS> $PORT

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

