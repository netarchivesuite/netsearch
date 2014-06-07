Netsearch
==========

**Abstract**

Netarchive is a open source Maven project that can process a very large number of arc/warc-files (Web ARChive file format) and make the content of the archive
searchable in a Solr-server cluster (SolrCloud). The search-results can then be shown in the WebArchive viewer.

**Scalability**

The solution is scalable with growing index-size without reducing the search performance. More specific we require non-faceted/grouping search times to be very fast &lt; 200ms
and faceted/grouping search time &lt; 2000ms.

**Software components**

1. Archon. WAR-application that keeps track of the arc/warc files book keeping. Uses a DB (H2) for persistence.
2. Arctika: Java program that builds a given index(shard) and manage a worker pool of jobs that each process a arc/warc-file and submits the extracted meta-data to solr. The workers uses (https://github.com/ukwa/webarchive-discovery) for reading the arc-files using Tika for text extraction.
3. A Solr-Cloud cluster where you can add new servers(shards). Each index is put into a Solr server instance(shard) in the cluster.  A zookeeper emsemble monitors the cluster.
4. Front-end server for searching and showing the results. We use the open source project SolrAjax for this. This will likely be replaced with a better front-end solution later.
5. WebArchive server (Front-end server for displaying the websites)

**Hardware configuration**

1. Index-builder server
  * This server runs a single instance of Solr with the solely purpose of producing optimized indexes of a given size, which are then feed to the solr-cluster. The Solr server require 32 GB ram if you are building 1 TB index and (probably) ram requirement scales with index-size. The Archon application also runs on this server for simplicity, but Archon could be running on a completely different server if needed. The webserver running archon will perform better with more ram allocated due to the H2 database memory cache, we have started the web server with 4 GB. The Arctika process is also running on this server and starts a given number of workers running simultaneous.  Each worker needs 1 GB ram for the Tika process, this can maybe be reduced, but better safe than sorry. Each worker require 1 core(hypertthreaded) to run 'optimally'. When running 40 workers it takes 1 week to build  a 1 TB  optimized index. Again (as with Archon) the Arctika process can run on a different server than the index-builder Solr instance. 
  * Spec:
  * 24 cores (hyperthreaded to 48 CPU)
  * 256 GB ram. 
  * 5+ TB SSH storage for the index location and tika-temp folder. When optimizing the index, it can teoretically grow to three times the size.  Also the Tika-temp folder can grow to 500MB when building a 1 TB index. So if you are building a 1 TB index you need at least 4 TB device space.

2. Solr-Cluster server(s)
 * Runs a zookeeper ensemble (3 zookeeper instances) and a SolrCloud cluster. The SolrCloud setup has a solr master(also a shard) and a number of solr servers (shards).  Each solr-instance runs a index stored on a seperated SSD disc.
If you are not using SSD disc the performance will suffer by a factor 10+. Each shard only consist of a single server, but using replica servers for a shard is a very easy to configure. 
You can run the zookeeper  ensemble on a difference machine to avoid single point of failure, but so far we have had no stability issues what so ever with SolrCloud and Zookeeper.
When we are using 1 TB index and facetting on 6 fields, each Solr instance require a minimum of 8GB ram. Using 2 TB indexes probably require 12-16 GB ram for each Solr instance.
If you are not using facetting/grouping each Solr instance only require 4 GB ram.


**Arcfiles/index ration**

100000 arc/warc files (100MB each) produces ~1 TB index (optimized)


**Netsearch on GitHub**

https://github.com/netarchivesuite/netsearch

**Releases**

For a full install of Arctika,Archon,SolrCloud and Zookeeper you can download the full release package here:
https://github.com/netarchivesuite/netsearch/tree/master/releases/version1.0
Each folder has an install guide. You only need to git clone and build the warc-indexer project for a jar-file, the rest
is included in the release package.

**Performance test**

https://plus.google.com/+TokeEskildsen/posts/4yPvzrQo8A7


Thomas Egense
2014-06-06



