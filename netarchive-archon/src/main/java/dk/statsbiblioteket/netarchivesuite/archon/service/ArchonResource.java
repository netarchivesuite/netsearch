package dk.statsbiblioteket.netarchivesuite.archon.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.archon.ArchonPropertiesLoader;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.ArcVO;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.H2Storage;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.ArchonServiceException;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.InternalServiceException;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.InvalidArgumentServiceException;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.StringListWrapper;



//No path except the context root+servletpath for the application. Example http://localhost:8080/licensemodule/services 
//servlet path is defined in web.xml and is  /services 

@Path("/") 
public class ArchonResource {
    private static final Logger log = LoggerFactory.getLogger(ArchonResource.class);		                                                         

    @GET			
    @Path("nextShardID")		
    @Produces({MediaType.APPLICATION_JSON})	
    public String nextShardID() throws ArchonServiceException  {        						  	    
        try {
            return ""+H2Storage.getInstance().nextShardId();	
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }           
    }

    @GET           
    @Path("find/{arcID}")        
    @Produces({MediaType.APPLICATION_JSON}) 
    public ArcVO find(@PathParam("arcID") String arcID) throws ArchonServiceException  {                                        
        try {
            return H2Storage.getInstance().getArcByID(arcID);    
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }           
    }
    
    @POST            
    @Path("addARC/{arcID}")               
    public void addARC(@PathParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
            H2Storage.getInstance().addARC(arcID); 
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    
    }
    
    @POST            
    @Path("addOrUpdateARC/{arcID}")               
    public void addOrUpdateARC(@PathParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
            H2Storage.getInstance().addOrUpdateARC(arcID); 
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }            
    }

    @GET            
    @Path("nextARC/{shardID}")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public String nextARC(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
            String nextARC = H2Storage.getInstance().nextARC(shardID);
            return nextARC;
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }                
    }

    @POST            
    @Path("setARCState/{arcID}/{state}")           
    public void setARCState(@PathParam("arcID") String arcID , @PathParam("state") String state) throws ArchonServiceException  {                                     
        try {
            ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);
            H2Storage.getInstance().setARCState(arcID, stateEnum);
            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }    

    @POST            
    @Path("clearIndexing/{shardID}")              
    public void clearIndexing(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
             H2Storage.getInstance().clearIndexing(shardID);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }

    @POST            
    @Path("resetShardID/{shardID}")              
    public void resetShardID(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
             H2Storage.getInstance().resetShardId(shardID);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }
    
    @POST            
    @Path("removeARC/{arcID}")           
    public void removeARC(@PathParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
            H2Storage.getInstance().removeARC(arcID);            
       } catch (Exception e) {
           throw handleServiceExceptions(e);
       }        
    }

    @GET            
    @Path("getShardIDs")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public StringListWrapper getShardIDs() throws ArchonServiceException  {                                     
        try {
            List<String> shardIds = H2Storage.getInstance().getShardIDs();
            StringListWrapper wrapper = new StringListWrapper(); //JSON,XML can not marshal List, need a wrapping object
            wrapper.setValues((ArrayList<String>) shardIds); 
            return wrapper;
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }      
    }

    @GET            
    @Path("getARCFiles/{shardID}")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public StringListWrapper getARCFiles(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
            List<String> ARCFiles = H2Storage.getInstance().getARCFiles(shardID);
            StringListWrapper wrapper = new StringListWrapper(); //JSON,XML can not marshal List, need a wrapping object
            wrapper.setValues((ArrayList<String>) ARCFiles); 
            return wrapper;            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }
        
    }

    @POST            
    @Path("setARCProperties/{arcID}/{shardID}/{state}/{priority}")              
    public void setARCProperties(@PathParam("arcID") String arcID,
            @PathParam("shardID") String shardID,
            @PathParam("state") String state,
            @PathParam("priority") int priority) throws ArchonServiceException  {                                     
  
        try {
            ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);
            
             H2Storage.getInstance().setARCProperties(arcID, shardID, stateEnum, priority);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }
    
    }

    @POST            
    @Path("setARCPriority/{arcID}/{priority}")              
    public void setARCProperties(@PathParam("arcID") String arcID,            
            @PathParam("priority") int priority) throws ArchonServiceException  {                                     
  
        try {            
             H2Storage.getInstance().setARCPriority(arcID, priority);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }
    
    }
    
    
    @POST            
    @Path("setShardState/{shardID}/{state}/{priority}")              
    public void setShardState(@PathParam("shardID") String shardID,
            @PathParam("state") String state,
            @PathParam("priority") int priority) throws ArchonServiceException  {                                     
   
        try {
            ArchonConnector.ARC_STATE stateEnum = ArchonConnector.ARC_STATE.valueOf(state);
            H2Storage.getInstance().setShardState(shardID, stateEnum, priority);            
       } catch (Exception e) {
           throw handleServiceExceptions(e);
       }
    }
    
    
    
    /*
     * This method is not called from frontend. It creates a backup of
     * the database.  dbBackupfolder must defined in the property-file
     *   
     */
    @POST
    @Path("system/backup_database")
    public void backupDatabase() throws ArchonServiceException  {
       // MonitorCache.registerNewRestMethodCall("backupDatabase");
        String file = ArchonPropertiesLoader.DBBACKUPFOLDER+"/"+System.currentTimeMillis()+".zip";           
        log.info("Making DB backup to:"+ file);
        try {
            H2Storage.getInstance().backupDatabase(file);   
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }
        log.info("DB backup succeeded:"+ file);
    }
    

    //This avoids have each method trying to catch 2+ exceptions with a lot of waste of code-lines
    private ArchonServiceException handleServiceExceptions(Exception e){
        if (e instanceof  ArchonServiceException){
            return (ArchonServiceException) e;  //No nothing,  exception already correct
        }				
        else if (e instanceof IllegalArgumentException){
            log.error("ServiceException(HTTP 400) in Archon:",e.getMessage());
            return new InvalidArgumentServiceException(e.getMessage());
        }
        else {//SQL and other unforseen exceptions.... should not happen.			
            log.error("ServiceException(HTTP 500) in Archon:",e);
            return new InternalServiceException(e.getMessage());
        }

    }


}