package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;


import com.google.common.collect.Iterables;


public class SolrClient {

  HttpSolrServer solrServer;
  
  
  public SolrClient(String solrUrl){
    solrServer= new HttpSolrServer(solrUrl);                
    solrServer.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster) 
  }
  
  /*
   * Bulk query 1000 at a time. Add results
   * 
   */
public ArrayList<String> lookupRecords(ArrayList<String> source_files) throws Exception{
    
 ArrayList<String> recordsFound = new ArrayList<String>(); 
  
  Iterable<List<String>> splitSets = Iterables.partition(source_files, 1000); //split into sets of size max 1000;
  
  for (List<String> records : splitSets){
    ArrayList<String> recs= lookupRecordsMax1000(records);
    recordsFound.addAll(recs);
  }   
    return recordsFound;
  }

  
private ArrayList<String> lookupRecordsMax1000(List<String> source_files) throws Exception{

  if (source_files.size() > 1000){
    throw new IllegalArgumentException("More than 1000 different urls in query:"+source_files.size() +". Solr does not allow more than 1024 queries");
  }

  //Generate URL string: (url:"A" OR url:"B" OR ....)
  StringBuffer buf = new StringBuffer();
  buf.append("(source_file_s:test"); //Just to avoid last OR logic
  for (String  url : source_files) {            
    buf.append(" OR source_file_s:\""+url+"\"");        

  }
  buf.append(")");


  String  query = buf.toString();     
  SolrQuery solrQuery = new SolrQuery();
  solrQuery.setQuery(query);

  solrQuery.setRows(source_files.size());
  solrQuery.set("facet", "false"); 
  solrQuery.add("fl","id,source_file_s"); //only request fields used

  QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

  ArrayList<String>  records = new ArrayList<String>();

  for ( SolrDocument doc: rsp.getResults()){

    records.add((String) doc.getFieldValue("source_file_s"));                             
  }                    

  return records;                     
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
