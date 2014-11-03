Composite Model Reference
=========================
Composite keeps track of two objects:

* Sessions
* Devices

The relationship between sessions and devices is one-to-many, and devices are stored in an array in a given session object.

----

Session Model
-------------
All types given are Java types.

.. cssclass:: table-bordered
.. cssclass:: table-striped
+-----------------------+-------------------+--------------------------------------------------------------------------+
| Member                | Type              | Description                                                              |
+=======================+===================+==========================================================================+
| _id                   | String            | CouchDB unique id for the session record.                                |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| _rev                  | String            | CouchDB revision version for the session record.                         |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| devices               | ArrayList<Device> | Array of devices in the session.                                         |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| geoLocation           | float[]           | Array of lat/lon coordinates for the session location.                   |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| inserted              | long              | Unix timestamp of when the session was created.                          |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| locked                | boolean           | Indicates if the session is unlocked or not. If locked, no more devices  |
|                       |                   | join the session until it is unlocked.                                   |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| room                  | string            | The name of the "room" for the session. Can be used on the client end to |
|                       |                   | disambiguate multiple sessions in close proximity.                       |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| sessionStarted        | long              | Unix timestamp for when a session's activity begun. For example, when a  |
|                       |                   | shared game begins.                                                      |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| sessionEnded          | long              | Unix timestamp for when a session's activity ended.                      |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| updated               | long              | Unix timestamp of the last update to the session object.                 |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| uuid                  | UUID              | Unique identifier for the session.                                       |
+-----------------------+-------------------+--------------------------------------------------------------------------+

Device Model
------------
All types given are Java types.

.. cssclass:: table-bordered
.. cssclass:: table-striped
+-----------------------+-------------------+--------------------------------------------------------------------------+
| Member                | Type              | Description                                                              |
+=======================+===================+==========================================================================+
| uuid                  | UUID              | Unique identifier for the device.                                        |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| width                 | int               | Device screen width.                                                     |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| height                | int               | Device screen height.                                                    |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| performance           | int               | Figure representing relative performance of device. This property is used|
|                       |                   | on the client-side to manage screen redrawing.                           |
+-----------------------+-------------------+--------------------------------------------------------------------------+
| instructions          | int               | Figure representing client-side instructions that tell the device what to|
|                       |                   | do within the context of the session and other devices.                  |
+-----------------------+-------------------+--------------------------------------------------------------------------+


Sample Record (CouchDB)
-----------------------
Records are saved as JSON in the CouchDB instance(s). Below is a sample taken from the Composite Demo app.

.. code:: json

    {
       "_id": "39d4e19c25964bb3a6c3b2f806e8fa33",
       "_rev": "3-32d6973d965f40261053b2cfb523212f",
       "devices": [
           {
               "uuid": "5c6f9f63-dc2f-4f4c-81dc-b722eb4b0a1f",
               "width": 375,
               "height": 667,
               "performance": 0,
               "instructions": 0
           },
           {
               "uuid": "7c528f92-9223-4802-bede-b8b01e8e5aa6",
               "width": 375,
               "height": 667,
               "performance": 0,
               "instructions": 0
           },
           {
               "uuid": "b969d6e3-94f8-4c1a-a8e9-efa7b4b58962",
               "width": 375,
               "height": 667,
               "performance": 0,
               "instructions": 0
           }
       ],
       "geoLocation": [
           45.524426,
           -122.68396
       ],
       "inserted": 1412097229725,
       "locked": false,
       "room": "default_room",
       "sessionStarted": 1412097232288,
       "sessionEnded": 0,
       "updated": 1412097232288,
       "uuid": "28171d9c-05e4-422b-9dd9-9e8b8ff84609"
    }
