package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class IndexWorker implements Callable<IndexWorker> {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);
    // TODO: Make this a property
    public static final long WORKER_TIMEOUT = 60 * 60 * 1000L;

    // A bit ugly with globals...
    public static final AtomicInteger workerCount = new AtomicInteger(0);
    public static final AtomicLong workerTime = new AtomicLong(0);
    public static String timeStats() {
        return workerCount.get() == 0 ? "N/A" : String.format("%.1f", workerTime.get() / workerCount.get() / 1000.0);
    }

    public static enum RUN_STATUS {NEW,RUNNING, COMPLETED,RUN_ERROR}
    private String arcFile;
    private String workerJarFile;
    private int maxMemInMb;
    private String solrUrl;
    private RUN_STATUS status;
    private long runtimeMS = 0;
                
    public IndexWorker(String arcFile,String solrUrl, int maxMemInMb, String workerJarFile){
        this.arcFile=arcFile;
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

    public RUN_STATUS getStatus() {
        return status;
    }

    public void setStatus(RUN_STATUS status) {
        this.status = status;
    }

    /**
     * @return the number of MS the job took. Calling this before the job has been finished will return 0.
     */
    public long getRuntime() {
        return runtimeMS;
    }

    @Override
    public IndexWorker call() throws Exception {
        status = RUN_STATUS.RUNNING;
        final long startTime = System.currentTimeMillis();
        log.info("Started indexing: "+arcFile + " with worker #" + workerCount.incrementAndGet());
        
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
         runner.setTimeout(WORKER_TIMEOUT); // 1 hour
         
         runner.run(); //this will wait until native call returned         
         int returnCode = runner.getReturnCode();

         if (returnCode == 0){
             status= RUN_STATUS.COMPLETED;    
             log.info("Completed indexing: "+arcFile);
         }
         else{                          
             log.info("Error processing: "+arcFile);
             log.info("return code not expected: "+returnCode);
             log.info("Error output: "+runner.getProcessErrorAsString());
             status = RUN_STATUS.RUN_ERROR;             
         }
         
        } catch(Exception e){
            log.info("Error processing: "+arcFile,e);
            status = RUN_STATUS.RUN_ERROR;
        }
        runtimeMS = System.currentTimeMillis() - startTime;
        workerTime.addAndGet(runtimeMS);
        return this;
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
