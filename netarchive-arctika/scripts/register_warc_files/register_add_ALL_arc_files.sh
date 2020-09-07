#!/bin/bash

for d in /netarkiv/*/filedir ; do
    for f in $d/* ; do
	url_filename=$f
	curl --silent --data '' http://belinda:9721/netarchive-archon/services/addOrUpdateARC?arcID=$url_filename
    done
done
