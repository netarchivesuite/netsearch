package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import dk.statsbiblioteket.util.console.ProcessRunner;

public class IndexWorker implements Runnable{

    
    public static enum RUN_STATUS {NEW,RUNNING, COMPLETED,RUN_ERROR} 
    private String arcFile;
    private String memOption;
    private String solrUrl;
    private long startTime;
    private RUN_STATUS status;
    private long endTime;
            
    public IndexWorker(String arcFile,String solrUrl){
        this.arcFile=arcFile;
        startTime=System.currentTimeMillis();
        this.solrUrl=solrUrl;
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
        System.out.println("Started indexing:"+arcFile);       
        try{
       
            
         //java -jar /media/teg/500GB/netarchive_servers/warc-discovery/warc-indexer/warc-indexer-1.0.1-SNAPSHOT-jar-with-dependencies -s "http://localhost:8983/" $I
            
         ProcessRunner runner = new ProcessRunner("java",
                 "-jar",
                 "/media/teg/500GB/netarchive_servers/warc-discovery/warc-indexer/warc-indexer-1.0.1-SNAPSHOT-jar-with-dependencies.jar",
                 "-s",
               solrUrl,
                 arcFile);
         runner.run(); //this will wait until native call returned         
         int returnCode = runner.getReturnCode();

         if (returnCode == 0){
             status= RUN_STATUS.COMPLETED;    
             endTime=System.currentTimeMillis();
             
         }
         else{
             System.out.println("return code not expected:"+returnCode);
             System.out.println("error output:"+runner.getProcessErrorAsString());
             
             status = RUN_STATUS.RUN_ERROR;             
         }
         
        }
        catch(Exception e){
            e.printStackTrace();
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
