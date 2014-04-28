package dk.statsbiblioteket.netarchivesuite.archon.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;


/**
 * This class handles all communication with the DB. Singleton pattern. From unit-test the test-class initializes the singleton - on Tomcat it is initialized by
 * InitializationContextListener.
 * 
 * For performance the DB can be accessed through the LicenseCache-class that cache the complete DB-tables. The cache has a 15 minute reload timer, but can also
 * instant reload when implicit notified by the DB-methods in this class.
 * 
 * The DB consist of the following tables:
 * 
 * TODO
 * 
 */
public class H2Storage {

    private static final Logger log = LoggerFactory.getLogger(H2Storage.class);
    private static H2Storage instance = null;

    private static Connection singleDBConnection = null;

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;
    public static Date LASTDATABASEBACKUPDATE = null;
    public static int NUMBER_DATABASE_BACKUP_SINCE_STARTUP = 0;

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
                    +" "+SHARD_ID_COLUMN +" DESC,"
                    +" "+PRIORITY_COLUMN +" DESC"                                        
                    +" LIMIT 10";


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
            + MODIFIED_TIME_COLUMN  + ","
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



    public H2Storage(String dbFilePath) throws SQLException {
        log.info("Intialized H2Storage, dbFile=" + dbFilePath);
        synchronized (H2Storage.class) {
            initializeDBConnection(dbFilePath);
        }
    }

    public static H2Storage getInstance() {
        if (instance == null) {
            throw new IllegalArgumentException("H2 Storage has not been initialized yet");
        }

        return instance;
    }



    public int nextShardId() throws Exception{
        PreparedStatement stmt = null;
        try {
            stmt = singleDBConnection.prepareStatement(selectNextShardIdQuery);
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


    //synchronized since we are writing.
    public synchronized void addARC(String arcPath) throws Exception {


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
            stmt = singleDBConnection.prepareStatement(addArcStatement);


            long now = System.currentTimeMillis();

            stmt.setString(1, arcId);
            stmt.setString(2, folder);
            stmt.setLong(3, now);
            stmt.setInt(4,5);  //priority 5
            stmt.setString(5,"NEW");					
            stmt.setLong(6,now);
            //Shardid is not set. Will be null
            stmt.execute();

            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in addFile:" + e.getMessage());								
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    //synchronized since we are writing.
    public synchronized void addOrUpdateARC(String arcPath) throws Exception {


        log.info("AddOrUpdateARC arc-file: " + arcPath);
        String[] splitPath = splitPath(arcPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        boolean aRCIDExist = aRCIDExist(arcId);

        PreparedStatement stmt = null;
        try {
            if (aRCIDExist){ //update
                log.info("Updating folder for arc-file: " + arcPath);
                stmt = singleDBConnection.prepareStatement(changeArcFolderStatement);
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
                stmt = singleDBConnection.prepareStatement(addArcStatement);

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
            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in addFile:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }
    }


    public List<String> getShardIDs() throws Exception{
        PreparedStatement stmt = null;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            stmt = singleDBConnection.prepareStatement(selectShardIDsQuery);
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
            stmt = singleDBConnection.prepareStatement(selectNextArcQuery);
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
    
    public synchronized String nextARC(String shardID) throws Exception{
        
        PreparedStatement stmt = null;

        try {
                        
            //Update cache is empty
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
            stmt = singleDBConnection.prepareStatement(getArcsByShardIDQuery);
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
            stmt = singleDBConnection.prepareStatement(getArcByIDQuery);
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
            stmt = singleDBConnection.prepareStatement(selectCountArcsQuery);

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
            stmt = singleDBConnection.prepareStatement(selectNewest1000Query);            
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
            stmt = singleDBConnection.prepareStatement(selectAllRunningQuery);            
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




    //synchronized since we are writing.
    public synchronized void setARCState(String arcPath, ArchonConnector.ARC_STATE arcState) throws Exception{
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

            stmt = singleDBConnection.prepareStatement(setArcStateStatement);

            stmt.setString(1, arcState.toString());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcId);
            int updated = stmt.executeUpdate();
            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }

            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in setARCState:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }        
    }




    //TODO vil gerne returnere antallet af ændrede filer
    //change state/priority for all archfiles in the shardID
    //synchronized since we are writing.
    public synchronized void setShardState(String shardID, ArchonConnector.ARC_STATE state, int priority) throws Exception{

        PreparedStatement stmt = null;
        try {                   


            stmt = singleDBConnection.prepareStatement(setShardStateStatement);

            stmt.setString(1, state.toString());
            stmt.setInt(2, priority);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.setString(4, shardID);
            int updated = stmt.executeUpdate();

            //System.out.println("setShardState for shardID:"+shardID +" #updated arcfiles ="+updated);
            log.info("setShardState for shardID:"+shardID +" #updated arcfiles ="+updated);


            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in setARCState:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }                           
    }

    //synchronized since we are writing. 
    //TODO vil gerne returnere antallet af ændrede filer
    //Supposed to change all data for the given arcID
    public synchronized void setARCProperties(String fullPath, String shardID,ArchonConnector.ARC_STATE state, int priority) throws Exception{

        String[] splitPath = splitPath(fullPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt = singleDBConnection.prepareStatement(setArcPropertiesStatement);

            stmt.setInt(1, Integer.parseInt(shardID));
            stmt.setString(2, state.toString());
            stmt.setInt(3, priority);            
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, arcId);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }

            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in setARCProperties:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               
    }

    
    //synchronized since we are writing.     
    public synchronized void setARCPriority(String fullPath, int priority) throws Exception{

        String[] splitPath = splitPath(fullPath); 
        String folder = splitPath[0];
        String arcId = splitPath[1];

        PreparedStatement stmt = null;
        try {                   
            boolean aRCIDExist = aRCIDExist(arcId);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcId);            
            }

            stmt = singleDBConnection.prepareStatement(setArcPriorityStatement);

            stmt.setInt(1, priority);            
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcId);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcId);
            }

            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in setARCPriority:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               
    }
    
    //synchronized since we are writing.
    public synchronized void removeARC(String arcID) throws Exception{
        PreparedStatement stmt = null;
        try {                   

            boolean aRCIDExist = aRCIDExist(arcID);
            if (!aRCIDExist){
                throw new IllegalArgumentException("ArcID does not exist:"+arcID);            
            }

            stmt = singleDBConnection.prepareStatement(deleteArcByIDQuery);

            stmt.setString(1, arcID);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcID);
            }

            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in removeARC:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }               


    }

    //synchronized since we are writing.
    public synchronized void clearIndexing(String shardID) throws Exception{
        PreparedStatement stmt = null;
        try {                               
            stmt = singleDBConnection.prepareStatement(clearIndexingStatement);

            stmt.setString(1, "NEW");
            stmt.setString(2, null);
            stmt.setInt(3, Integer.parseInt(shardID));
            stmt.setString(4, "RUNNING");

            int updated = stmt.executeUpdate();
            log.info("Cleared indexing for shardId:"+shardID +" Number of arcfiles changed status:"+updated);           
            singleDBConnection.commit(); 
        } catch (Exception e) {
            log.error("SQL Exception in clearIndexing:" + e.getMessage());                                
            throw e;
        } finally {
            closeStatement(stmt);
        }                       

    }

    //This is the filename only, not the full path
    private boolean aRCIDExist(String arcID) throws Exception{

        PreparedStatement stmt = null;
        try {
            stmt = singleDBConnection.prepareStatement(getArcByIDQuery);
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
    
    /*
     * Will create a zip-file with a backup of the database.
     * 
     * @param filePathtoBackup Full path+ filename of the backup file (including .zip)
     * 
     * It will take about 5 seconds for a database with 250K rows
     */
    public synchronized void backupDatabase(String fileName) throws SQLException {
        log.info("Creating database backup :" + fileName);
        PreparedStatement stmt = null;
        try {
            stmt = singleDBConnection.prepareStatement("BACKUP TO '" + fileName + "'");
            stmt.execute();
            log.info("Database backup success");
        } catch (SQLException e) {
            log.error("SQL Exception in databasebackup:" + e.getMessage());
            throw e;
        } finally {
            closeStatement(stmt);
        }
        LASTDATABASEBACKUPDATE = new Date();
        NUMBER_DATABASE_BACKUP_SINCE_STARTUP++;
    }




    private synchronized void initializeDBConnection(String dbFilePath) throws SQLException {
        log.info("initializeDBConnection. DB path:" + dbFilePath);
        if (singleDBConnection != null) {
            log.error("DB allready initialized and locked:" + dbFilePath);
            throw new RuntimeException("DB allready initialized and locked:" + dbFilePath);
        }

        try {
            Class.forName("org.h2.Driver"); // load the driver
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        String DB_URL = "jdbc:h2:" + dbFilePath;
        singleDBConnection = DriverManager.getConnection(DB_URL, "", "");
        singleDBConnection.setAutoCommit(false);

        instance = this;
        // If ever performance is an issue, TRANSACTION_READ_UNCOMMITTED can be enabled. But
        // I prefer not to have this setting. Since we only have 1 connection at a time,
        // it should be possible to enable this setting without breaking anything.
        // singleDBConnection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

        INITDATE = new Date();

        // In-comment to enable profiler (small performance overhead)
        // profiler = new Profiler();
        // profiler.startCollecting();

    }


    // Used from unittests. Create tables DDL etc.
    protected synchronized void runDDLScript(File file) throws SQLException {
        log.info("Running DDL script:" + file.getAbsolutePath());

        if (!file.exists()) {
            log.error("DDL script not found:" + file.getAbsolutePath());
            throw new RuntimeException("DDLscript file not found:" + file.getAbsolutePath());
        }

        String scriptStatement = "RUNSCRIPT FROM '" + file.getAbsolutePath() + "'";

        singleDBConnection.prepareStatement(scriptStatement).execute();
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
        log.info("Shutdown H2Storage");
        try {
            if (singleDBConnection != null) {
                PreparedStatement shutdown = singleDBConnection.prepareStatement("SHUTDOWN");
                shutdown.execute();
                if (singleDBConnection != null) {
                    singleDBConnection.close();
                }
                Thread.sleep(3000L);
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
