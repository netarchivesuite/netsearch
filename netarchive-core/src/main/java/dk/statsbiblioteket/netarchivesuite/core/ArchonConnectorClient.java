package dk.statsbiblioteket.netarchivesuite.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
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

        //String shardID =service.path("nextShardID").get(String.class);
        ClientResponse response= service.path("nextShardID").get(ClientResponse.class);
        handleHttpExceptions(response);
        String shardID = response.getEntity(String.class);
        return shardID;
    }

    @Override
    public void addARC(String arcID){
        String urlencodedArcId= fixSlashUrlEncoding(arcID);       
        ClientResponse response = service.path("addARC").path(urlencodedArcId).post(ClientResponse.class);                    
        handleHttpExceptions(response);                    
    }

    @Override
    public String nextARC(String shardID){                   
        ClientResponse response  = service.path("nextARC").path(shardID).get(ClientResponse.class);
        handleHttpExceptions(response);
        String nextArc = response.getEntity(String.class);
        return nextArc;
    }

    @Override
    public void setARCState(String arcID, ARC_STATE state){
        String urlencodedArcId= fixSlashUrlEncoding(arcID);         
        ClientResponse response = service.path("setARCState").path(urlencodedArcId).path(state.toString()).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    @Override
    public void clearIndexing(String shardID){
        //  service.path("clearIndexing").path(shardID).post();   
        ClientResponse response  = service.path("clearIndexing").path(shardID).post(ClientResponse.class);
        handleHttpExceptions(response);        
    }

    @Override
    public void removeARC(String arcID){
        String urlencodedArcId= fixSlashUrlEncoding(arcID); 
        ClientResponse response = service.path("removeARC").path(urlencodedArcId).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    @Override
    public List<String> getShardIDs(){     
        ClientResponse response =  service.path("getShardIDs").get(ClientResponse.class);
        handleHttpExceptions(response);
        StringListWrapper wrapper = response.getEntity(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();          
        return values;              
    }

    @Override
    public List<String> getARCFiles(String shardID){
        ClientResponse response = service.path("getARCFiles").path(shardID).get(ClientResponse.class);
        handleHttpExceptions(response);        
        StringListWrapper wrapper = response.getEntity(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();        
        return values;     
    }

    @Override 
    public void setARCProperties(String arcID, String shardID, ARC_STATE state, int priority){              
        String urlencodedArcId= fixSlashUrlEncoding(arcID);     
        ClientResponse response = service.path("setARCProperties").path(urlencodedArcId).path(shardID).path(state.toString()).path(""+priority).post(ClientResponse.class);  
        handleHttpExceptions(response);
    }

    @Override
    public void setShardState(String shardID, ARC_STATE state, int priority){       
        ClientResponse response =  service.path("setShardState").path(shardID).path(state.toString()).path(""+priority).post(ClientResponse.class);       
        handleHttpExceptions(response);
    }

    private void handleHttpExceptions(ClientResponse response) {
        //200 is OK also 204 is OK (no content)
        if (!(response.getStatus() == 200 || response.getStatus() == 204)) {
            int status = response.getStatus();
            String content_message = response.getEntity(String.class);                        
            throw new IllegalArgumentException("HTTP code:"+status +" error:"+content_message);                     
        }
    }

    /*
     * the ARCName is a file-path which contains /, and these must be url encoded.
     */
    private String fixSlashUrlEncoding(String url){
        String urlencodedArcId= url.replaceAll("/", "%2F");
        return urlencodedArcId;                
    }

}
