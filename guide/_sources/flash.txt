Serve flash socket policy file
==============================

Read about flash socket policy:

* http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html
* http://www.lightsphere.com/dev/articles/flash_socket_policy.html

The protocol to serve flash socket policy file is different from HTTP.
To serve:

1. Modify `config/flash_socket_policy.xml <https://github.com/ngocdaothanh/xitrum-new/blob/master/config/flash_socket_policy.xml>`_ appropriately
2. Modify `config/xitrum.conf <https://github.com/ngocdaothanh/xitrum-new/blob/master/config/xitrum.conf>`_ to enable serving the above file
