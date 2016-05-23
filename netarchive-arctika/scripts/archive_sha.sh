#!/bin/bash

#
# Takes the sha1 and represents it in Base32 (see RFC 4648).
#

B32="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

if [ ! -f "$1" ]; then
   echo "Usage: ./archive_sha file <LIMIT>"
fi

for FILE in $@; do
    SHA=`cat "$FILE" | sha1sum -b | grep -o "^[^ ]*"`
    SHA=${SHA^^}
    SHA_B32_SPACE=`echo "obase=32 ; ibase=16 ; $SHA" | bc | tr '\n' ' ' | sed 's/\\\\ //'`
    
    for ENTRY in $SHA_B32_SPACE; do
        SANS_ZERO=`echo "$ENTRY" | sed 's/^0\(.\)$/\1/'`
        SHA_B32="$SHA_B32"${B32:$SANS_ZERO:1}
    done
    echo "$FILE: $SHA_B32"
done
