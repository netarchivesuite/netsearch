package dk.statsbiblioteket.netarchivesuite.archon.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Date;
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

    private static final String FILENAME_COLUMN = "FILENAME";
    private static final String CREATED_TIME_COLUMN = "CREATED_TIME";
    private static final String PRIORITY_COLUMN = "PRIORITY";
    private static final String ARC_STATE_COLUMN = "ARC_STATE";
    private static final String SHARD_ID_COLUMN = "SHARD_ID";
    private static final String MODIFIED_TIME_COLUMN = "MODIFIED_TIME";

    //private static final String ID_COLUMN = "ID"; // ID used for all tables

    private final static String selectNextShardIdQuery = 
            " SELECT MAX("+SHARD_ID_COLUMN+") FROM " + ARCHON_TABLE;

    //select FILENAME from ARCHON where status ='NEW' AND ( SHARD_ID = 10 OR SHARD_ID is null) order by shard_ID desc, priority desc, filename asc limit  1
    private final static String selectNextArcQuery =
            "SELECT "+FILENAME_COLUMN +" FROM " + ARCHON_TABLE            
            +" WHERE "+ ARC_STATE_COLUMN+"  = 'NEW'"
            +"  AND ("+SHARD_ID_COLUMN+" = ?  OR "+ SHARD_ID_COLUMN +" IS NULL )"
            +" ORDER BY "
            +" "+SHARD_ID_COLUMN +" DESC,"
            +" "+PRIORITY_COLUMN +" DESC,"
            +" "+FILENAME_COLUMN +" ASC"
            +" LIMIT 1";


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
            + FILENAME_COLUMN + ","
            + CREATED_TIME_COLUMN + "," 
            + PRIORITY_COLUMN + ","
            + ARC_STATE_COLUMN  + ","
            + MODIFIED_TIME_COLUMN  + ","
            + ") VALUES (?,?,?,?,?)"; // #|?|=5

    private final static String setArcStateStatement = "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = ? ,"
             + MODIFIED_TIME_COLUMN + " = ? " 
            +" WHERE "
            +FILENAME_COLUMN+" = ?";


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
            +FILENAME_COLUMN+" = ?";

    //SELECT DISTINCT (SHARD_ID) from ARCHON WHERE SHARD_ID IS NOT NULL
    private final static String selectShardIDsQuery =
            "SELECT DISTINCT("+SHARD_ID_COLUMN+") FROM " + ARCHON_TABLE +" WHERE "+SHARD_ID_COLUMN +"  IS NOT NULL" ;            


    private final static String getArcsByShardIDQuery =
            "SELECT "+FILENAME_COLUMN+" FROM " + ARCHON_TABLE +" WHERE "+SHARD_ID_COLUMN +" = ?" ;            


    private final static String getArcByIDQuery =
            "SELECT * FROM " + ARCHON_TABLE +" WHERE "+FILENAME_COLUMN +" = ?" ;            

    private final static String deleteArcByIDQuery =
            "DELETE FROM " + ARCHON_TABLE +" WHERE "+FILENAME_COLUMN +" = ?" ;   


    private final static String clearIndexingStatement =  "UPDATE "+ ARCHON_TABLE 
            +" SET "+ARC_STATE_COLUMN+ " = ?  " 
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


    
    //TODO verify shardID is not already in DB
    //synchronized since we are writing.
    public synchronized void addARC(String arcID) throws Exception {

        /*
			    if (priority<1){
			        throw new IllegalArgumentException("Priority must be greater than 1, priority="+priority);
			    }			    
         */

        log.info("Persisting new arc-file: " + arcID);

        /*
				if (!validateAttributesValues) {
					throw new IllegalArgumentException("Validation error. Attributes or values must not be empty");
				}
         */
        PreparedStatement stmt = null;
        try {					
            stmt = singleDBConnection.prepareStatement(addArcStatement);

            long now = System.currentTimeMillis();
            
            stmt.setString(1, arcID);
            stmt.setLong(2, now);
            stmt.setInt(3,1);  //priority 1
            stmt.setString(4,"NEW");					
            stmt.setLong(5,now);
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

    public String nextARC(String shardID) throws Exception{
        PreparedStatement stmt = null;
        int shardIdInt =  Integer.parseInt(shardID);

        try {
            stmt = singleDBConnection.prepareStatement(selectNextArcQuery);
            stmt.setInt(1, shardIdInt);
            ResultSet rs = stmt.executeQuery();


            if (!rs.next()){                        
                log.info("No ARC files with status NEW are ready to process.");                                                
                return "";
            }

            String fileName = rs.getString(1);                       

            log.info("Returning next shardId:"+fileName);                                                 
            return fileName;


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
                String arc= rs.getString(FILENAME_COLUMN);
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


    public ArcVO getArcByID(String arcID) throws Exception{
        PreparedStatement stmt = null;
        try {
            stmt = singleDBConnection.prepareStatement(getArcByIDQuery);
            stmt.setString(1, arcID);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()){
                throw new IllegalArgumentException("No arc with id:"+arcID);
            }

            ArcVO arc = new ArcVO();
            arc.setFileName(rs.getString(FILENAME_COLUMN));
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


        } catch (SQLException e) {
            log.error("SQL Exception in getArcByID:" + e.getMessage());
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
                ArcVO arc = new ArcVO();
                arc.setFileName(rs.getString(FILENAME_COLUMN));
                arc.setCreatedTime(rs.getLong(CREATED_TIME_COLUMN));            
                arc.setArcState(rs.getString(ARC_STATE_COLUMN)); 
                arc.setPriority(rs.getInt(PRIORITY_COLUMN));            
                arc.setModifiedTime(rs.getLong(MODIFIED_TIME_COLUMN));
                
                //Can be NULL
                String shardIdStr= rs.getString(SHARD_ID_COLUMN);            
                if (shardIdStr != null){
                    arc.setShardId(new Integer(shardIdStr));                
                }                                                                   
                latestArcs.add(arc);
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
                ArcVO arc = new ArcVO();
                arc.setFileName(rs.getString(FILENAME_COLUMN));
                arc.setCreatedTime(rs.getLong(CREATED_TIME_COLUMN));            
                arc.setArcState(rs.getString(ARC_STATE_COLUMN)); 
                arc.setPriority(rs.getInt(PRIORITY_COLUMN));            
                arc.setModifiedTime(rs.getLong(MODIFIED_TIME_COLUMN));
                
                //Can be NULL
                String shardIdStr= rs.getString(SHARD_ID_COLUMN);            
                if (shardIdStr != null){
                    arc.setShardId(new Integer(shardIdStr));                
                }                                                                   
                latestArcs.add(arc);
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
    public synchronized void setARCState(String arcID, ArchonConnector.ARC_STATE arcState) throws Exception{
        PreparedStatement stmt = null;
        try {                   
            stmt = singleDBConnection.prepareStatement(setArcStateStatement);

            stmt.setString(1, arcState.toString());
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setString(3, arcID);
            int updated = stmt.executeUpdate();
            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcID);
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
    public synchronized void setARCProperties(String arcID, String shardID,ArchonConnector.ARC_STATE state, int priority) throws Exception{
        PreparedStatement stmt = null;
        try {                   
            stmt = singleDBConnection.prepareStatement(setArcPropertiesStatement);

            stmt.setInt(1, Integer.parseInt(shardID));
            stmt.setString(2, state.toString());
            stmt.setInt(3, priority);            
            stmt.setLong(4, System.currentTimeMillis());
            stmt.setString(5, arcID);
            int updated = stmt.executeUpdate();

            if (updated == 0){ //arcfile not found
                throw new IllegalArgumentException("Arcfile not found with id:"+arcID);
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
    public synchronized void removeARC(String arcID) throws Exception{
        PreparedStatement stmt = null;
        try {                   
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
            stmt.setInt(2, Integer.parseInt(shardID));
            stmt.setString(3, "RUNNING");

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
    

}
