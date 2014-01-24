package dk.statsbiblioteket.netarchivesuite.arctika.solr;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.common.params.CoreAdminParams.CoreAdminAction;
import org.apache.solr.common.util.NamedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArctikaSolrJClient{
	private static HttpSolrServer solrServer;
	
	private static final Logger log = LoggerFactory.getLogger(ArctikaSolrJClient.class);

	
	public ArctikaSolrJClient(String solr_url){

        try{
            //Silent all the debugs log from HTTP Client (used by SolrJ)
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR"); 
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR"); 
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");        
            java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.OFF); 
            java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.OFF);

            solrServer = new HttpSolrServer(solr_url); //TODO factor out
            solrServer.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster) 
                
        }
        catch(Exception e){
            System.out.println("Unable to connect to netarchive indexer Solr server");
            e.printStackTrace();
            log.error("Unable to connect to netarchive indexer Solr server",e);
        }  
	    
	}
	
	
	
	public void optimize() throws Exception{
	    solrServer.optimize();	       	    
	}
	
	
	//http://127.0.0.1:8983/solr/admin/cores?action=STATUS
	public SolrCoreStatus getStatus() throws Exception{
	    CoreAdminRequest request = new CoreAdminRequest();
	    request.setAction(CoreAdminAction.STATUS);
	    CoreAdminResponse cores = request.process(solrServer);

	    SolrCoreStatus status = new SolrCoreStatus();
	    
	    // List of the cores
	    List<String> coreList = new ArrayList<String>();
	    	   	    
	    String coreName = cores.getCoreStatus().getName(0); // just 1 core	    
	    NamedList<Object> namedList = cores.getCoreStatus().get(coreName);	    	    
	    NamedList<Object> indexObj = (  NamedList<Object> ) namedList.get("index");
	    
        status.setCoreName(coreName);	    	    
	    status.setSegmentCount((Integer)indexObj.get("segmentCount"));
	    status.setIndexSizeBytes((Long)indexObj.get("sizeInBytes"));
	    status.setIndexSizeHumanReadable((String)indexObj.get("size"));
	    status.setNumDocs((Integer)indexObj.get("numDocs"));
	    
        return status;	    
	}
	
}
