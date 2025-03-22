#!/bin/bash

#Install Java
sudo dnf install java-17-openjdk-devel.x86_64 -y

#Install mySQL
# sudo dnf install mysql mysql-server -y
# sleep 5

#Start mySQL
# sudo systemctl enable mysqld
# sudo systemctl start mysqld

# sleep 5

#Alter Password of mySQL
# sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';"