Composite CouchDB View Reference
================================
Composite instances use the below views to retrieve documents from the CouchDB database via the SessionRepository class.

application-id
~~~~~~~~~~~~~~
Used to retrieve sessions by application id. Used when multiple Composite-based applications are working from the same
server or set of resources.

.. code:: javascript

    function(doc) {
        if(doc.applicationId && !doc.locked){
            emit(doc.applicationId, doc)
        }
    }

locked-sessions
~~~~~~~~~~~~~~~
Used to retrieve a list of sessions, keyed by their lock status.

.. code:: javascript

    function(doc){
        emit(doc.locked, doc)
    }

session-by-device
~~~~~~~~~~~~~~~~~
Used to retrieve a set of sessions keyed by device UUID.

.. code:: javascript

    function(doc) {
        if(doc.devices){
            for(var i in doc.devices){
                emit(doc.devices[i].uuid, doc)
            }
        }
    }

session-devices
~~~~~~~~~~~~~~~
Used to retrieve a set of devices in a session.

.. code:: javascript

    function(doc) {
        if(doc.devices){
            emit(doc.uuid, doc.devices)
        }
    }

session-by-timestamp
~~~~~~~~~~~~~~~~~~~~
Used to retrieve a session by its inserted timestamp.

.. code:: javascript

    function(doc) {
        if(doc.inserted){
            emit(doc.inserted, doc)
        }
    }

uuid
~~~~
Used to retrieve a session by its UUID.

.. code:: javascript

    function(doc) {
        if(doc.uuid){
            emit(doc.uuid, doc)
        }
    }
