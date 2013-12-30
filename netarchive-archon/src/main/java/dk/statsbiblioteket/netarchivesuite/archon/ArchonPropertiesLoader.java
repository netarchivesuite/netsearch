package dk.statsbiblioteket.netarchivesuite.archon;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArchonPropertiesLoader {
	
	private static final Logger log = LoggerFactory.getLogger(ArchonPropertiesLoader.class);
	private static final String ARCHON_PROPERTY_FILE = "archon.properties";
	 
	private static final String H2_DB_FILE_PROPERTY="archon.h2.db.file";
	private static final String H2_DB_BACKUP_FOLDER_PROPERTY="archon.h2.db.backup.folder";
	
	
	public static String DBFILE = null;
	public static String DBBACKUPFOLDER = null;
	public static String DOMS_SOLR_SERVER = null;
	
	static{
		log.info("Initializing Archon-properties");
		try {
			initProperties();		
		} 
		catch (Exception e) {
			e.printStackTrace();
			log.error("Could not load property file:"+ARCHON_PROPERTY_FILE);					
		}
	}
		
	private static void initProperties()  throws Exception{

		String user_home=System.getProperty("user.home");
		log.info("Load properties: Using user.home folder:" + user_home);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(user_home,ARCHON_PROPERTY_FILE)), "ISO-8859-1");

		Properties serviceProperties = new Properties();
		serviceProperties.load(isr);
		isr.close();

		DBFILE =serviceProperties.getProperty(H2_DB_FILE_PROPERTY);		
		DBBACKUPFOLDER =serviceProperties.getProperty(H2_DB_BACKUP_FOLDER_PROPERTY);
			
		log.info("Property:"+ H2_DB_FILE_PROPERTY +" = " + DBFILE );
		log.info("Property:"+ H2_DB_BACKUP_FOLDER_PROPERTY +" = "+ DBBACKUPFOLDER );
		
	}
	
}
