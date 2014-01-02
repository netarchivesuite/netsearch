package dk.statsbiblioteket.netarchivesuite.core;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector.ARC_STATE;



public class ArchonConnectorClient implements ArchonConnector{ //TODO implement interface

    private  WebResource service;

    public static void main(String[] args){
        ArchonConnectorClient client = new ArchonConnectorClient("http://localhost:8080/archon/services");
        //   System.out.println(client.nextARCShardID());
        //client.addARC("home/netarc/test2s.arc");
        //  client.nextARC("1");
        //System.out.println( client.getShardIDs());
        ///  client.setARCState("home/netarc/test1s.arc", ARC_STATE.NEW);
        //
        //   client.setARCProperties("home/netarc/test1s.arc", "1", ARC_STATE.REJECTED, 5);
        // client.clearIndexing("1");
        //client.setShardState("1", ARC_STATE.NEW, 6);
        //  System.out.println(client.getARCFiles("1"));
        //  client.removeARC("home/netarc/test2s.arc");        
    }


    /**
     * Client for the REST-based webservice provided by the netachive-archon module. 
     * @param archonServerUrl url to the service on the webserver (ie. localhost:8080/archon/services)    
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
        String urlencodedArcId= arcID.replaceAll("/", "%2F");       
        service.path("addARC").path(urlencodedArcId).post();        
    }

    @Override
    public String nextARC(String shardID){
        String nextArc = service.path("nextARC").path(shardID).get(String.class);      
        return nextArc;
    }

    @Override
    public void setARCState(String arcID, ARC_STATE state){
        String urlencodedArcId= arcID.replaceAll("/", "%2F"); 
        service.path("setARCState").path(urlencodedArcId).path(state.toString()).post();        
    }

    @Override
    public void clearIndexing(String shardID){
        service.path("clearIndexing").path(shardID).post();   
    }

    @Override
    public void removeARC(String arcID){
        String urlencodedArcId= arcID.replaceAll("/", "%2F"); 
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
        String urlencodedArcId= arcID.replaceAll("/", "%2F");         
        service.path("setARCProperties").path(urlencodedArcId).path(shardID).path(state.toString()).path(""+priority).post();  

    }
    @Override
    public void setShardState(String shardID, ARC_STATE state, int priority){       
        service.path("setShardState").path(shardID).path(state.toString()).path(""+priority).post();       
    }

}
