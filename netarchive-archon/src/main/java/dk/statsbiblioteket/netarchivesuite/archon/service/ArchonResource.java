package dk.statsbiblioteket.netarchivesuite.archon.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.archon.facade.ArchonFacade;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.ArcVO;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.ArchonPausedServiceException;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.ArchonServiceException;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.InternalServiceException;
import dk.statsbiblioteket.netarchivesuite.archon.service.exception.InvalidArgumentServiceException;
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
            return ArchonFacade.nextShardID();	
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }           
    }

    @GET           
    @Path("find")        
    @Produces({MediaType.APPLICATION_JSON}) 
    public ArcVO find(@QueryParam("arcID") String arcID) throws ArchonServiceException  {                                        
        try {
            return ArchonFacade.getArcById(arcID);    
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }           
    }
    
    @POST            
    @Path("addARC")               
    public void addARC(@QueryParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
            ArchonFacade.addARC(arcID); 
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    
    }
    /*
     * Rewritten to query param
     */
    @POST            
    @Path("addOrUpdateARC")               
    public void addOrUpdateARC(@QueryParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
        	ArchonFacade.addOrUpdateARC(arcID); 
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }            
    }

    @GET            
    @Path("nextARC/{shardID}")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public String nextARC(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        
      //If archon is in mode Facade.isPaused do not return new jobs. Instead return HTTP 503 
      //This situation will only happen if isPaused=true is set from the archon admin webpage.
      
      if (ArchonFacade.isPaused()){
        log.info("Archon is paused, will not return nextArc until archon is active again.");                        
          throw new ArchonPausedServiceException("Archon is in paused mode. Try later");
      }
      
      try {            
        	return ArchonFacade.nextARC(shardID);        	
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }                
    }

    @POST            
    @Path("setARCState")           
    public void setARCState(@QueryParam("arcID") String arcID , @QueryParam("state") String state) throws ArchonServiceException  {                                     
        try {            
        	ArchonFacade.setARCState(arcID, state);        	            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }    

    @POST            
    @Path("clearIndexing/{shardID}")              
    public void clearIndexing(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
             ArchonFacade.clearIndexing(shardID);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }

    @POST            
    @Path("resetShardID/{shardID}")              
    public void resetShardID(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {            
        	ArchonFacade.resetShardID(shardID);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }
    
    @POST            
    @Path("removeARC")           
    public void removeARC(@QueryParam("arcID") String arcID) throws ArchonServiceException  {                                     
        try {
            ArchonFacade.removeARC(arcID);            
       } catch (Exception e) {
           throw handleServiceExceptions(e);
       }        
    }

    @GET            
    @Path("getShardIDs")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public StringListWrapper getShardIDs() throws ArchonServiceException  {                                     
        try {
           return ArchonFacade.getShardIDs();
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }      
    }

    @GET            
    @Path("getARCFiles/{shardID}")        
    @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})   
    public StringListWrapper getARCFiles(@PathParam("shardID") String shardID) throws ArchonServiceException  {                                     
        try {
         return ArchonFacade.getARCFiles(shardID);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }        
    }

    @POST            
    @Path("setARCProperties")              
    public void setARCProperties(@QueryParam("arcID") String arcID,
            @QueryParam("shardID") String shardID,
            @QueryParam("state") String state,
            @QueryParam("priority") int priority) throws ArchonServiceException  {                                     
  
        try {
         ArchonFacade.setARCProperties(arcID, shardID, state, priority);        	
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }
    
    }

    @POST            
    @Path("setARCPriority")              
    public void setARCPriority(@QueryParam("arcID") String arcID,            
            @QueryParam("priority") int priority) throws ArchonServiceException  {                                       
        try {            
           ArchonFacade.setARCPriority(arcID, priority);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }    
    }
    

    @POST            
    @Path("resetARCWithPriority")              
    public void resetArcWithPriority(@QueryParam("arcID") String arcID,            
            @QueryParam("priority") int priority) throws ArchonServiceException  {                                       
        try {            
             ArchonFacade.resetArcWithPriority(arcID, priority);            
        } catch (Exception e) {
            throw handleServiceExceptions(e);
        }    
    }
        
    @POST            
    @Path("setShardState/{shardID}/{state}/{priority}")              
    public void setShardState(@QueryParam("shardID") String shardID,
        @QueryParam("state") String state,
        @QueryParam("priority") int priority) throws ArchonServiceException  {                                     
   
        try {
           ArchonFacade.setShardState(shardID, state, priority);           
       } catch (Exception e) {
           throw handleServiceExceptions(e);
       }
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