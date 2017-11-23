package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;

public class TempTest {

  
  
  public static void main(String[] args) throws Exception {
   
    try{
    
    IndexBuilderConfig config = new IndexBuilderConfig("/netarkiv/arctika_teg_local.properties");
    IndexBuilder builder = new IndexBuilder(config);
    

    ArctikaSolrJClient solrClient = new ArctikaSolrJClient("http://localhost:8983/solr/","netarchivebuilder");
    //solrClient.optimize();
    SolrCoreStatus status = solrClient.getStatus();
    System.out.println(status);
    
    }
    catch (Exception e){
      e.printStackTrace();
    }
    
    
  }
  
}
