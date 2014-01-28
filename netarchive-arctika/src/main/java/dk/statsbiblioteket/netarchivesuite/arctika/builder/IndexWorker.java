package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class IndexWorker implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);    
    public static enum RUN_STATUS {NEW,RUNNING, COMPLETED,RUN_ERROR} 
    private String arcFile;
    private String workerJarFile;
    private int maxMemInMb;
    private String solrUrl;
    private long startTime;
    private RUN_STATUS status;
                
    public IndexWorker(String arcFile,String solrUrl, int maxMemInMb, String workerJarFile){
        this.arcFile=arcFile;
        this.startTime=System.currentTimeMillis();
        this.solrUrl=solrUrl;
        this.maxMemInMb=maxMemInMb;
        this.workerJarFile=workerJarFile;
        status=RUN_STATUS.NEW;         
    }
        
    public String getArcFile() {
        return arcFile;
    }

    public void setArcFile(String arcFile) {
        this.arcFile = arcFile;
    }

    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public RUN_STATUS getStatus() {
        return status;
    }

    public void setStatus(RUN_STATUS status) {
        this.status = status;
    }
   
    public void run() {
        status =RUN_STATUS.RUNNING;
        log.info("Started indexing:"+arcFile);       
        
        try{                   
           //Example of final command: 
           //java -Xmx256M -jar /media/teg/500GB/netarchive_servers/warc-discovery/warc-indexer/warc-indexer-1.1.1-SNAPSHOT-jar-with-dependencies -s "http://localhost:8983/" arcFile1.arc
            
         ProcessRunner runner = new ProcessRunner("java",
                 "-Xmx"+maxMemInMb+"M", //-Xmx256M etc              
                 "-jar",
                 workerJarFile,
                 "-s",
                 solrUrl,
                 arcFile);
         runner.setTimeout(60*60*1000l); // 1 hour
         
         runner.run(); //this will wait until native call returned         
         int returnCode = runner.getReturnCode();

         if (returnCode == 0){
             status= RUN_STATUS.COMPLETED;    
             log.info("Completed indexing:"+arcFile);     
         }
         else{                          
             log.info("Error processing:"+arcFile);
             log.info("return code not expected:"+returnCode);
             log.info("error output:"+runner.getProcessErrorAsString());                        
             status = RUN_STATUS.RUN_ERROR;             
         }
         
        }
        catch(Exception e){
            log.info("Error processing:"+arcFile,e);           
            status = RUN_STATUS.RUN_ERROR;
            return;
        }
       
    }
    
    // only arcFile attribute used for hashCode and equal 
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((arcFile == null) ? 0 : arcFile.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndexWorker other = (IndexWorker) obj;
        if (arcFile == null) {
            if (other.arcFile != null)
                return false;
        } else if (!arcFile.equals(other.arcFile))
            return false;
        return true;
    }            
}
