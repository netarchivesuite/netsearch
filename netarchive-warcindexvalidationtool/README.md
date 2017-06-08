Warcindexvalidationtool
==========

**Arc/Warc validation**

The jar fil can be found in the /target folder. Notice snapshot-version can be incuded in the name.
It validates the Arc/Warc file using the archive.org Arc/Warc reader and counts the expected number of records
to be sent to the Solrserver for indexing. (Http status codes 200-299).
If a solrserver url is given as a second argument, it checks and list missing records in the Solr Index.


Example usage:

java -jar netarchive-warcindexvalidationtool-1.3-SNAPSHOT-jar-with-dependencies.jar <pathToWarcFile>

or

java -jar netarchive-warcindexvalidationtool-1.3-SNAPSHOT-jar-with-dependencies.jar <pathToWarcFile> <SolrServerUrl>

