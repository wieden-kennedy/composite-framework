Composite (Backend) Messaging Reference
=======================================

Endpoints
---------

/init
~~~~~
Direct message handler for an init message sent by a device. The init message will follow directly after the
device has made a successful socket connection to the server, and indicates that the device would like to start
interacting with Composite.

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+--------------------------------------------------------------+
| Parameters                 | Description                                                  |
+============================+==============================================================+
| principal                  | The device principal that sent the request                   |
+----------------------------+--------------------------------------------------------------+

**Returns to:**
``/queue/device``

/join
~~~~~
Direct message handler that receives a join message from a device. When a device joins, a session is attempted to
be found within a tolerated geo-proximity. if one is found, the device is added to it and returned, if not, a
new session is created.

**Params**

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+------------------------------------------------------------------------+
| Parameters                 | Description                                                            |
+============================+========================================================================+
| j                          | Stringified JoinMessage sent from the device seeking to join a session |
+----------------------------+------------------------------------------------------------------------+

**Returns to:**
``/queue/device``

/sync
~~~~~
Direct message handler for assisting connected devices in calculating the latency between when messages are sent
by the server and when they are received by the client. Each client should hit this endpoint a number of times
just after the initial connect response is received, and will calculate an average latency time based off of
the server responses.

**Params**

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+------------------------------------------------------------------------+
| Parameters                 | Description                                                            |
+============================+========================================================================+
| s                          | Stringified SyncMessage sent by client device                          |
+----------------------------+------------------------------------------------------------------------+

**Returns to:**
``/queue/device``

/{id}
~~~~~
Multiplex handler inbound messages from session devices that are sent to the session topic channel. Uses the following handlers
to determine what information to broadcast back across the session topic:

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+---------------------------------------------------------------------------------------------+
| Handler                    | Description                                                                                 |
+============================+=============================================================================================+
| update                     | Broadcasts an update response back to the session when a device principal sends an update   |
+----------------------------+---------------------------------------------------------------------------------------------+
| data                       | Broadcasts a data response back to the session when a device principal sends a data message |
+----------------------------+---------------------------------------------------------------------------------------------+
| start                      | Broadcasts a start event back to the session when a device principal initiates a start event|
+----------------------------+---------------------------------------------------------------------------------------------+
| stop                       | Broadcasts a stop event back to the session when a device principal initiates a stop event  |
+----------------------------+---------------------------------------------------------------------------------------------+
| devices                    | Broadcasts a list of devices found in a session back to the session topic                   |
+----------------------------+---------------------------------------------------------------------------------------------+

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+------------------------------------------------------------------------+
| Parameters                 | Description                                                            |
+============================+========================================================================+
| principal                  | Device principle sending the message                                   |
+----------------------------+------------------------------------------------------------------------+
| id                         | UUID of the session to which the inbound message need be returned      |
+----------------------------+------------------------------------------------------------------------+
| obj                        | Map<String, Object> message data for the endpoint. Contains the type of|
|                            | message that corresponds to one of the above endpoints as well as      |
|                            | arbitrary String:Object pairs that contain the main message body       |
+----------------------------+------------------------------------------------------------------------+

**Returns to:**
``/queue/device``


/ping
~~~~~
Message handler that receives a ping from a connected client device, and in turn adds the device to a list of
"healthy" session devices, thereby preventing it from being automatically deleted from the session. If a device
fails to ping the server within a specified timeframe, it will be marked as unhealthy, and subsequently deleted.

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+--------------------------------------------------------------+
| Parameters                 | Description                                                  |
+============================+==============================================================+
| principal                  | The device principal that sent the request                   |
+----------------------------+--------------------------------------------------------------+

**Returns to:**
None. Logs the device in a registry that determines which devices to boot if they do not ping regularly.

/disconnect
~~~~~~~~~~~
Handles disconnect messages sent by a client device by removing the device from its associated session.
if there are still devices in its session, they are notified of the disconnect, otherwise, the session is removed.

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------+--------------------------------------------------------------+
| Parameters                 | Description                                                  |
+============================+==============================================================+
| principal                  | The device principal that sent the request                   |
+----------------------------+--------------------------------------------------------------+


**Returns to:**
``/topic/{id}`` where id is the session id to which the device belongs.
