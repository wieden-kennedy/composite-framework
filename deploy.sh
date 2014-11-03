#!/bin/sh

if [ -z "$TOMCAT7_HOME" ];
then
  export TOMCAT7_HOME=/usr/share/tomcat7
fi

mvn -DskipTests clean package
rm -rf $TOMCAT7_HOME/webapps/ROOT/*
cp -R target/composite/* $TOMCAT7_HOME/webapps/ROOT/
$TOMCAT7_HOME/bin/shutdown.sh && $TOMCAT7_HOME/bin/startup.sh

