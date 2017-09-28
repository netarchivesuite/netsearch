package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;

import com.google.common.collect.Iterables;


public class SolrClient {

  private HttpSolrServer solrServer;
  private boolean hasSource_file_field;
  private boolean hasSource_file_s_field;


  public SolrClient(String solrUrl) throws Exception{
    solrServer= new HttpSolrServer(solrUrl);                
    solrServer.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster) 

    SolrQuery query = new SolrQuery();
    query.add(CommonParams.QT, "/schema/fields");
    QueryResponse response = solrServer.query(query);
    NamedList responseHeader = response.getResponseHeader();
    ArrayList<SimpleOrderedMap> fields = (ArrayList<SimpleOrderedMap>) response.getResponse().get("fields");
    for (SimpleOrderedMap field : fields) {
      String fieldName = (String) field.get("name");   
      
      if (fieldName.equals("source_file")){
        hasSource_file_field=true;
      }
      else if (fieldName.equals("source_file_s")){
        hasSource_file_s_field=true;
      }        
    }
    
    
    if (hasSource_file_field){
      System.out.println("warc-index version 3.0 detected. Using source_file to count documents");
    }
    else if ( hasSource_file_s_field){
      System.out.println("warc-index version 2.0 detected. Using source_file_s to count documents");
    }else{
      System.out.println("Could not detect schema version. Neigther 'source_file' or 'source_file_s' field found");
      System.exit(1);
    }

  }

  /*
   * Bulk query 1000 at a time. Add results
   * 
   */
  public ArrayList<String> lookupRecords(ArrayList<String> source_files) throws Exception{

    ArrayList<String> recordsFound = new ArrayList<String>(); 

    Iterable<List<String>> splitSets = Iterables.partition(source_files, 500); //split into sets of size max 500; //wasd 1000 before 3.0 support

    for (List<String> records : splitSets){
      ArrayList<String> recs= lookupRecordsBatch(records);
      recordsFound.addAll(recs);
    }   
    return recordsFound;
  }


  /*
   * The look up is different is warc-indexer 2.0 and 3.0. But the fix is just a double query that will find match in one of the cases
   * 
   */
  private ArrayList<String> lookupRecordsBatch(List<String> source_files) throws Exception{

    if (source_files.size() > 500){
      throw new IllegalArgumentException("More than 500 different urls in query:"+source_files.size() +". Solr does not allow more more than 1000 query terms.");
    }

    ArrayList<String> records = new ArrayList<String>();
    StringBuffer buf = new StringBuffer();
    if (hasSource_file_s_field){ //warc-indexer 2.0

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

      for ( SolrDocument doc: rsp.getResults()){
        String source_file_s = (String) doc.getFieldValue("source_file_s");        
        records.add(source_file_s);              
      }
      
    }
    else{ //warc-indexer 3.0
      
      buf.append("(source_file:test"); //Just to avoid last OR logic
      for (String  url : source_files) {            
        // Split in two parts: 276693-272-20170622081520337-00007-kb-prod-har-002.kb.dk.warc.gz@762 
        int offsetIndex = url.indexOf("@");
        String warc=url.substring(0,offsetIndex);
        String offset = url.substring(offsetIndex+1);
         buf.append(" OR (source_file:\""+warc+"\" AND source_file_offset:"+offset+")");
      }
      buf.append(")");


      String  query = buf.toString();     

      SolrQuery solrQuery = new SolrQuery();
      solrQuery.setQuery(query);

      solrQuery.setRows(source_files.size());
      solrQuery.set("facet", "false"); 
      solrQuery.add("fl","source_file,source_file_offset"); //only request fields used

      QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);        

      for ( SolrDocument doc: rsp.getResults()){
        String name = (String) doc.getFieldValue("source_file");
        long offset = (Long) doc.getFieldValue("source_file_offset");
        records.add(name+"@"+offset);           
      }            
    }
    
  return records;                     
  }

  public int countRecordsForFile(String arc_full) throws Exception{

    String[] tokens = arc_full.split("/");
    String filename = tokens[tokens.length-1]; //last is filename
    
    String q=null;; //use the correct schema
    
     if (hasSource_file_field){
      q = "source_file:\""+filename+"\"";
    }
    else if ( hasSource_file_s_field){
      q="source_file_s:"+filename+"*"; // * is wildcard for all @offset suffixes.
    }    
    
    SolrQuery solrQuery = new SolrQuery();    
    solrQuery.setQuery(q);  
    solrQuery.set("fl", "id");
    QueryResponse rsp = solrServer.query(solrQuery,METHOD.POST);
    long numberFound =  rsp.getResults().getNumFound();  
 
    return (int) numberFound; //There will not be that many records in a single warc-file...  
  }

}
