package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexWorker.RUN_STATUS;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;

public class IndexBuilder {

    private static Log log = LogFactory.getLog(IndexBuilder.class);
    private HashSet<IndexWorker> workers = new HashSet<IndexWorker>(); 
    private IndexBuilderConfig config;     
    private ArctikaSolrJClient solrClient;                        
    private ArchonConnectorClient archonClient;

    public static void main (String[] args) throws Exception{

        String propertyFile = System.getProperty("ArtikaPropertyFile");
        if (propertyFile == null || "".equals(propertyFile)){
            System.out.println("Propertyfile location must be set. Use -DArtikaPropertyFile={path to file}");            
            log.error("Propertyfile location must be set. Use -DArtikaPropertyFile={path to file}");
            System.exit(1);
        }
        String log4JFile = System.getProperty("log4j.configuration");

        if (log4JFile  == null || "".equals(log4JFile )){
            log.info("Log4j configuration not defined, using default. Use -Dlog4j.configuration={path to file}");
            System.out.println("Log4j configuration not defined, using default. Use -Dlog4j.configuration={path to file}");                        
        }

        IndexBuilderConfig config = new IndexBuilderConfig(propertyFile);                
        IndexBuilder builder = new IndexBuilder(config);
        builder.buildIndex();
    }

    public IndexBuilder(IndexBuilderConfig config){
        this.config = config;                
        solrClient = new ArctikaSolrJClient(config.getSolr_url());
        archonClient =  new ArchonConnectorClient(config.getArchon_url());                       
    }

    public void buildIndex() throws Exception{        
        SolrCoreStatus status = solrClient.getStatus();        
        log.info("Starting building index for shardID:"+config.getShardId());
        log.info("Index status:"+status);

        //Clear shardId for old jobs that hang(status RUNNING)
        archonClient.clearIndexing(""+config.getShardId());

        optimizeAndExitIfSizeIsReached();//Check index has target size before we start indexing further

        do{                                                  
            //Cleanup in worker-pool
            checkAndRemoveFinishedWorkers();

            //Start up new workers until pool is full
            while (workers.size() < config.getMax_concurrent_workers()){
                startNewIndexWorker();                                                        
            }

            Thread.sleep(10*1000l); //Sleep for 10 secs before checking workers           
        }
        while (optimizeAndExitIfSizeIsReached());                         
        System.out.println("unexpected to get here");
        System.exit(1);
    }


    private boolean optimizeAndExitIfSizeIsReached() throws Exception, InterruptedException {        
        //Do we need to optimize yet?        

        if (solrClient.getStatus().getIndexSizeBytes() < config.getIndex_max_sizeInBytes()*config.getOptimize_limit()){
            return true; //Size not reached.. Continue
        }

        //Wait for all workers to complete. The native worker process is given 1 hour timeout.
        System.out.println("Waiting for all workers to complete before optimizing...");
        while (workers.size() >0){
            checkAndRemoveFinishedWorkers();
            Thread.sleep(10*1000l); //Sleep 10 secs between checks   
        }        

        //Do the optimize
        log.info("Optimizing, size of index before optimize:"+solrClient.getStatus().getIndexSizeHumanReadable());
        long start=System.currentTimeMillis();
        solrClient.optimize();
        Thread.sleep(10*1000l);

        while (!solrClient.getStatus().isOptimized()){          
            Thread.sleep(60*1000l); //Sleep 60 secs between checks
        }

        SolrCoreStatus status = solrClient.getStatus();        
        log.info("Optimize complete. Size of index after optimize:"+status.getIndexSizeHumanReadable() +". Optimize took in millis:"+(System.currentTimeMillis()-start));
        long indexSizeBytes = status.getIndexSizeBytes();

        //Too big? Stop with error
        if (indexSizeBytes >config.getIndex_max_sizeInBytes()){
            log.error("Total screw up. Index too large. Max allowed bytes="+config.getIndex_max_sizeInBytes() +" and index was:"+indexSizeBytes);
            System.out.println("Total screw up. Index too large. Max allowed bytes="+config.getIndex_max_sizeInBytes() +" and index was:"+indexSizeBytes);
            System.exit(1);
        }

        //Big enough? stop with success
        if (indexSizeBytes > config.getIndex_target_limit()*config.getIndex_max_sizeInBytes()){
            float percentage= (1f*indexSizeBytes)/(1f*config.getIndex_max_sizeInBytes())*100f;        
            log.info("Building of shardId="+config.getShardId()+" completed. Index limit percentage:"+percentage);
            System.out.println("Building of shardId="+config.getShardId()+" completed. Index limit percentage:"+percentage);  
            System.exit(0);            
        }
        return true;
    }


    private void startNewIndexWorker() {
        String nextARC = archonClient.nextARC(""+config.getShardId());
        if ("".equals(nextARC)){
            log.error("no more arc-files to index. Stopping index process. It can be continued when there are new arc-files");
            System.out.println("no more arc-files to index. Stopping index process. It can be continued when there are new arc-files");
            System.exit(1);
        }

        IndexWorker newWorker = new IndexWorker(nextARC,config.getSolr_url(),config.getWorker_maxMemInMb(),config.getWorker_jar_file());
        workers.add(newWorker);
        new Thread(newWorker).start();
    }


    private  void checkAndRemoveFinishedWorkers(){
        HashSet<IndexWorker> finishedWorkers = new HashSet<IndexWorker>();

        //Cleanup in worker-pool
        for (IndexWorker worker : workers){
            RUN_STATUS workerStatus = worker.getStatus();
            if (workerStatus==RUN_STATUS.RUNNING || workerStatus==RUN_STATUS.NEW){
                //do nothing
            }
            if (workerStatus==RUN_STATUS.COMPLETED){
                log.info("Worker completed success:"+worker.getArcFile());
                finishedWorkers.add(worker);
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.COMPLETED);

            }
            if (workerStatus==RUN_STATUS.RUN_ERROR){
                log.info("Worker FAIL:"+worker.getArcFile());
                finishedWorkers.add(worker);
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.REJECTED);
            }                                             
        }         
        workers.removeAll(finishedWorkers);       
    }

}
