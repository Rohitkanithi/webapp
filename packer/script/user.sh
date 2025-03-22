#!/bin/bash

#Creating group webappuser
sudo groupadd webappuser

#Creating user webappuser
sudo adduser webappuser --shell /usr/sbin/nologin -g webappuser