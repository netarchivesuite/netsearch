Description:
Archon is a web application that keeps track of which ARC-files are in which SOLR-index.
See it as a book keeping for ARC-files. If index-files are lost, they can be rebuild with 
the information about which ARC-files they had.
   
Installation:
netarchive-archon.war  <- the WEB-archive, deployed to a tomcat.
archon_h2storage.h2.db  <- The empty H2 database with tables created.
archon.properties   <- To the home-directory. Specifies there the h2-database is 

Rest-interface.
An nice API page can be seen at: http://belinda:9721/netarchive-archon/   (API tab)
There are more methods that will be described later, but they are only necessary in case of something goes wrong.

Example script that registre all arc files from a folder (/netarkiv/01/0003/filedir/) in archon:
Notice this will also registre the meta-data files, and this should be kept out. But nothing happens
if they are added.  




#!/bin/bash
FILES=/netarkiv/01/0003/filedir/*
for f in $FILES
do
  echo "processing: $f"
  ## urlencode all / to %2F
  f_urlencoded=`echo $f | sed 's/\//%2F/g'`
  echo "curl --data '' http://belinda:9721/netarchive-archon/services/addOrUpdateARC/$f_urlencoded"  

  curl --data ' ' http://belinda:9721/netarchive-archon/services/addOrUpdateARC/$f_urlencoded

done
 