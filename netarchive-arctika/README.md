search-arctika
==============

Builds limited size Solr indexes from ARC files using Tika. Sister project to Archon.


Background
----------

The State and University Library, Denmark, has about 372 TB ARC files with harvested web
resources as of fall 2013. The primary discovery interface will be search using Solr.
The Archon/ARCTika project aims to build index shards for use in a SolrCloud setup.


Setup
-----

The cloud setup will be using 1 TB Solid State Drives, mounted independently. Each SSD
will contain one shard, optimized down to a single segment. The goal of ARCTika is to
produce shards of sizes close to the upper limit for a single drive. Each instance of
ARCTika is responsible for building a single shard at a time and spawns a number of Tika
slave processes for the heavy analysis.


Technology
----------

ARCTika is a Java project utilizing Tika for resource analysis and Solr for indexing.
Development, stage and final deploy is done under Linux and although the technologies
used are not platform-specific, no testing will be done under OS X, Windows or other
platforms.


Active developers: Thomas Egense and Toke Eskildsen
