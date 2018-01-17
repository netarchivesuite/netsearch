package dk.statsbiblioteket.netarchivesuite.arctika.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexBuilder;

import java.io.IOException;

public class ArctikaSolrJClient{
    private static final Logger log = LoggerFactory.getLogger(ArctikaSolrJClient.class);
    private static SolrClient solrUrlWithCollection;	
    private static SolrClient solrCoreAdminServer;
	
    
	public ArctikaSolrJClient(String solrUrl, String coreName){
	  String solrUrlWithCore = null;
	  try{
          removeHttpLogSpam();
          int timeout5Min = 5*60*1000; //5minutes.. 
          int timeout4Hours = 4*60*60*1000; //4 hours. 
          
          
          String solrCollection= IndexBuilder.getSolrUrlWithCollection(solrUrl, coreName);          
          
          //long timeout, since coreadmin is called when optimizing
          solrCoreAdminServer= new HttpSolrClient.Builder(solrUrl).withSocketTimeout(timeout5Min).build(); //Must be without collectionname can not use solrServer                 
          solrUrlWithCollection =  new HttpSolrClient.Builder(solrCollection).withSocketTimeout(timeout4Hours).build();          
  
	  }
        catch(Exception e){
            System.out.println("Unable to connect to netarchive indexer Solr server:"+solrUrlWithCore);
            e.printStackTrace();
            log.error("Unable to connect to netarchive indexer Solr server:"+solrUrlWithCore,e);       
        }  	    
	}

	public void flush() {
		final long startTime = System.nanoTime();
		try {
			log.info("Calling commit with SolrJ");
			solrUrlWithCollection.commit(true, true);
		} catch (Exception e) {
			String message = "Exception while calling commit with waitFlush=true, waitSearcher=true";
			log.error(message);
			throw new RuntimeException(message, e);
		}
		log.info("Finished commit with SolrJ in " + (System.nanoTime()-startTime)/1000000/1000 + " seconds");

	}

	public void optimize() throws IOException, SolrServerException {
		final long startTime = System.nanoTime();
		flush();
		try {
			log.info("Calling optimize with SolrJ");
			solrUrlWithCollection.optimize(false,false);
		} catch (Exception e) {
			String message = "Exception while calling optimize with waitFlush=false, waitSearcher=false";
			log.error(message);
			throw new RuntimeException(message, e);
		}
		log.info("Finished commit + optimize with SolrJ in " + (System.nanoTime()-startTime)/1000000 + "ms");
	}

	//http://127.0.0.1:8983/solr/admin/cores?action=STATUS
	public SolrCoreStatus getStatus() throws IOException, SolrServerException {
       	  
	  int attemps = 0;
	  while (attemps++ <3){
	    try{	     
	      return getStatusImpl();
	    }
	    catch(Exception e){
	      log.warn("Failed getting core stats, will wait 5 min");
	     try{
	      Thread.sleep(5*60*1000);
	     }
	     catch(Exception e2){
	       //ignore
	     }
	    }	   
	  }
	  
      throw new IOException("Could not get core status, index is probably still optimizing or may be finished optimizing");	  
	  
	}
	
	@SuppressWarnings("unchecked")
    public SolrCoreStatus getStatusImpl() throws IOException, SolrServerException {
          
      CoreAdminRequest request = new CoreAdminRequest();
        request.setAction(CoreAdminAction.STATUS);
        CoreAdminResponse cores = request.process(solrCoreAdminServer);

        SolrCoreStatus status = new SolrCoreStatus();
        
        //This is the solr way to represent XML/JSON. You must know attributes names
        String coreName = cores.getCoreStatus().getName(0); // Exactly 1 core define for index building. TODO match core        
        NamedList<Object> namedList = cores.getCoreStatus().get(coreName);              
        NamedList<Object> indexObj = ( NamedList<Object> ) namedList.get("index"); //Unchecked cast
        
        status.setCoreName(coreName);               
        status.setSegmentCount((Integer)indexObj.get("segmentCount"));
        status.setIndexSizeBytes((Long)indexObj.get("sizeInBytes"));
        status.setIndexSizeHumanReadable((String)indexObj.get("size"));
        status.setNumDocs((Integer)indexObj.get("numDocs"));
        
        return status;      
    }
	
	private void removeHttpLogSpam(){
	  //Silent all the debugs log from HTTP Client (used by SolrJ). 
	    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR"); 
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR"); 
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");        
        java.util.logging.Logger.getLogger("org.apache.http").setLevel(java.util.logging.Level.OFF);
	}
	
	
}