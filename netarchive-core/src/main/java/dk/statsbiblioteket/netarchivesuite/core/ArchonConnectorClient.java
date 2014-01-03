package dk.statsbiblioteket.netarchivesuite.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;


public class ArchonConnectorClient implements ArchonConnector{ 

    private  WebResource service;

    /**
     * Client for the REST-based webservice provided by the netachive-archon module. 
     * @param archonServerUrl url to the service on the webserver
     * For tomcat this: localhost:8080/netarchive-archon/services
     * For jetty (mvn jetty:run) this is localhost:8080/archon/services 
     */
    public ArchonConnectorClient(String archonServerUrl){
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        service = client.resource(UriBuilder.fromUri(archonServerUrl).build());
    }

    @Override
    public String nextShardID(){
        String shardID =service.path("nextShardID").get(String.class);
        return shardID;
    }

    @Override
    public void addARC(String arcID){
        String urlencodedArcId= fixSlashUrlEncoding(arcID);       
        service.path("addARC").path(urlencodedArcId).post();        
    }

    @Override
    public String nextARC(String shardID){
        String nextArc = service.path("nextARC").path(shardID).get(String.class);      
        return nextArc;
    }

    @Override
    public void setARCState(String arcID, ARC_STATE state){
        String urlencodedArcId= fixSlashUrlEncoding(arcID); 
        service.path("setARCState").path(urlencodedArcId).path(state.toString()).post();        
    }

    @Override
    public void clearIndexing(String shardID){
        service.path("clearIndexing").path(shardID).post();   
    }

    @Override
    public void removeARC(String arcID){
        String urlencodedArcId= fixSlashUrlEncoding(arcID); 
        service.path("removeARC").path(urlencodedArcId).post();              
    }

    @Override
    public List<String> getShardIDs(){     
        StringListWrapper wrapper = service.path("getShardIDs").get(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();          
        return values;              
    }

    @Override
    public List<String> getARCFiles(String shardID){
        StringListWrapper wrapper = service.path("getARCFiles").path(shardID).get(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();        
        return values;     
    }

    @Override 
    public void setARCProperties(String arcID, String shardID, ARC_STATE state, int priority){              
        String urlencodedArcId= fixSlashUrlEncoding(arcID);
        service.path("setARCProperties").path(urlencodedArcId).path(shardID).path(state.toString()).path(""+priority).post();  
    }

    @Override
    public void setShardState(String shardID, ARC_STATE state, int priority){       
        service.path("setShardState").path(shardID).path(state.toString()).path(""+priority).post();       
    }

    /*
     * the ARCName is a file-path which contains /, and these must be url encoded.
     */
    private String fixSlashUrlEncoding(String url){
        String urlencodedArcId= url.replaceAll("/", "%2F");
        return urlencodedArcId;                
    }

}
