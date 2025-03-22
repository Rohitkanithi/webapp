#!/usr/bin/bash

#Changing ownership of the JAR file
sudo chown -R webappuser:webappuser /tmp/webapp-1.0-SNAPSHOT.jar

sudo mv /tmp/webapp-1.0-SNAPSHOT.jar /opt/webapp-1.0-SNAPSHOT.jar