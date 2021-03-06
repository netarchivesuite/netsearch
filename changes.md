# Major changes

## Version 3.1
Java6 -> java 8
Tomcat 6 -> tomcat 8.5 
The tomcat required all method with arcID as pathparam had to be changed to queryparam 


## Premise

This documents covers significant changes between major versions.
For a detailed changelog on indexing, see the webarchive-discovery project at
https://github.com/ukwa/webarchive-discovery

## History

 * Version 1 was short-lived and never went to production
 * Version 2 was used in production at the Royal Danish Library for 2 years (2016 to May 2018)
 * Version 3 is expected to enter production May 2018

## Changes from version 2 to version 3


### Solr 4 to Solr 7

Version 3 of Netsearch uses Solr 7, where version 2 used Solr 4.
Detailed information on changes from Solr 4 to 7 can be found at
http://lucene.apache.org/solr/7_3_0/changes/Changes.html

With a 3 major versions bump, there are a significant amount of new features. However, Solr
strives to maintain backwards compatibility so existing applications and scripts should work
with only small changes.


New features that has special potential for Netarchive Search are

 * JSON Facet API: Flexible faceting with nesting and aggregations: "Find the average file sizes for all different MIME-types"
 * Streaming API: SQL expressions, advanced aggregations such as histograms, complete faceting (all results no matter the result set size)
 * Streaming exports: No limit on data export size (simulated in version 2/Solr 4 by using client-side code)
 * Graph queries: "Find all records that has the same hash as any record from a given seed domain"

### Indexing changes

Resources for all HTTP-codes are now indexed, including 404 Not Found and 302 Redirect. This makes it possible to track the full life cycle of a web resource.

### Solr schema changes

The Solr schema defines which fields the index contains and how they behave in Solr.

Version 3 of Netsearch uses the Solr 7 schema located in
netarchive-arctika/properties/solr7.0/schema.xml

#### Changed fields

There has been a major clean up of the schema from Netsearch version 2 to 3. However, all previous
fields are still available and from a client perspective they should behave the same.

The exception to the rule are the fields
 * content
 * content_type
 * hash
 * last_modified
 * last_modified_year
which has been changed from multi-value to single-value. The difference is that a single
value is returned instead of a list and that `content_type`, `hash`, `last_modified` and 
`last_modified_year` can now be used for sorting.


Most stored field has been changed to DocValues. In Solr 7 the content of a DocValue-field is returned
just as the content from a stored field is, so this does not break the document display in clients.

Fields with DocValues can be used efficiently for exporting, faceting, grouping and other aggregations.
E.g. it is now possible to perform faceting on nearly all fields.

#### New fields

* `exif_location` contains coordinates extracted from image EXIF, which can be used for geographical search
* `index_time` is a timestamp for when the Solr document was indexed, usable for checking for new content or freezing a corpus-defining filter to how the index was at freeze time
* `links_images` contains links to images shown on the page, usable for freetext-based image search
* `redirect_to_norm` contains redirect information, usable with graph queries to get to the resource behind the redirect
* `resourcename` is the last part of the resource, typically a filename
* `status_code` HTTP status-code

To be described: `links_norm, record_type, source_file, source_file_offset, source_file_path, tpye, url_path, url_search, warc_ip, warc_key_id`.

#### Misc

*Note* The field `url_norm`, contains a normalised version of the raw `url`. The field `links`, which
contains outgoing links for a resource, used the same normalisation. The normalisation attempts to
disambiguate the URLs so `http://example.com/foo%20bar/` is the same as `https://example.com/foo+bar`
in the index. Unfortunately the `www.`-prefix is not removed (this will be fixed in version 4), so
any code using the two fields to create graphs of interconnected resources need to take this into
account.

