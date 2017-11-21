#!/usr/bin/env bash

#
# Dummy script that ignores all processing and reports that all jobs succeds
#

# Input: solr warc*

echo "Solr: $1"
shift # Ignore Solr
for W in "$@"; do
    echo "0 $W"
done
