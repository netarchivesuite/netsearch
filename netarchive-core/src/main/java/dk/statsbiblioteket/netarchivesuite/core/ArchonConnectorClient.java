package dk.statsbiblioteket.netarchivesuite.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;


public class ArchonConnectorClient implements ArchonConnector{ 
    private static final Logger log = LoggerFactory.getLogger(ArchonConnectorClient.class);
    public static final String WAIT_MODE="WAIT_MODE"; 
    
    private final WebResource service;

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
        log.info("Constructed connector to '" + archonServerUrl + "'");
    }

    @Override
    public String nextShardID(){                      
        //String shardID =service.path("nextShardID").get(String.class);
        ClientResponse response= service.path("nextShardID").get(ClientResponse.class);
        handleHttpExceptions(response);
        String shardID = response.getEntity(String.class);
        log.debug("nextShardID resolved " + shardID);
        return shardID;
    }

    @Override
    public void addARC(String arcID){
        log.debug("addARC(arcID=" + arcID + ") called");
        String urlencodedArcId= fixSlashUrlEncoding(arcID);       
        ClientResponse response = service.path("addARC").path(urlencodedArcId).post(ClientResponse.class);                    
        handleHttpExceptions(response);                    
    }

    @Override
    public String nextARC(String shardID){
        log.debug("nextARC(shardID=" + shardID + ") called");
        ClientResponse response  = service.path("nextARC").path(shardID).get(ClientResponse.class);
        
        int status = response.getStatus(); //Handle 503,  archon in wait mode.
        if (status == 503){
          return WAIT_MODE;
        }                
        handleHttpExceptions(response); 
        
        String nextArc = response.getEntity(String.class);
        log.debug("nextARC(shardID=" + shardID + ") resolved '" + nextArc + "'");
        return nextArc;
    }

    @Override
    public void setARCState(String arcID, ARC_STATE state){
        log.debug("setARCState(arcID=" + arcID + ", state=" + state + ") called");
        String urlencodedArcId= fixSlashUrlEncoding(arcID);         
        ClientResponse response =
                service.path("setARCState").path(urlencodedArcId).path(state.toString()).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    @Override
    public void setARCStates(Collection<String> arcIDs, ARC_STATE state){
        for (String arcID: arcIDs) {
            setARCState(arcID, state);
        }
    }

    @Override
    public void clearIndexing(String shardID){
        log.debug("clearIndexing(shardID=" + shardID + ") called");
        //  service.path("clearIndexing").path(shardID).post();   
        ClientResponse response  = service.path("clearIndexing").path(shardID).post(ClientResponse.class);
        handleHttpExceptions(response);        
    }

    @Override
    public void removeARC(String arcID){
        log.debug("removeARC(arcID=" + arcID + ") called");
        String urlencodedArcId= fixSlashUrlEncoding(arcID);
        ClientResponse response = service.path("removeARC").path(urlencodedArcId).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    @Override
    public List<String> getShardIDs(){
        log.debug("getShardIDs() called");
        ClientResponse response =  service.path("getShardIDs").get(ClientResponse.class);
        handleHttpExceptions(response);
        StringListWrapper wrapper = response.getEntity(StringListWrapper.class);
        ArrayList<String> ids = wrapper.getValues();
        log.debug("getShardIDs() resolved " + toString(ids));
        return ids;
    }

    @Override
    public List<String> getARCFiles(String shardID){
        log.debug("getARCFiles(shardID=" + shardID + ") called");
        ClientResponse response = service.path("getARCFiles").path(shardID).get(ClientResponse.class);
        handleHttpExceptions(response);        
        StringListWrapper wrapper = response.getEntity(StringListWrapper.class);
        ArrayList<String> arcs = wrapper.getValues();
        log.debug("getARCFiles(shardID=" + shardID + ") resolved " + toString(arcs));
        return arcs;
    }

    @Override 
    public void setARCProperties(String arcID, String shardID, ARC_STATE state, int priority){
        log.debug("setARCProperties(arcID=" + arcID + ", shardID=" + shardID + ", state=" + state
                  + ", priority=" + priority + ") called");
        String urlencodedArcId= fixSlashUrlEncoding(arcID);     
        ClientResponse response = service.path("setARCProperties").path(urlencodedArcId).path(shardID).
                path(state.toString()).path(""+priority).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    @Override
    public void setShardState(String shardID, ARC_STATE state, int priority){
        log.debug("setShardState(shardID=" + shardID + ", state=" + state + ", priority=" + priority + ") called");
        ClientResponse response =  service.path("setShardState").path(shardID).path(state.toString()).
                path("" + priority).post(ClientResponse.class);
        handleHttpExceptions(response);
    }

    private void handleHttpExceptions(ClientResponse response) {
        //200 is OK also 204 is OK (no content)
        if (!(response.getStatus() == 200 || response.getStatus() == 204)) {
            int status = response.getStatus();
            String content_message = response.getEntity(String.class);                        
            throw new IllegalArgumentException("HTTP code: "+status +" error: "+content_message);
        }
    }

    /*
     * the ARCName is a file-path which contains /, and these must be url encoded.
     */
    private String fixSlashUrlEncoding(String url){
        return url.replace("/", "%2F");
    }


    private StringBuilder toString(ArrayList<String> ids) {
        StringBuilder sb = new StringBuilder(ids.size() * 10);
        for (String id: ids) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(id);
        }
        return sb;
    }

}
