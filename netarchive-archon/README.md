search-archon
=============

Tracks overall progress in indexing ARC files with the sister project ARCTika

How to set up the archon application.
1)Copy /netarchive-archon/properties/archon.properties to home-dir
2)Copy /netarchive-archon/h2/archon_h2storage.h2.db to etc homedir/archon_h2storage
3)Edit archon.properties så db-path matches where you put 2)
4) 'mvn package deploy' (in main module)
5) cd netarchive-archon and start web-serveren with 'mvn jetty:run' . Log-file are in /target/logs/jetty.log
Application can be accessed on localhost:8080/archon
Path to the REST services is localhost:8080/archon/services

When deployed in Tomcat the context-root will be netarchove-archon instead.
