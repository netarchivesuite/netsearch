package dk.statsbiblioteket.netarchivesuite.archon.listeners;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.archon.ArchonPropertiesLoader;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.H2Storage;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class InitializationContextListener implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(InitializationContextListener.class);
	
	//this is called by the web-container before opening up for requests.(defined in web.xml)
	public void contextInitialized(ServletContextEvent event) {

		Properties props = new Properties();
		try 
		{
			props.load(InitializationContextListener.class.getResourceAsStream("/build.properties"));

			String version = props.getProperty("APPLICATION.VERSION");
			log.info("Archon version "+version+" started successfully");
			
		} catch (Exception e) {
			log.error("failed to initialize service", e);
			throw new RuntimeException("failed to initialize service", e);
		}
	
	    //Initialize DB when used in a WEB container. 
		String dbFile=null;

		try {
		    dbFile=ArchonPropertiesLoader.DBFILE;			
			log.info("Connecting to H2 Database with DBfile:"+dbFile);
		    new H2Storage(dbFile); //Singleton
		    log.info("Connected");
		} 
		catch (Exception e) {
			e.printStackTrace();
			log.error("Could not connect to DBfile:"+dbFile+ ". ");
		} 			
	
	}

	//this is called by the web-container at shutdown. (defined in web.xml)
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			H2Storage.shutdown();
			log.info("Archon H2database shutdown succesfully");
	
		} catch (Exception e) {
			log.error("failed to shutdown service", e);
		}
	}



}

