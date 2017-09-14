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
  
  Iterable<List<String>> splitSets = Iterables.partition(source_files, 500); //split into sets of size max 500; //wasd 1000 before 3.0 support
  
  for (List<String> records : splitSets){
    ArrayList<String> recs= lookupRecordsMax1000(records);
    recordsFound.addAll(recs);
  }   
    return recordsFound;
  }

  
/*
 * The look up is different is warc-indexer 2.0 and 3.0. But the fix is just a double query that will find match in one of the cases
 * 
 */
private ArrayList<String> lookupRecordsMax1000(List<String> source_files) throws Exception{

  if (source_files.size() > 500){
    throw new IllegalArgumentException("More than 500 different urls in query:"+source_files.size() +". Solr does not allow more more than 1000 query terms.");
  }

  //Generate URL string: (url:"A" OR url:"B" OR ....)
  StringBuffer buf = new StringBuffer();
  buf.append("(source_file_s:test"); //Just to avoid last OR logic
  for (String  url : source_files) {            
    // Split in two parts: 276693-272-20170622081520337-00007-kb-prod-har-002.kb.dk.warc.gz@762 
    int offsetIndex = url.indexOf("@");
     String warc=url.substring(0,offsetIndex);
     String offset = url.substring(offsetIndex+1);
        
    buf.append(" OR source_file_s:\""+url+"\" OR (source_file:\""+warc+"\" AND source_file_offset:"+offset+")"); //First is 2.0, second is 3.        

  }
  buf.append(")");


  String  query = buf.toString();     
  SolrQuery solrQuery = new SolrQuery();
  solrQuery.setQuery(query);

  solrQuery.setRows(source_files.size());
  solrQuery.set("facet", "false"); 
  solrQuery.add("fl","id,source_file_s,source_file ,source_file_offset"); //only request fields used

  QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

  ArrayList<String>  records = new ArrayList<String>();

  for ( SolrDocument doc: rsp.getResults()){

    String arc2_0 = (String) doc.getFieldValue("source_file_s");
    if (arc2_0 != null){ //warc-indexer 2.0
      records.add(arc2_0);      
    }
    else{ //warc-indexer 3.0
      String name = (String) doc.getFieldValue("source_file");
      long offset = (Long) doc.getFieldValue("source_file_offset");
      records.add(name+"@"+offset);      
    }
    
                                 
  }                    
  return records;                     
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
