#!/bin/bash

set -e # exit on errors

cache=$HOME/var/register-arc-files

exec 200> $cache/lock
flock -n 200 || exit

# first update cache
for d in /netarkiv/*/filedir ; do
    id=${d%/filedir}
    id=${id##*/}
    c=$cache/$id.cache
    ts=$c.timestamp
    if [ -f $ts ] ; then
	if [ $d -nt $ts ] ; then
	    :
	else
	    continue
	fi
    fi
    touch -r $d $ts.new
    ls -1f $d | grep -v ^[.] > $c.new
    mv $c.new $c
    mv $ts.new $ts
done

# register new files
for f in $cache/*.cache ; do
    id=${f%.cache}
    r=$id.registered
    id=${id##*/}
    touch $r
    d=/netarkiv/$id/filedir/
    url=http://belinda:9721/netarchive-archon/services/addOrUpdateARC?arcID=$d
    comm -23 <(sort $f) <(sort $r) |
	while read fn ; do
           curl --silent --data '' ${url}$fn	
        done
    cp $f $r.new
    mv $r.new $r
done

