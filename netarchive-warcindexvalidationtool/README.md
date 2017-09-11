Warcindexvalidationtool
==========

**Arc/Warc validation**

The jar fil can be found in the /target folder. Notice snapshot-version can be incuded in the name.
It validates the Arc/Warc file using the archive.org Arc/Warc reader and counts the expected number of records
to be sent to the Solrserver for indexing. 
If a solrserver url is given as a second argument, it checks and list missing records in the Solr Index. This also require a list of http status prefix codes. 


Example usage:

java -jar netarchive-warcindexvalidationtool-1.3-SNAPSHOT-jar-with-dependencies.jar pathToWarcFile

or

java -jar netarchive-warcindexvalidationtool-1.3-SNAPSHOT-jar-with-dependencies.jar pathToWarcFile SolrServerUrl 2
(only accept http 2xx status codes).
java -jar netarchive-warcindexvalidationtool-1.3-SNAPSHOT-jar-with-dependencies.jar pathToWarcFile SolrServerUrl 2 4
(only both http 2xx and 4xx status status codes).


Example run:

Reading warc file:/netarkiv/0211/filedir/48477-91-20090610093830-00033-sb-prod-har-005.arc

File:48477-91-20090610093830-00033-sb-prod-har-005.arc

Read complete

File:/netarkiv/0211/filedir/48477-91-20090610093830-00033-sb-prod-har-005.arc

Total records:5938

Expected Solr documents:5147

Warc types:

Http codes:

  200:5147
  
  300:1
  
  302:620
  
  404:169
  
  500:1
  
Validating records are found in Solr...

The Solr index does not have the correct number of documents! File:5147 solr index:5100

Checking every document... (can takes up to 30 minutes)

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99229491 

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99241094 

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99280471

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99331009

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99362656

...

Missing solr document:48477-91-20090610093830-00033-sb-prod-har-005.arc@99989692

Validation error! Number of missing documents:47
