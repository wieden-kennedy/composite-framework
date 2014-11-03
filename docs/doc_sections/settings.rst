Configuration
=============
Composite is set up to be built for local, development, test, and production environments by means of configuration properties.
Each environment has its own set of configuration/properties files, and they can be found in one of two locations:

* ``src/main/resources/``.
* ``pom.xml``

In each of the properties files, defaults have been left in place, and context-specific values have been omitted.

application.properties
^^^^^^^^^^^^^^^^^^^^^^
This is where global application settings are stored, such as maximium distance between two devices that should be
considered to be in the same session. Properties that can be edited in this file are:

Global Composite Properties
"""""""""""""""""""""""""""

.. cssclass:: table-striped
.. cssclass:: table-bordered
+-----------------------------------------+------------------------+---------------------------------------------------+
| Property                                | Set in                 | Description                                       |
+=========================================+========================+===================================================+
| **staleSessionMinutes**                 | application.properties | the number of minutes after which a session should|
|                                         |                        | be considered stale and eligible for deletion     |
+-----------------------------------------+------------------------+---------------------------------------------------+
| **minDistanceThresholdBetweenDevices**  | application.properties | the preferred maximum distance between devices    |
|                                         |                        | that should be considered in the same session.    |
|                                         |                        | When a new device contacts the server, this       |
|                                         |                        | distance will be checked to find sessions within  |
|                                         |                        | this range that the device may join.              |
+-----------------------------------------+------------------------+---------------------------------------------------+
| **maxDistanceThresholdBetweenDevices**  | application.properties | the maximum distance between devices that should  |
|                                         |                        | be considered to be in the same session. This     |
|                                         |                        | distance will be used to find a session when a    |
|                                         |                        | device connects, and no sessions are found within |
|                                         |                        | the preferred maximum distance range are found.   |
+-----------------------------------------+------------------------+---------------------------------------------------+
| **regex.applicationId**                 | application.properties | a regex pattern containing all apps running on a  |
|                                         |                        | Composite instance, e.g.,                         |
|                                         |                        | *regex.applicationId=(appOne|appTwo)              |
+-----------------------------------------+------------------------+---------------------------------------------------+
| **deleteUnhealthyDevices**              | application.properties | whether to delete unhealthy devices from the      |
|                                         |                        | database when the periodic cleanup task runs.     |
+-----------------------------------------+------------------------+---------------------------------------------------+

Application-Specific Properties
"""""""""""""""""""""""""""""""

.. cssclass:: table-striped
.. cssclass:: table-bordered
+--------------------------------+------------------------+----------------------------------------------------------+
| Property                       | Set in                 | Description                                              |
+================================+========================+==========================================================+
| **appId.applicationId**        | application.properties | the name of the application                              |
+--------------------------------+------------------------+----------------------------------------------------------+
| **appId.maxDevicesPerSession** | application.properties | the maximum number of devices that can exist in a session|
+--------------------------------+------------------------+----------------------------------------------------------+
| **appId.roomNames**            | application.properties | comma-separated list of names identifying rooms assigned |
|                                |                        | to sessions. Room names do not have any bearing on       |
|                                |                        | session activity and are used to distinguish between     |
|                                |                        | multiple sessions in close proximity.                    |
+--------------------------------+------------------------+----------------------------------------------------------+

----

couchdb.properties
^^^^^^^^^^^^^^^^^^
This is where global settings for the CouchDB connection will be defined, such as host, port, and maximum number of connections.
Properties that can be edited in this file are:

.. cssclass:: table-striped
.. cssclass:: table-bordered
+---------------------------------------------+------------------------+-----------------------------------------------+
| Property                                    | Set in                 | Description                                   |
+=============================================+========================+===============================================+
| **couchdb.host**                            | pom.xml                | the host IP or DNS name for the CouchDB       |
|                                             |                        | server                                        |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.port**                            | application.properties | the port CouchDB is served over               |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.username**                        | pom.xml                | the username used to connect to a CouchDB     |
|                                             |                        | database that is password                     |
|                                             |                        | protected. Can be blank if not needed.        |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.password**                        | pom.xml                | the password used to conenct to a CouchDB     |
|                                             |                        | database that is password                     |
|                                             |                        | protected. Can be blank if not needed.        |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.max.connections**                 | application.properties | the maximum number of connections to allow in |
|                                             |                        | the connection pool.                          |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.createdb.if-not-exist**           | application.properties | whether to create databases if they do not    |
|                                             |                        | already exist when the application first      |
|                                             |                        | connects to the CouchDB host.                 |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.protocol**                        | application.properties | the protocol to use when connecting to the    |
|                                             |                        | CouchDB database.                             |
+---------------------------------------------+------------------------+-----------------------------------------------+
| **couchdb.sessions.database.name**          | application.properties | the name of the database where session        |
|                                             |                        | information will be stored.                   |
+---------------------------------------------+------------------------+-----------------------------------------------+

----

rabbitmq.properties
^^^^^^^^^^^^^^^^^^^
This is where global settings for the RabbitMQ broker can be configured, such as host, port, and login.
Properties that can be edited in this file are:

.. cssclass:: table-striped
.. cssclass:: table-bordered
+----------------------------------------------+------------------------+----------------------------------------------+
| Property                                     | Set in                 | Description                                  |
+==============================================+========================+==============================================+
| **rabbitmq.host**                            | pom.xml                | the host IP or DNS name for the RabbitMQ     |
|                                              |                        | server.                                      |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.port**                            | rabbitmq.properties    | the port RabbitMQ is served over             |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.clientLogin**                     | pom.xml                | the login name used for the RabbitMQ broker. |
|                                              |                        | Can be left blank if not needed.             |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.clientPasscode**                  | pom.xml                | the password used for the RabbitMQ broker.   |
|                                              |                        | Can be left blank if not needed.             |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.systemLogin**                     | pom.xml                | the login name used for the RabbitMQ host    |
|                                              |                        | system.Can be left blank if not needed.      |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.systemPasscode**                  | pom.xml                | the password used for the RabbitMQ host      |
|                                              |                        | system. Can be left blank if not needed.     |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.systemHeartbeatSendInterval**     | rabbitmq.properties    | interval, in milliseconds, on which to send  |
|                                              |                        | a heartbeat                                  |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.systemHeartbeatReceiveInterval**  | rabbitmq.properties    | interval, in milliseconds, on which to       |
|                                              |                        | receive heartbeats                           |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.heartbeatTime**                   | rabbitmq.properties    | heartbeat interval, in milliseconds, for the |
|                                              |                        | Stomp client to send on                      |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.inboundChannelCorePoolSize**      | rabbitmq.properties    | initial number of executor threads for       |
|                                              |                        | inbound message processing                   |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.outboundChannelCorePoolSize**     | rabbitmq.properties    | initial number of executor threads for       |
|                                              |                        | outbound message processing                  |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.brokerChannelCorePoolSize**       | rabbitmq.properties    | initial number of executor threads for broker|
|                                              |                        | message processing.                          |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.sendTimeLimit**                   | rabbitmq.properties    | the message timeout value in milliseconds    |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.sendBufferSizeLimit**             | rabbitmq.properties    | the maximum number of bytes to buffer when   |
|                                              |                        | sending messages in KB.                      |
+----------------------------------------------+------------------------+----------------------------------------------+
| **rabbitmq.messageSizeLimit**                | rabbitmq.properties    | the maximum message size in KB               |
+----------------------------------------------+------------------------+----------------------------------------------+

Test Properties
^^^^^^^^^^^^^^^
The same three properties files are available for unit tests as well, under ``src/test/resources``. Unlike the main run
configuration files, however, the test files have all of their values set in the actual files, not in the pom.

By default, the CouchDB and RabbitMQ hosts are listed as ``localhost``. Update these accordingly, if needed.
