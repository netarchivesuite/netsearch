package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

public class SolrClient {

  HttpSolrServer solrServer;
  
  
  public SolrClient(String solrUrl){
    solrServer= new HttpSolrServer(solrUrl);                
    solrServer.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster) 
  }
  
  
  public boolean lookupRecord(String source_file_s) throws Exception{
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("source_file_s:\""+source_file_s+"\""); 
    solrQuery.set("fl", "id,source_file_s");
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    long numberFound =  rsp.getResults().getNumFound();  
    return numberFound==1;
  
  }
    
public int countRecordsForFile(String arc_full) throws Exception{
    
    SolrQuery solrQuery = new SolrQuery();
    solrQuery.setQuery("arc_full:\""+arc_full+"\""); 
    solrQuery.set("fl", "id,source_file_s");
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);

    long numberFound =  rsp.getResults().getNumFound();  
    return (int) numberFound; //There will not be that many records in a single warc-file...  
  }
  
}
