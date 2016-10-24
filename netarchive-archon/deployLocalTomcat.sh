#!/bin/sh

rm  /home/teg/Desktop/apache-tomcat-8.0.23/webapps/netarchive-archon -r
mvn clean package -DskipTests
mv target/netarchive-archon*.war target/netarchive-archon.war
cp target/netarchive-archon.war /home/teg/Desktop/apache-tomcat-8.0.23/webapps

echo "netarchive-archon.war deployed to tomcat"

