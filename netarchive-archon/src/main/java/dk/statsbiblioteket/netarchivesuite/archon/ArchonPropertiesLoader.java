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
	 
	private static final String DB_DRIVER_PROPERTY="archon.database.driver"; 
    private static final String DB_URL_PROPERTY="archon.database.url";
    private static final String DB_USERNAME_PROPERTY="archon.database.username";
    private static final String DB_PASSWORD_PROPERTY="archon.database.password";
	
    public static String DB_DRIVER = null; 
    public static String DB_URL  = null;
    public static String DB_USERNAME = null;
    public static String DB_PASSWORD = null;
	
	
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
	     
	    DB_DRIVER = serviceProperties.getProperty(DB_DRIVER_PROPERTY);
	    DB_URL = serviceProperties.getProperty(DB_URL_PROPERTY);
		DB_USERNAME = serviceProperties.getProperty(DB_USERNAME_PROPERTY);
		DB_PASSWORD = serviceProperties.getProperty(DB_PASSWORD_PROPERTY);
		
		log.info("Property:"+ DB_DRIVER_PROPERTY +" = "+ DB_DRIVER);
	    log.info("Property:"+ DB_URL_PROPERTY +" = "+ DB_URL);
	    log.info("Property:"+ DB_USERNAME_PROPERTY +" = "+ DB_USERNAME);
	    log.info("Property:"+ DB_PASSWORD_PROPERTY +" = *******");			
		
	}
	
}
