'''
Copyright (c) 2014, Wieden + Kennedy
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the  Wieden + Kennedy, nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
'''

# !/usr/bin/python

#imports
import argparse
from subprocess import check_output, check_call, call, CalledProcessError
import json

# GLOBAL VARS
# adding true=True to account for CouchDB return value
true = True
SERVER_NAME = '0.0.0.0'
COUCHDB_PORT = '5984'
COUCHDB_ADMIN = 'admin'
COUCHDB_PASSWORD = 'password'
DATABASES = [
    {
        "name": "sessions",
        "views": {
            "_id": "design/app",
            "language": "javascript",
            "views": {
                "session-devices": {
                    "map": "function(doc){if(doc.devices){emit(doc.uuid,doc.devices)}}"
                },
                "session-by-device": {
                    "map": "function(doc){if(doc.devices){for(var i in doc.devices){emit(doc.devices[i].uuid, doc);}}}"
                },
                "uuid": {
                    "map": "function(doc){if(doc.uuid){emit(doc.uuid, doc)}}"
                },
                "session-by-timestamp": {
                    "map": "function(doc){if(doc.inserted){emit(doc.inserted, doc);}}"
                },
                "locked-sessions": {
                    "map": "function(doc){emit(doc.locked,doc)}"
                },
                "application-id": {
                    "map": "function(doc){if(doc.applicationId && !doc.locked){emit(doc.applicationId, doc)}}"
                },
                }
        }
    }
]

'''
create_database
Creates a database in a couchdb instance on server identified by SERVER_NAME and port identified by COUCHDB_PORT
If database creation is successful, database views, if any, are also created.
'''


def create_database(database):
    f = open('/usr/local/var/lib/couchdb/setup.log', 'w')
    if COUCHDB_ADMIN == '' or COUCHDB_PASSWORD == '':
        args = ['curl', '-X', 'PUT', 'http://{0}:{1}/{2}'.format(SERVER_NAME, COUCHDB_PORT, database['name'])]
    else:
        args = ['curl', '-X', 'PUT', 'http://{0}:{1}@{2}:{3}/{4}'.format(COUCHDB_ADMIN, COUCHDB_PASSWORD, SERVER_NAME,
                                                                         COUCHDB_PORT, database['name'])]
    try:
        call(args)
        f.write('%s\n' % ' '.join(args))
        f.write('%s database created successfully.\n' % database['name'])
    except:
        f.write('%s\n' % ' '.join(args))
        f.write('Error in creating %s database\n' % database['name'])

    # create views
    if not database['views']:
        return

    call(['sleep', '5'])
    design_doc = database['views']['_id']
    view_data = json.dumps(database['views'])
    if COUCHDB_ADMIN == '' or COUCHDB_PASSWORD == '':
        args = ['curl', '-X', 'PUT', '-d', '%s' % view_data, 'http://{0}:{1}/{2}/_{3}'.format(SERVER_NAME, COUCHDB_PORT,
                                                                                              database['name'], design_doc)]
    else:
        args = ['curl', '-X', 'PUT', '-d', '%s' % view_data, 'http://{0}:{1}@{2}:{3}/{4}/_{5}'.format(COUCHDB_ADMIN,
                                                                                                      COUCHDB_PASSWORD,
                                                                                                      SERVER_NAME,
                                                                                                      COUCHDB_PORT,
                                                                                                      database['name'],
                                                                                                      design_doc)]
    try:
        call(args)
        f.write('%s\n' % ' '.join(args))
        f.write('%s database views created successfully.\n' % database['name'])
    except:
        f.write('%s\n' % ' '.join(args))
        f.write(' '.join(args))
        f.write('Error in creating %s database views\n' % database['name'])


'''
delete_database
Deletes couchdb database identified by SERVER_NAME, COUCHDB_PORT, and database_name
'''


def delete_database(database_name):
    if COUCHDB_ADMIN == '' or COUCHDB_PASSWORD == '':
        args = ['curl', '-X', 'DELETE', 'http://{0}:{1}/{2}'.format(SERVER_NAME, COUCHDB_PORT, database_name)]
    else:
        args = ['curl', '-X', 'DELETE',
                'http://{0}:{1}@{2}:{3}/{4}'.format(COUCHDB_ADMIN, COUCHDB_PASSWORD, SERVER_NAME,
                                                    COUCHDB_PORT, database_name)]
    call(args)


def download_puppet_manifest():
    call(['sudo', 'apt-get', '-y', 'install', 'git', 'wget'])
    call(['git', 'clone', 'https://github.com/wieden-kennedy/puppet-module-couchdb', '/tmp/puppet-module-couchdb'])


def install_puppet():
    call(['wget', 'https://apt.puppetlabs.com/puppetlabs-release-precise.deb', '-O',
          '/tmp/puppet-labs-release-precise.deb'])
    call(['sudo', 'dpkg', '-i', '/tmp/puppetlabs-release-precise.deb'])
    call(['apt-get', '-y', 'update'])
    call(['sudo', 'apt-get', '-y', 'install', 'puppet'])


def apply_couch_module():
    couchdb_class = "class {'couchdb': bind => '0.0.0.0'}"
    call(['sudo', 'mv', '/tmp/puppet-module-couchdb', '/etc/puppet/modules/couchdb'])
    call(['sudo', 'puppet', 'apply', '-e', couchdb_class])


'''
parse_args
Parses command line flags and args.
Available arguments:
Flag                Name        Purpose
-install-couch      Install     Install couchdb with puppet on machine. Should only be run on clean ubuntu instance
-setup-databases    Setup       Sets up databases if desired, and runs after couch install if couch install is run
-flush              Flush       Flush databases before creating (if recreating databases)

Returns parsed args Namespace
'''


def parse_args():
    parser = argparse.ArgumentParser(description="command line args to assist with development testing")
    parser.add_argument('-setup-database', dest='setup', action='store_true',
                        help='pass `-setup-database to set up the databases. Databases will be installed after '
                             'couch if couch install is selected\n')
    parser.add_argument('-flush', dest='flush', action='store_true',
                        help='pass `-flush` to flush the databases before creating them. Only need be '
                             'used to recreate the database structure, in conjunction with -setup-database')
    return parser.parse_args()


if __name__ == '__main__':
    args = parse_args()
    if args.setup:
        for database in DATABASES:
            if args.flush:
                delete_database(database['name'])
            create_database(database)
