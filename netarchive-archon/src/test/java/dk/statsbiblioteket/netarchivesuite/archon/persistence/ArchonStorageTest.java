package dk.statsbiblioteket.netarchivesuite.archon.persistence;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector.ARC_STATE;

/*
 * Unittest class for the H2Storage.
 * The unittest uses the H2 driver so no running database server is required
 * 
 * All tests creates and use H2 database in the directory: target/h2
 *  
 * Currently the directory is not deleted after the tests have run. This is useful as you can
 * open and checke the database and see what the unit-tests did.
 */

public class ArchonStorageTest {

    private static final Logger log = LoggerFactory.getLogger(ArchonStorageTest.class);
  
    private static final String CREATE_TABLES_DDL_FILE = "H2_DDL_scripts/archon_create_db.ddl";
    
    private static final String DRIVER = "org.h2.Driver";
    private static final String URL = "jdbc:h2:target/test-classes/h2/archon";
    private static final String USERNAME = "";
    private static final String PASSWORD = "";
    
    private static ArchonStorage storage = null;

    private static final String arcFile1 ="folder1/folder2/arcfile1";
    private static final String arcFile2 ="folder3/arcfile2";

    private static void createEmptyDBFromDDL() throws Exception {
        // Delete if exists
        doDelete(new File("target/test-classes/h2"));
        Connection connection = null;

        try {
            Class.forName(DRIVER); // load the driver
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
        connection = DriverManager.getConnection(URL, "", "");

        File file = getFile(CREATE_TABLES_DDL_FILE);
        log.info("Running DDL script:" + file.getAbsolutePath());

        if (!file.exists()) {
            log.error("DDL script not found:" + file.getAbsolutePath());
            throw new RuntimeException("DDLscript file not found:" + file.getAbsolutePath());
        }

        String scriptStatement = "RUNSCRIPT FROM '" + file.getAbsolutePath() + "'";

        connection.prepareStatement(scriptStatement).execute();

        PreparedStatement shutdown = connection.prepareStatement("SHUTDOWN");
        shutdown.execute();
        connection.close();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        createEmptyDBFromDDL();
        ArchonStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);

    }

    @AfterClass
    public static void afterClass() throws Exception {
        // No reason to delete DB data after test, since we delete it before each test.
        // This way you can open the DB in a DB-browser after a unittest and see the result.
        ArchonStorage.shutdown();
    }

    /*
     * Delete data from all tables before each unittest
     */

    @Before
    public void before() throws Exception {
        storage = new ArchonStorage();
    }

    @After
    public void after() throws Exception {
        storage.rollback();
        storage.close();
    }

    @Test
    public void testCreateEmptyDB() throws Exception {
    
    }


    @Test
    public void testNextShardID() throws Exception {

        //Empty DB, first shardId must be 1.
        int nextShardId = storage.nextShardId();
        assertEquals(1,nextShardId);

        storage.addARC(arcFile1);          

        // Has not been assigned shardId yet
        assertEquals(1,nextShardId);

        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 9);         
        nextShardId = storage.nextShardId(); 
        assertEquals(2,nextShardId);


    }

    
    @Test
    public void testNextARC() throws Exception {
        String nextArc = storage.nextARC("10");
        assertEquals("",nextArc); //behavior if none is left
        storage.addARC(arcFile1);

        nextArc = storage.nextARC("10");
        assertEquals(arcFile1,nextArc); //have shardid = null as is returned
        storage.addARC(arcFile2);
        storage.setARCProperties(arcFile2, "10",ArchonConnector.ARC_STATE.NEW, 1); //set shardid 10 and still NEW

        //Test shard sorting
        nextArc = storage.nextARC("10");
        assertEquals(arcFile2,nextArc); // must return this over the one with no shardID

        nextArc = storage.nextARC("10");
        assertEquals("",nextArc); //behavior if none is left

        //Test priority sorting
        String arcFile3Shard10Priority3= "arcfile3";
        String arcFile4Shard10Priority2= "arcfile4";      
        storage.addARC(arcFile3Shard10Priority3);
        storage.addARC(arcFile4Shard10Priority2);
        storage.setARCProperties(arcFile3Shard10Priority3, "10", ArchonConnector.ARC_STATE.NEW, 3);
        storage.setARCProperties(arcFile4Shard10Priority2, "10", ArchonConnector.ARC_STATE.NEW, 2);

        nextArc = storage.nextARC("10");
        assertEquals(arcFile3Shard10Priority3,nextArc); // must be the one with highest priority

        storage.clearCachedArcs();
        
        //Test Lexicographical sorting
        //add another with priority 3, but name aaarcfile5 and this must be returned    
        String arcFile5Shard10Priority3= "aaarcfile5";      
        storage.addARC(arcFile5Shard10Priority3);
        storage.setARCProperties(arcFile5Shard10Priority3, "10", ArchonConnector.ARC_STATE.NEW, 3);
        nextArc = storage.nextARC("10"); 
        assertEquals(arcFile5Shard10Priority3,nextArc);


        nextArc = storage.nextARC("12");
        assertEquals("",nextArc); //behavior if none is left

    }
    
    //This test is due to a rare race condition bug that only occured when running concurrent shardid builders.
    //The same arc (with no shard) could be returned to both builders and would end up in both shards
    @Test
    public void testMultipleConcurrentShards() throws Exception {
        //Make sure arcs with no shardid are not returned twice to different shards due to the caching.
        
        Random r = new Random();
        //Construct data for test (all have random priorities)
        //250 arcs with shardid 1 
        //350 arcs with shardid 2
        //1111 arcs with no shard id 
        
        String shard1Path = "folder1/";
        String shard2Path = "folder2/"; 
        String noShardPath = "folder3/";
        
        for (int i =1;i<=250;i++){
            String arc = shard1Path+i+"_1.arc";
            storage.addARC( arc);  
            storage.setARCProperties(arc,"1",ARC_STATE.NEW,r.nextInt(10)+1);
        }
        
        for (int i =1;i<=350;i++){
            String arc = shard2Path+i+"_2.arc";
            storage.addARC( arc);  
            storage.setARCProperties(arc,"2",ARC_STATE.NEW,r.nextInt(10)+1);
        }
        
        for (int i =1;i<=1111;i++){
            String arc = noShardPath+i+"_none.arc";
            storage.addARC( arc);  
            storage.setARCPriority(arc,r.nextInt(10)+1);
        }
        
        //and now we are ready to test concurrency and want to make sure the same no-shard arc id is not returned
        //to both a shard1 and shard2 build process.
        HashSet<String> returnedArcFiles = new HashSet<String>(); 
 
        String currentArc= null;
        while (!"".equals(currentArc)){ //The model will return "" when no arcs are left
            int shardId =r.nextInt(2)+1; // Random index into  shard1 or shard2             
            if (shardId == 1){
                currentArc = storage.nextARC("1");  //The arc will now be in the running state
            } else  {
                currentArc = storage.nextARC("2");  //The arc will now be in the running state            
            }
              
            if (returnedArcFiles.contains(currentArc)){
                System.out.println("Same arc has already been returned:"+currentArc);
                fail("Same arc has already been returned:"+currentArc);                         
            }
            
            returnedArcFiles.add(currentArc);
            //System.out.println("adding arc:"+currentArc);
          
            
        }
        //When removing arc-file, remove from ALL shardis or have shardid only with non-shardi
        assertEquals(250+350+1111, returnedArcFiles.size()-1); //extract 1 as the last is the "" arc when none are left
    }

    

    @Test
    public void testGetShardIDs() throws Exception {

        List<String> ids = storage.getShardIDs();
        assertEquals(0, ids.size());   //empty DB

        storage.addARC(arcFile1);

        ids = storage.getShardIDs();
        assertEquals(0, ids.size());   //not empty DB, but this arc has not been assigned ID yet.

        storage.setARCProperties(arcFile1, "1",ArchonConnector.ARC_STATE.NEW, 1);
        ids = storage.getShardIDs();
        assertEquals(1, ids.size());   //not empty DB, but this arc has not been assigned ID yet.
        assertEquals("1", ids.get(0));

        storage.addARC(arcFile2);
        assertEquals(1, ids.size());  //Still only 1 not null shardID
        storage.setARCProperties(arcFile2, "2", ArchonConnector.ARC_STATE.NEW, 1);
        ids = storage.getShardIDs();
        assertEquals(2, ids.size());  //now 2 ids (1 and 2)                   
    }

    @Test
    public void testGetARCFiles() throws Exception {

        List<String> ids = storage.getARCFiles("10");      
        assertEquals(0, ids.size()); 
        storage.addARC("arcfile1");
        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.NEW, 1); //shardid 1

        ids = storage.getARCFiles("2"); //None      
        assertEquals(0, ids.size());

        ids = storage.getARCFiles("1");      
        assertEquals(1, ids.size());

        //add one with shardid2 
        storage.addARC("arcfile2");
        storage.setARCProperties(arcFile2, "2", ArchonConnector.ARC_STATE.NEW, 1); //shardid 2

        //Still only 1 with shardid 1
        ids = storage.getARCFiles("1");      
        assertEquals(1, ids.size());      

    }

    

    @Test
    public void testCreateDBAndDoNothing() throws Exception {
        //Run this test to create the DB      
          
    }
        

    @Test
    public void testAddArc() throws Exception {

        ArcVO arc = null;
        try{
        arc = storage.getArcByID(arcFile1);
        fail();
        }
        catch (Exception e){
            //Expected
        }
        
        storage.addARC(arcFile1);

        arc = storage.getArcByID("arcfile1");
        assertEquals(arcFile1, arc.getFileName());
        assertEquals("NEW", arc.getArcState());   
        assertEquals(5, arc.getPriority());
        assertEquals(null, arc.getShardId());

        
          int arcCount = storage.getArcCount();
          assertEquals(1,arcCount);
         
        
    }

    @Test
    public void testSetARCState() throws Exception {

    	Date d = new Date(1477303834757L);
    	
        try{
            storage.setARCState(arcFile1,ArchonConnector.ARC_STATE.RUNNING);// does not exist
            fail();
        }
        catch(Exception e){
            //Expected            
        }
        storage.addARC(arcFile1);
        storage.setARCState(arcFile1,ArchonConnector.ARC_STATE.RUNNING);        
        //Check status has been set to running.
        ArcVO arc = storage.getArcByID("arcfile1"); 
        assertEquals("RUNNING",arc.getArcState());                
    }


    @Test
    public void testSetShardState() throws Exception {

        String fileName ="arcfile1";

        storage.setShardState("1", ArchonConnector.ARC_STATE.RUNNING, 2); //shard ID does not exist, but this is OK        
        storage.addARC(fileName);

        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 1); //shardid 1

        ArcVO arc = storage.getArcByID(fileName); 
        assertEquals("RUNNING",arc.getArcState());                
    }

    @Test
    public void testSetARCProperties() throws Exception {    
        try{
            storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 9);
            fail();
        }
        catch(Exception e){
            //Expected            
        }
        storage.addARC(arcFile1);          
        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 9); //This should succeed

        ArcVO arc = storage.getArcByID("arcfile1"); 
        assertEquals("RUNNING",arc.getArcState());    
        assertEquals(9,arc.getPriority());
    }

    @Test
    public void testSetARCPriority() throws Exception {          
        storage.addARC(arcFile1);          
        storage.setARCPriority(arcFile1, 7); //This should succeed
        ArcVO arc = storage.getArcByID("arcfile1");            
        assertEquals(7,arc.getPriority());
    }

    

    @Test
    public void testResetArcWithPriorty() throws Exception {          
        storage.addARC(arcFile1);       
        storage.setARCProperties(arcFile1, "51", ArchonConnector.ARC_STATE.COMPLETED, 3);        
              
        //Now reset it
        storage.resetArcWithPriorityStatement(arcFile1,6);
        
        ArcVO arc = storage.getArcByID("arcfile1");                    
        assertEquals(6,arc.getPriority());        
        assertEquals(ArchonConnector.ARC_STATE.NEW.name(),arc.getArcState());
        assertNull(arc.getShardId());       
    }

    
    
    @Test
    public void testClearIndexing() throws Exception {    
        
        storage.addARC(arcFile1);          
        storage.addARC(arcFile2);
        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 9);  
        storage.setARCProperties(arcFile2, "1", ArchonConnector.ARC_STATE.RUNNING, 9);
        //Now clearIndexing and check status
        storage.clearIndexing("1");
        
        //check status is running.
        ArcVO arc1 = storage.getArcByID("arcfile1");   
        ArcVO arc2 = storage.getArcByID("arcfile2");
        assertEquals("NEW",arc1.getArcState());
        assertEquals("NEW",arc2.getArcState());                       
        
        assertEquals(null,arc1.getShardId());
    }
    
        

    @Test
    public void testResetShardIDs() throws Exception {    
        
        storage.addARC(arcFile1);          
        storage.addARC(arcFile2);
        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.COMPLETED, 9);  
        storage.setARCProperties(arcFile2, "1", ArchonConnector.ARC_STATE.RUNNING, 9);
        //Now clearIndexing and check status
        storage.resetShardId("1");
        
        //check status is running.
        ArcVO arc1 = storage.getArcByID("arcfile1");   
        ArcVO arc2 = storage.getArcByID("arcfile2");
        assertEquals("NEW",arc1.getArcState());
        assertEquals("NEW",arc2.getArcState());                       
        
        assertEquals(new Integer(1),arc1.getShardId());
    }
    

    @Test
    public void testRemoveARC() throws Exception {    

        try{
            storage.removeARC(arcFile1);
            fail();
        }
        catch(Exception e){
            //Expected            
        }
        storage.addARC(arcFile1);          
        ArcVO arc = storage.getArcByID("arcfile1"); 
        assertEquals(arcFile1, arc.getFileName());
        
        //now remove it
        storage.removeARC("arcfile1");
        
        try{
        arc = storage.getArcByID("arcfile1");
         fail();
        }
        catch(Exception e){
            //Expected
        }        
    }
        
    @Test
    public void testGetLatest1000Arcs() throws Exception {
         List<ArcVO> latest1000Arcs = storage.getLatest1000Arcs();
         assertEquals(0, latest1000Arcs.size());
        
        storage.addARC(arcFile1);
        latest1000Arcs = storage.getLatest1000Arcs();
        assertEquals(1, latest1000Arcs.size());       
    }
        
    
    @Test
    public void testGetAllRunningArcs() throws Exception {
         List<ArcVO> allRunningArcs = storage.getAllRunningArcs();
         assertEquals(0, allRunningArcs.size());
        
        storage.addARC(arcFile1);
        storage.addARC(arcFile2); //This should not be found
        storage.setARCProperties(arcFile1, "1", ArchonConnector.ARC_STATE.RUNNING, 9);
        allRunningArcs = storage.getAllRunningArcs();
        assertEquals(1, allRunningArcs.size());       
    }
    
    
    @Test
    public void testAddOrUpdateArc() throws Exception {
        storage.addOrUpdateARC(arcFile1);
        ArcVO arc = storage.getArcByID("arcfile1");
        assertEquals(arcFile1, arc.getFileName());
        
        storage.addOrUpdateARC("folder3/folder4/arcfile1");
        arc = storage.getArcByID("arcfile1");
        assertEquals("folder3/folder4/arcfile1", arc.getFileName());
    }
    
    
    // file.delete does not work for a directory unless it is empty. hence this method
    private static void doDelete(File path) throws IOException {
        if (path.isDirectory()) {
            for (File child : path.listFiles()) {
                doDelete(child);
            }
        }
        if (!path.delete()) {
            log.info("Could not delete " + path);
        }
    }


    /**
     * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
     *
     * @param resource
     *            a generic resource.
     * @return a File pointing to the resource.
     */
    private static File getFile(String resource) throws IOException {
        File directFile = new File(resource);
        if (directFile.exists()) {
            return directFile;
        }
        URL classLoader = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (classLoader == null) {
            throw new FileNotFoundException("Unable to locate '" + resource + "' as direct File or on classpath");
        }
        String fromURL = classLoader.getFile();
        if (fromURL == null || fromURL.isEmpty()) {
            throw new FileNotFoundException("Unable to convert URL '" + fromURL + "' to File");
        }
        return new File(fromURL);
    }
}

