Upload
======

See also :doc:`Scopes chapter </scopes>`.

In your upload form, remember to set ``enctype`` to ``multipart/form-data``.

MyUpload.scalate:

::

  form(method="post" action={url[MyUpload]} enctype="multipart/form-data")
    != antiCSRFInput

    label Please select a file:
    input(type="file" name="myFile")

    button(type="submit") Upload

MyUpload:

::

  val myFile = param[FileUpload]("myFile")

myFile is an instance of `FileUpload <http://netty.io/3.6/api/org/jboss/netty/handler/codec/http/multipart/FileUpload.html>`_.
Use its methods to get file name, move file to a directory etc.

Ajax style upload
-----------------

There are many client side libraries that support Ajax style upload. They use
hidden iframe or Flash to send the ``multipart/form-data`` above. If you are not
sure which request parameter the libraries use in the form to send file, see
Xitrum request log.
