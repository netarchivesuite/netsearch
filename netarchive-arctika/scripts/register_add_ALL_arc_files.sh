#!/bin/bash

for ARC_DIR in $(ls -d /netarkiv/*/*/filedir /netarkiv/*/filedir 2>/dev/null)
do
  echo "processing directory: $ARC_DIR"
  for f in $(find $ARC_DIR -maxdepth 1 -type f -name '*.arc' 2>/dev/null)
  do
    full_path=$f
    echo "processing arc-file: $full_path"
    ## urlencode all / to %2F
    f_urlencoded=${full_path////%2F}
    ##echo "curl --data '' http://belinda:9721/netarchive-archon/services/addARC/$f_urlencoded"  
    curl --data ' ' http://belinda:9721/netarchive-archon/services/addARC/$f_urlencoded 
   done
done

# vim: set ft=sh sw=2 sts=2 : #
