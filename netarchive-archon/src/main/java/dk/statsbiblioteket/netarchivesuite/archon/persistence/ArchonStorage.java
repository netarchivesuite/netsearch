package dk.statsbiblioteket.netarchivesuite.archon.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.archon.ArchonPropertiesLoader;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;

/*
 * Persistance layer. Can use any JDBC driver.
 * Must be initialized before use.
 * Uses a connectionpool 
 */
public class ArchonStorage {

    private static final Logger log = LoggerFactory.getLogger(ArchonStorage.class);

    private static BasicDataSource dataSource = null;
    private Connection connection = null;

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;
    
    // Table and column names
    private static final String ARCHON_TABLE = "ARCHON";

    private static final String ARC_FILE_ID_COLUMN = "ARC_FILE_ID";
    private static final String CREATED_TIME_COLUMN = "CREATED_TIME";
    private static final String PRIORITY_COLUMN = "PRIORITY";
    private static final String ARC_STATE_COLUMN = "ARC_STATE";
    private static final String SHARD_ID_COLUMN = "SHARD_ID";
    private static final String MODIFIED_TIME_COLUMN = "MODIFIED_TIME";    
    private static final String FOLDER_COLUMN = "FOLDER";
    private static HashMap<String,ArrayList<ArcVO>> nextArcIdsCached= new HashMap<String,ArrayList<ArcVO>>();


    //private static final String ID_COLUMN = "ID"; // ID used for all tables

    private final static String selectNextShardIdQuery = 
            " SELECT MAX("+SHARD_ID_COLUMN+") FROM " + ARCHON_TABLE;

    private final static String selectCountArcsQuery = 
            " SELECT COUNT(*) AS COUNT FROM " + ARCHON_TABLE;


    //select FILENAME from ARCHON where status ='NEW' AND ( SHARD_ID = 10 OR SHARD_ID is null) order by shard_ID desc, priority desc, filename asc limit  1
    private final static String selectNextArcQuery =
            "SELECT *"+            
                    " FROM " + ARCHON_TABLE            
                    +" WHERE "+ ARC_STATE_COLUMN+"  = 'NEW'"
                    +"  AND ("+SHARD_ID_COLUMN+" = ?  OR "+ SHARD_ID_COLUMN +" IS NULL )"
                    +" ORDER BY "
                    +" "+SHARD_ID_COLUMN +" ASC," //Null will come last.
                    +" "+PRIORITY_COLUMN +" DESC"                                        
                    +" LIMIT 1000";


    private final static String selectNewest1000Query =
            "SELECT * FROM " + ARCHON_TABLE            
            +" ORDER BY "+CREATED_TIME_COLUMN +" DESC"
            +" LIMIT 1000";

    private final static String selectAllRunningQuery =
            "SELECT * FROM " + ARCHON_TABLE         
            +" WHERE "+ ARC_STATE_COLUMN+"  = 'RUNNING'"
            +" ORDER BY "+CREATED_TIME_COLUMN +" DESC";



    private final static String addArcStatement = 
            "INSERT INTO " +ARCHON_TABLE
            + " ("
            + ARC_FILE_ID_COLUMN + ","
            + FOLDER_COLUMN + ","
            + CREATED_TIME_COLUMN + "," 
            + PRIORITY_COLUMN + ","
            + ARC_STATE_COLUMN  + ","
            + MODIFIED_TIME_COLUMN
            + ") VALUES (?,?,?,?,?,?)"; // #|?|=6

    private final static String setArcStateStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = ? ,"
            + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +ARC_FILE_ID_COLUMN+" = ?";


    private final static String changeArcFolderStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+FOLDER_COLUMN+ " = ? ,"
            + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +ARC_FILE_ID_COLUMN+" = ?";



    private final static String setShardStateStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = ? , " 
            + PRIORITY_COLUMN + " = ? ,"
            + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +SHARD_ID_COLUMN+" = ?";


    private final static String setArcPropertiesStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+SHARD_ID_COLUMN+ " = ? , " 
            + ARC_STATE_COLUMN + " = ? ,"
            + PRIORITY_COLUMN + " = ? ,"
            + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +ARC_FILE_ID_COLUMN+" = ?";


    private final static String setArcPriorityStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+PRIORITY_COLUMN+ " = ? , "             
            + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +ARC_FILE_ID_COLUMN+" = ?";
    

    private final static String resetArcWithPriorityStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+PRIORITY_COLUMN+ " = ? , "             
            + ARC_STATE_COLUMN+ " = 'NEW' ,"
            + SHARD_ID_COLUMN + " = null ,"
            + MODIFIED_TIME_COLUMN + " = ?  "             
            +" WHERE "
            +ARC_FILE_ID_COLUMN+" = ?";
    

    //SELECT DISTINCT (SHARD_ID) from ARCHON WHERE SHARD_ID IS NOT NULL
    private final static String selectShardIDsQuery =
            "SELECT DISTINCT("+SHARD_ID_COLUMN+") FROM " + ARCHON_TABLE +" WHERE "+SHARD_ID_COLUMN +"  IS NOT NULL" ;            


    private final static String getArcsByShardIDQuery =
            "SELECT "+ARC_FILE_ID_COLUMN+" FROM " + ARCHON_TABLE +" WHERE "+SHARD_ID_COLUMN +" = ?" ;            


    private final static String getArcByIDQuery =
            "SELECT * FROM " + ARCHON_TABLE +" WHERE "+ARC_FILE_ID_COLUMN +" = ?" ;            

    private final static String deleteArcByIDQuery =
            "DELETE FROM " + ARCHON_TABLE +" WHERE "+ARC_FILE_ID_COLUMN +" = ?" ;   


    private final static String clearIndexingStatement =  "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = ?  ,"
            + SHARD_ID_COLUMN + " = ? "
            +" WHERE "
            +SHARD_ID_COLUMN+" = ?"
            +" AND "+ARC_STATE_COLUMN+" = ?";

    private final static String resetShardIDStatement =  "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = 'NEW'"
            +" WHERE "
            +SHARD_ID_COLUMN+" = ?";


    public static void initialize(String driverName, String driverUrl, String userName, String password) throws SQLException {
  	    dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setUrl(driverUrl);

        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);

        //enable detection and logging of connection leaks
        dataSource.setRemoveAbandonedOnBorrow(true);
        dataSource.setRemoveAbandonedOnMaintenance(true);
        dataSource.setRemoveAbandonedTimeout(3600); //1 hour
        dataSource.setLogAbandoned(true);

        INITDATE = new Date();
  }



    public ArchonStorage() throws Exception {
        connection = dataSource.getConnection();
    }


    public int nextShardId() throws Exception{
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(selectNextShardIdQuery);
            ResultSet rs = stmt.executeQuery();

            rs.next(); //Select MAX will always return 1 result (maybe NULL) 
            Integer id = rs.getInt(1);                       
            if (id== null || id == 0){
                log.info("No shardID's found in DB, starting with shardID 1.");
                return 1;
            }
            id++;          
            log.info("Returning next unused shardId:"+id);
            return id;                  
        } catch (SQLException e) {
            log.error("SQL Exception in getNextShardId():" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    public void addARC(String arcPath) throws Exception {


        log.info("Persisting new arc-file: " + arcPath);
        String[] splitPath = splitPath(arcPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        boolean aRCIDExist = aRCIDExist(arcId);
        if (aRCIDExist){
            throw new IllegalArgumentException("ArcID already exist:"+arcId);            
        }

        PreparedStatement stmt = null;
        try {					
            stmt = connection.prepareStatement(addArcStatement);


            long now = System.currentTimeMillis();

            stmt.setString(1, arcId);
            stmt.setString(2, folder);
            stmt.setLong(3, now);
            stmt.setInt(4,5);  //priority 5
            stmt.setString(5,"NEW");					
            stmt.setLong(6,now);
            //Shardid is not set. Will be null
            stmt.execute();
 
        } catch (Exception e) {
            log.error("SQL Exception in addFile:" + e.getMessage());								
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    public void addOrUpdateARC(String arcPath) throws Exception {


        log.info("AddOrUpdateARC arc-file: " + arcPath);
        String[] splitPath = splitPath(arcPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        boolean aRCIDExist = aRCIDExist(arcId);

        PreparedStatement stmt = null;
        try {
            if (aRCIDExist){ //update
                log.info("Updating folder for arc-file: " + arcPath);
                stmt =  connection.prepareStatement(changeArcFolderStatement);
                stmt.setString(1, folder);
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, arcId);
                int updated = stmt.executeUpdate();
                if (updated == 0){ //arcfile not found
                    throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
                }                

            }
            else{ //create new
                log.info("Persisting new arc-file: " + arcPath);
                stmt =  connection.prepareStatement(addArcStatement);

                long now = System.currentTimeMillis();

                stmt.setString(1, arcId);
                stmt.setString(2, folder);
                stmt.setLong(3, now);
                stmt.setInt(4,5);  //priority 5
                stmt.setString(5,"NEW");                    
                stmt.setLong(6,now);
                //Shardid is not set. Will be null
                stmt.execute();
            }
 
        } catch (Exception e) {
            log.error("SQL Exception in addOrUpdateARC:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    public List<String> getShardIDs() throws Exception{
        PreparedStatement stmt = null;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            stmt =  connection.prepareStatement(selectShardIDsQuery);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                String shardIdStr= rs.getString(SHARD_ID_COLUMN);                
                ids.add(""+shardIdStr); //TODO toke format, maybe ?                
            }
        } catch (SQLException e) {
            log.error("SQL Exception in getShardIDs():" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
        return ids;
    }


    private ArrayList<ArcVO> getNextArcIdsToCache(String shardID)  throws Exception{  
        PreparedStatement stmt = null;
        int shardIdInt =  Integer.parseInt(shardID);
        ArrayList<ArcVO> list = new  ArrayList<ArcVO>();
        try {
            log.info("Reloading cache for shardID:"+shardID);
            stmt =  connection.prepareStatement(selectNextArcQuery);
            stmt.setInt(1, shardIdInt);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                list.add(createArcVOFromRS(rs));
            }            

            return list;
        }
        catch (SQLException e) {
            log.error("SQL Exception in getNextArcIdsToCache:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }   


    }

    public String nextARC(String shardID) throws Exception{

        PreparedStatement stmt = null;

        try {

            //Update cache if needed
            ArrayList<ArcVO> nextArcIdsCachedForShard = nextArcIdsCached.get(shardID);
            if (nextArcIdsCachedForShard == null || nextArcIdsCachedForShard.size() == 0){ //init if empty
                ArrayList<ArcVO> nextArcIds = getNextArcIdsToCache(shardID);
                nextArcIdsCached.put(shardID, nextArcIds);
                log.info("initialized nextArcIdCache for shardID:"+shardID);
            }


            nextArcIdsCachedForShard = nextArcIdsCached.get(shardID);
            if (nextArcIdsCachedForShard.size() == 0){                        
                log.info("No ARC files with status NEW are ready to process.");                                                
                return "";
            }
            ArcVO nextArc = nextArcIdsCachedForShard.remove(0);
            //Remove it from all caches! (fix of rare race condition bug when indexing concurrent to multiple shards)
            //TODO maybe implement different so logic is simpler
            
            Set<String> keySet = nextArcIdsCached.keySet();
            for (String shardId: keySet){
                 boolean removed = nextArcIdsCached.get(shardId).remove(nextArc);
                  //temp log to see much this happens
                 if (removed){
                   log.info("warc was also removed from cache for shardid:"+shardId);
                 }
                 
            }
            
            
            String fullPath = nextArc.getFileName();
            setARCProperties(nextArc.getFileName(), shardID, ArchonConnector.ARC_STATE.RUNNING, nextArc.getPriority());            
            log.info("Returning next arc:"+fullPath);                                                 
            return fullPath;


        } catch (SQLException e) {
            log.error("SQL Exception in nextARC:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }   


    }


    public List<String> getARCFiles(String shardID) throws Exception{
        PreparedStatement stmt = null;
        ArrayList<String> arcs = new ArrayList<String>();
        int shardIdInt =  Integer.parseInt(shardID);  
        try {
            stmt =  connection.prepareStatement(getArcsByShardIDQuery);
            stmt.setInt(1, shardIdInt);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                String arc= rs.getString(ARC_FILE_ID_COLUMN);
                arcs.add(arc);                              
            }
            return arcs;
        } catch (SQLException e) {
            log.error("SQL Exception in getARCFiles:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }   


    }

    //This is the arc-file-name only
    public ArcVO getArcByID(String arcID) throws Exception{

        PreparedStatement stmt = null;
        try {
            stmt =  connection.prepareStatement(getArcByIDQuery);
            stmt.setString(1, arcID);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()){
                throw new IllegalArgumentException("No arc with id:"+arcID);
            }

            ArcVO arc = createArcVOFromRS(rs);
            return arc; 


        } catch (SQLException e) {
            log.error("SQL Exception in getArcByID:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }



    public int getArcCount() throws Exception{

        PreparedStatement stmt = null;
        try {
            stmt =  connection.prepareStatement(selectCountArcsQuery);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getInt("COUNT"); 


        } catch (SQLException e) {
            log.error("SQL Exception in getArcCount():" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }

    public List<ArcVO> getLatest1000Arcs() throws Exception{
        PreparedStatement stmt = null;

        List<ArcVO> latestArcs = new ArrayList<ArcVO>();
        try {
            stmt =  connection.prepareStatement(selectNewest1000Query);            
            ResultSet rs = stmt.executeQuery();

            while (rs.next()){        
                latestArcs.add(createArcVOFromRS(rs));
            }
            return latestArcs; 

        } catch (SQLException e) {
            log.error("SQL Exception in getLatest1000Arcs:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }



    public List<ArcVO> getAllRunningArcs() throws Exception{
        PreparedStatement stmt = null;

        List<ArcVO> latestArcs = new ArrayList<ArcVO>();
        try {
            stmt =  connection.prepareStatement(selectAllRunningQuery);            
            ResultSet rs = stmt.executeQuery();

            while (rs.next()){        
                latestArcs.add(createArcVOFromRS(rs));
            }
            return latestArcs; 

        } catch (SQLException e) {
            log.error("SQL Exception in getAllRunningArcs:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    public void setARCState(String arcPath, ArchonConnector.ARC_STATE arcState) throws Exception{
        String[] splitPath = splitPath(arcPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        log.info("setARCState: " + arcId +" new state:"+arcState);
        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt =  connection.prepareStatement(setArcStateStatement);

            stmt.setString(1, arcState.toString());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcId);
            int updated = stmt.executeUpdate();
            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }

 
        } catch (Exception e) {
            log.error("SQL Exception in setARCState:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }        
    }


    //change state/priority for all archfiles in the shardID
    public  void setShardState(String shardID, ArchonConnector.ARC_STATE state, int priority) throws Exception{

        PreparedStatement stmt = null;
        try {                   


            stmt =  connection.prepareStatement(setShardStateStatement);

            stmt.setString(1, state.toString());
            stmt.setInt(2, priority);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, shardID);
            int updated = stmt.executeUpdate();

            //System.out.println("setShardState for shardID:"+shardID +" #updated arcfiles ="+updated);
            log.info("setShardState for shardID:"+shardID +" #updated arcfiles ="+updated);
 
        } catch (Exception e) {
            log.error("SQL Exception in setARCState:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }                           
    }
 

    public void setARCProperties(String fullPath, String shardID,ArchonConnector.ARC_STATE state, int priority) throws Exception{

        String[] splitPath = splitPath(fullPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt =  connection.prepareStatement(setArcPropertiesStatement);

            stmt.setInt(1, Integer.parseInt(shardID));
            stmt.setString(2, state.toString());
            stmt.setInt(3, priority);            
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, arcId);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }
 
        } catch (Exception e) {
            log.error("SQL Exception in setARCProperties:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               
    }

     
    public void setARCPriority(String fullPath, int priority) throws Exception{

        String[] splitPath = splitPath(fullPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt =  connection.prepareStatement(setArcPriorityStatement);

            stmt.setInt(1, priority);            
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcId);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }
 
        } catch (Exception e) {
            log.error("SQL Exception in setARCPriority:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               
    }

         
    public  void resetArcWithPriorityStatement(String fullPath, int priority) throws Exception{

        String[] splitPath = splitPath(fullPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt =  connection.prepareStatement(resetArcWithPriorityStatement);

            stmt.setInt(1, priority);            
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcId);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }
 
        } catch (Exception e) {
            log.error("SQL Exception in resetArcWithPriorityStatement:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               
    }

    public  void removeARC(String arcID) throws Exception{
        PreparedStatement stmt = null;
        try {                   

            boolean aRCIDExist = aRCIDExist(arcID);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcID);            
            }

            stmt =  connection.prepareStatement(deleteArcByIDQuery);

            stmt.setString(1, arcID);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcID);
            }
             
        } catch (Exception e) {
            log.error("SQL Exception in removeARC:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               


    }

    public void clearIndexing(String shardID) throws Exception{
        PreparedStatement stmt = null;
        try {                               
            stmt =  connection.prepareStatement(clearIndexingStatement);

            stmt.setString(1, "NEW");
            stmt.setNull(2,Types.BIGINT);
            stmt.setInt(3, Integer.parseInt(shardID));
            stmt.setString(4, "RUNNING");

            int updated = stmt.executeUpdate();
            log.info("Cleared indexing for shardId:"+shardID +" Number of arcfiles changed status:"+updated);            
        } catch (Exception e) {
            log.error("SQL Exception in clearIndexing:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }                       

    }

    public void resetShardId(String shardID) throws Exception{
        PreparedStatement stmt = null;
        try {                               
            stmt =  connection.prepareStatement(resetShardIDStatement);
            stmt.setInt(1, Integer.parseInt(shardID));
            int updated = stmt.executeUpdate();
            log.info("resetShardId:"+shardID +" Number of arcfiles changed status:"+updated);                       
        } catch (Exception e) {
            log.error("SQL Exception in resetShardId:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }                       
    }
    
    //This is the filename only, not the full path
    private boolean aRCIDExist(String arcID) throws Exception{

        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(getArcByIDQuery);
            stmt.setString(1, arcID);
            ResultSet rs = stmt.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            log.error("SQL Exception in aRCIDExist:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        } 

    }


    private static ArcVO createArcVOFromRS(ResultSet rs) throws SQLException {
        ArcVO arc = new ArcVO();

        arc.setFileId(rs.getString(ARC_FILE_ID_COLUMN));
        arc.setFolderName(rs.getString(FOLDER_COLUMN));        
        arc.setFileName(rs.getString(FOLDER_COLUMN)+rs.getString(ARC_FILE_ID_COLUMN));
        arc.setCreatedTime(rs.getLong(CREATED_TIME_COLUMN));            
        arc.setArcState(rs.getString(ARC_STATE_COLUMN)); 
        arc.setPriority(rs.getInt(PRIORITY_COLUMN));            
        arc.setModifiedTime(rs.getLong(MODIFIED_TIME_COLUMN));

        //Can be NULL
        String shardIdStr= rs.getString(SHARD_ID_COLUMN);            
        if (shardIdStr != null){
            arc.setShardId(new Integer(shardIdStr));                
        }                                                                   
        return arc;
    }

    protected void clearCachedArcs(){
        nextArcIdsCached= new HashMap<String,ArrayList<ArcVO>>();
    }

    

    public void commit() throws Exception {
        connection.commit();
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            //nothing to do here
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            //nothing to do here
        }
    }

    private void closeStatement(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            log.error("Failed to close statement");
            // ignore..
        }
    }


    // This is called by from InialialziationContextListener by the Web-container when server is shutdown,
    // Just to be sure the DB lock file is free.
    public static void shutdown() {
        log.info("Shutdown ArchonStorage");        
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }

    // /test1/test2/arcfile.arc ville be split in "/test1/test2/" and "arcfile.arc"
    private String[] splitPath(String fullFilePath){

        String[] split = new String[2];                        
        int lastSlash=fullFilePath.lastIndexOf("/");            
        split[0]=fullFilePath.substring(0,lastSlash+1);
        split[1]=fullFilePath.substring(lastSlash+1,fullFilePath.length());            
        return split;                            
    }

}
