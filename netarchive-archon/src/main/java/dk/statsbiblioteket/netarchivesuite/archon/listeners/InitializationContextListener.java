package dk.statsbiblioteket.netarchivesuite.archon.listeners;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.archon.ArchonPropertiesLoader;
import dk.statsbiblioteket.netarchivesuite.archon.persistence.ArchonStorage;

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

			 System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
			 System.setProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", "true");
			 
			 
		} catch (Exception e) {
			log.error("failed to initialize service", e);
			throw new RuntimeException("failed to initialize service", e);
		}
	
	    //Initialize DB when used in a WEB container. 
		try {
			ArchonStorage.initialize(ArchonPropertiesLoader.DB_DRIVER, 
					ArchonPropertiesLoader.DB_URL,
					ArchonPropertiesLoader.DB_USERNAME,
					ArchonPropertiesLoader.DB_PASSWORD);
			
		} 
		catch (Exception e) {
		    e.printStackTrace();
            log.info("Initialization failed", e);
            throw new RuntimeException("Shutdown webapp, due to failure in initial context", e);
		} 			
	
	}

	  // this is called by the web-container at shutdown. (defined in web.xml)
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down");
    
        try {      
            ArchonStorage.shutdown();
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();

                try {
                    log.debug("deregistering jdbc driver: {}", driver);
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    log.debug("Error deregistering driver {}", driver, e);
                }
            }

            log.info("Shutdown completed");
        } catch (Exception e) {
            log.info("Shutdown failed",e);
        }
    }


}

