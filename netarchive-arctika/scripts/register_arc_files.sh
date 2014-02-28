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

