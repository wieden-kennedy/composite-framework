Installation
============

Composite Server
----------------

----

Minimum Requirements
~~~~~~~~~~~~~~~~~~~~
* Tomcat version 7.0.47+
* CouchDB (version 1.5.0 used in production)
* RabbitMQ (current)

Getting the Source
~~~~~~~~~~~~~~~~~~
The latest Composite backend can be fetched from github:

::

    $ git clone https://github.com/wieden-kennedy/composite


Setting up CouchDB
~~~~~~~~~~~~~~~~~~

OS X
^^^^
To set up CouchDB on OS X, we suggest you use Homebrew:

::

    $ brew install couchdb

Ubuntu
^^^^^^
We have created a setup script that works with Ubuntu 12.04 and 14.04. It's the easiest way to get CouchDB running:

::

    $ git clone https://gist.github.com/8df5e450f34248ad1679.git couchdb_bootstrap
    $ cd couchdb_bootstrap && /bin/bash run.bash

----

Setting up RabbitMQ
~~~~~~~~~~~~~~~~~~~

OS X
^^^^
To set up RabbitMQ on OS X, we suggest you use Homebrew:

::

    $ brew install rabbitmq

Ubuntu
^^^^^^
We have created a setup script that works with Ubuntu 12.04 and 14.04. It's the easiest way to get RabbitMQ running:

::

    $ git clone https://gist.github.com/keithhamilton/f2e20127f52618748266 rabbit_bootstrap
    $ /bin/bash rabbit_bootstrap/run.sh
    $ rabbitmq-server start


----

Building Composite
~~~~~~~~~~~~~~~~~~
The project is built using Maven, and is packaged as a war file. When building, the ``-Denv`` flag is used to indicate
which environment should be used to build the project.

Profiles
^^^^^^^^
The maven POM is currently configured for building in the following environments:

* local
* dev
* test (i.e., staging)
* prod
* unit

The desired build environment is defined using the -Denv flag, as follows:

```mvn -Denv=local clean package```

The above will build the war using the properties files located in ```src/main/classpath/local```.

When building for the ```unit``` profile (for unit testing), the properties files located in ```src/main/test/resources``` will be used.

Composite Client
----------------

----

Getting the Source
~~~~~~~~~~~~~~~~~~
The latest Composite frontend can be fetched from github:

::

    $ git clone https://github.com/wieden-kennedy/composite-client

Using the Client
~~~~~~~~~~~~~~~~
To use the client, first grab the ``/build/min/composite.min.js`` file from the client source,
then add the following into the head of your HTML document:

::

    <script src="/path/to/composite.min.js"></script>

Building with Gulp
~~~~~~~~~~~~~~~~~~
To build the client from source, you will need to first have the following installed:

* Nodejs + NPM
* Git
* Gulp

Once these are installed, building the client is easy:

::

    $ git clone https://github.com/wieden-kennedy/composite-client
    $ cd composite-client
    $ npm install

This will build the human-readable ``build/dev/composite.js`` file and the production-ready ``build/min/composite.min.js`` file.

You can also compile on save with ``gulp watch``

Autobuild (OS X + Ubuntu)
~~~~~~~~~~~~~~~~~~~~~~~~~
We've created an autobuild script you can use to get the client and build it, which can be run thusly:

::

    $ wget https://raw.github.com/wieden-kennedy/composite-client/master/autobuild.sh
    $ /bin/bash autobuild.sh

Autobuilding is supported on Ubuntu and OS X, but if you are running a different Debian flavor,
you can attepmt to force the autobuild to run by adding the ``--force`` flag:

::

    $ wget https://raw.github.com/wieden-kennedy/composite-client/master/autobuild.sh
    $ /bin/bash autobuild.sh --force

