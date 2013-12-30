package dk.statsbiblioteket.netarchivesuite.archon.service;

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

import dk.statsbiblioteket.netarchivesuite.archon.service.dto.StringListWrapper;


public class ArchonClient { //TODO implement interface
    
    private String archonServerUrl;
    private  WebResource service;
    
    public static void main(String[] args){
        ArchonClient client = new ArchonClient("http://localhost:8080/archon/services");
   //   System.out.println(client.nextARCShardID());
       //client.addARC("home/netarc/test2s.arc");
     //  client.nextARC("1");
    //    client.getShardIDs();
   // client.setARCState("home/netarc/test2s.arc", "RUNNING");
     //
   //     client.setARCProperties("home/netarc/test1s.arc", "1", "RUNNING", 5);
       // client.clearIndexing("1");
       // client.setShardState("1", "COMPLETED", 6);
  //  System.out.println(client.getARCFiles("1"));
      client.removeARC("home/netarc/test2s.arc");        
    }
    
    
    
    public ArchonClient(String archonServerUrl){
        this.archonServerUrl=archonServerUrl;
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        service = client.resource(UriBuilder.fromUri(archonServerUrl).build());
    }

    public String nextARCShardID(){
        String shardID =service.path("nextShardID").get(String.class);
        return shardID;
    }
    
    public void addARC(String arcID){
      String urlencodedArcId= arcID.replaceAll("/", "%2F");       
      service.path("addARC").path(urlencodedArcId).post();        
    }
    
    public String nextARC(String shardID){
        String nextArc = service.path("nextARC").path(shardID).get(String.class);
        System.out.println("nextArc:"+nextArc);      
       return nextArc;
    }

    
    public void setARCState(String arcID, String state){
        String urlencodedArcId= arcID.replaceAll("/", "%2F"); 
        service.path("setARCState").path(urlencodedArcId).path(state).post();        
    }
    
    
    public void clearIndexing(String shardID){
        service.path("clearIndexing").path(shardID).post();   
    }
    
    
    public void removeARC(String arcID){
        String urlencodedArcId= arcID.replaceAll("/", "%2F"); 
        service.path("removeARC").path(urlencodedArcId).post();              
    }
    
    public List<String> getShardIDs(){     

        StringListWrapper wrapper = service.path("getShardIDs").get(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();          
        return values;              
    }
    
   
    public List<String> getARCFiles(String shardID){
        StringListWrapper wrapper = service.path("getARCFiles").path(shardID).get(StringListWrapper.class);
        ArrayList<String> values = wrapper.getValues();        
        return values;     
        
    }
    
   public void setARCProperties(String arcID, String shardID, String state, int priority){              
       String urlencodedArcId= arcID.replaceAll("/", "%2F");         
       service.path("setARCProperties").path(urlencodedArcId).path(shardID).path(state).path(""+priority).post();  
       
   }
   
   public void setShardState(String shardID, String state, int priority){       
       service.path("setShardState").path(shardID).path(state).path(""+priority).post();       
   }
    
    
}
