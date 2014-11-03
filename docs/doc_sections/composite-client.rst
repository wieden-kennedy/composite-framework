Composite Client Command Reference
==================================
This is a companion guide for using the Composite Client, found `here <https://github.com/wieden-kennedy/composite-client>`_.

Methods
~~~~~~~

Constructor
^^^^^^^^^^^
The global ``Composite`` constructor. Takes no config options and is used for instantiation purposes.

.. code:: javascript

    var app = new Composite();

----

connect:function (string:url)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Connects to the Composite service. The handshake is over HTTP, so a good example would be:

.. code:: javascript

    app.connect('http://localhost:8081/composite');

----

on:function (string:event, function:callback)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Registers an event handler for a given event.

.. code:: javascript

    app.on('app_start', function(){ start_your_app(); });

The following are valid events (``Automatic`` denotes that the event is triggered internally, whilst ``Manual``
requires a client-triggered action):

.. cssclass:: table-bordered
.. cssclass:: table-striped
+----------------------------------------------+------------------------------------------------------------------------+
| Property                                     | Description                                                            |
+==============================================+========================================================================+
| ``init``                                     | Fires after a successful connect and a valid UUID has been assigned.   |
|                                              | Automatic.                                                             |
+----------------------------------------------+------------------------------------------------------------------------+
| ``synced``                                   | Fires once the app has determined the average latency between it and   |
|                                              | the service. Automatic.                                                |
+----------------------------------------------+------------------------------------------------------------------------+
| ``app_start``                                | Fires after the ``host`` device in the same session queries the        |
|                                              | ``start`` endpoint. Manual.                                            |
+----------------------------------------------+------------------------------------------------------------------------+
| ``app_end``                                  | Fires after the ``host`` device in the same session queries the ``end``|
|                                              | endpoint. Manual.                                                      |
+----------------------------------------------+------------------------------------------------------------------------+
| ``session_joined``                           | Fires after a successful ``join`` query and only to the client that    |
|                                              | queried it. Manual.                                                    |
+----------------------------------------------+------------------------------------------------------------------------+
| ``device_update``                            | Fires after a new device has joined the session. Manual                |
+----------------------------------------------+------------------------------------------------------------------------+
| ``device_disconnect``                        | Fires if a device in the session suddenly disconnects. Manual.         |
+----------------------------------------------+------------------------------------------------------------------------+
| ``data``                                     | Fires when a client sends a payload to the ``data`` endpoint. Manual.  |
+----------------------------------------------+------------------------------------------------------------------------+
| ``update``                                   | Fires when a client sends a payload to the ``update`` endpoint. Manual.|
+----------------------------------------------+------------------------------------------------------------------------+

----

off:function (string:event, function:callback)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Removes an event handler for a given event.

.. code:: javascript

    app.off('app_start', function(){ start_your_app(); }); // Must pass the function you called earlier

----

syncTime:function
^^^^^^^^^^^^^^^^^
Sends a message to the service with the current time in order to determine latency. This also happens automatically,
but is exposed in case your application needs to check more frequently.

.. code:: javascript

    app.syncTime()

----

join:function (object)
^^^^^^^^^^^^^^^^^^^^^^
Sends a join request to the service to get a session, requires that the client has already set its location (lat/lon)
under the ``location`` array (see location).

The Object passed can have the following parameters:
- type: String 'enter' or 'exit'. Defaults to 'exit'.
- geo: Array with two elements corresponding to a devices longitude/latitude. Defaults to the local lon/lat if not present.

.. code:: javascript

    app.join({type: 'exit', geo: [0.1234123, 1.123123]});

----

sendData:function (object)
^^^^^^^^^^^^^^^^^^^^^^^^^^
Sends a message to all clients in the same session with a data payload. The passed object is the payload you wish to
send to all clients. Requires that clients have ``join``ed successfully prior to sending.

.. code:: javascript

    app.sendData({ ballPosition: [103, 234], ballSpeed: 23, activeDevice: 2 });

----

sendUpdate:function (object)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Sends a message to all clients in the same session with a data payload. The passed object is the payload you wish to
send to all clients. Requires that clients have ``join``ed successfully prior to sending.

.. code:: javascript

    app.sendUpdate({ ballPosition: [103, 234], ballSpeed: 23, activeDevice: 2 });

----

startApp:function
^^^^^^^^^^^^^^^^^
Triggers the ``app_start`` event in all other clients. Must be the "host" client to trigger (see ``host`` below).

.. code:: javascript

    app.startApp()

----

endApp:function
^^^^^^^^^^^^^^^
Triggers the ``app_end`` event in all the other clients. Must be the "host" client to trigger (see ``host`` below).

.. code:: javascript

    app.endApp();

----

disconnect:function
^^^^^^^^^^^^^^^^^^^
Disconnects cleanly from the Composite service. Other clients in the same session will be notified of the disconnect.

.. code:: javascript

    app.disconnect()

----

Properties
~~~~~~~~~~

connected
^^^^^^^^^
**Type:** ``boolean``

Container indicating if the client is connected to the composite service.

.. code:: javascript

    app.connected; // true if connected, false if not

----

uuid
^^^^
**Type:** ``string``

The UUID of the device given from the service, can be used as a way to find the device(s) order.

.. code:: javascript

    app.uuid; // "7040550a-3834-4974-a19c-c7d39749a7e5"

----

timeDifference
^^^^^^^^^^^^^^
**Type:** ``number``

The median time difference between the client's Date.now and the services Date.now.

.. code:: javascript

    app.timeDifference; // 1121

----

latency
^^^^^^^
**Type:** ``number (milliseconds)``

The average time it takes to send and receive a message through composite. Updated periodically throughout the application.

.. code:: javascript

    app.latency; // 10

----

host
^^^^
**Type:** ``boolean``

If the device is the ``host`` device. This is determined by order ``join``ed, and the first device to join is given ``host``
privileges. This is updated during the ``device_update`` event as it's possible for the ``host`` to drop connection. ``host``s
can trigger the ``app_start`` and ``app_end`` events.

.. code:: javascript

    app.host; // true

----

location
^^^^^^^^
**Type:** ``array[float]``

The container for the devices geographic position in the form of ``[{latitude}, {longitude}]``.
Location is not captured automatically, and must be implemented manually, and is used for session management.

Example:

.. code:: javascript

    navigator.geolocation.getCurrentPosition(function(position) {
        // Setting the position
        app.location = [position.coords.latitude, position.coords.longitude];
    });

    app.location; // [45.523452, -122.67620699999999]

----

session
^^^^^^^
**Type:** ``string``

The session the device is currently a part of. This is set automatically after successfully ``join``ing, and is required when broadcasting ``update``s and ``data``.

.. code:: javascript

    app.session; // "7040550a-3834-4974-a19c-c7d39749a7e5"

----

active
^^^^^^
**Type:** ``boolean``
If the app is currently in the ``start`` state. This happens automatically after the ``app_start`` event and is set to false after the ``app_end`` event.

.. code:: javascript

    app.active; // true
