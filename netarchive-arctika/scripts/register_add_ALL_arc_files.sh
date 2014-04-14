#!/bin/bash

for d in $(ls -d /netarkiv/*/*/filedir /netarkiv/*/filedir 2>/dev/null)
do
  echo "processing: $f"
  ## urlencode all / to %2F
  f_urlencoded=`echo $f | sed 's/\//%2F/g'`
  ##echo "curl --data '' http://belinda:9721/netarchive-archon/services/addARC/$f_urlencoded"  

  curl --data ' ' http://belinda:9721/netarchive-archon/services/addARC/$f_urlencoded

done
