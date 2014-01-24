package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.util.ArrayList;
import java.util.HashSet;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexWorker.RUN_STATUS;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;

public class IndexBuilder {

    private static long gB = 1073741824l;
    private static long mB = 1048576l;
     
    private static HashSet<IndexWorker> workers = new HashSet<IndexWorker>(); 
               
    private static long max_workers = 10;
    
    private static long index_size_target = 100*mB; //300 MB for test
    private static double optimize_limit=0.96; //96%
    private static double index_target_limit=0.95; //95%
    
    
    private static String archon_url= "http://localhost:8080/netarchive-archon/services";
    private static String solr_url= "http://localhost:8983/solr";
    private static int shardId=1;
    private static ArctikaSolrJClient solrClient = new ArctikaSolrJClient(solr_url);                
    private static ArchonConnectorClient archonClient = new ArchonConnectorClient(archon_url);
    
    public static void main (String[] args) throws Exception{
        System.out.println("IndexBuilder started");
        System.out.println("SOLR_URL:"+solr_url);
        System.out.println("ShardID:"+shardId);
                
        SolrCoreStatus status = solrClient.getStatus();
        
        System.out.println("Core status:"+status);
        System.out.println("ShardID:"+shardId);           
                        
        //Clear shardId for old jobs that hang(status running)
        archonClient.clearIndexing(""+shardId);
        
        optimizeAndExitIfSizeIsReached();//Check index is not finished before we start
        
        do{                                                  
           //Cleanup in worker-pool
           checkAndRemoveFinishedWorkers();
           
           //Start up new workers until pool is full
           while (workers.size() < max_workers){
              startNewIndexWorker();                                             
               System.out.println("#workers:"+workers.size());
           }
                              
           Thread.sleep(10*1000l); //Sleep for 10 secs before checking workers           
        }
        while (optimizeAndExitIfSizeIsReached());
                          
        System.out.println("unexpected to get here");
     
    }

    private static boolean optimizeAndExitIfSizeIsReached() throws Exception, InterruptedException {        
        //Do we need to optimize yet?        
        if (solrClient.getStatus().getIndexSizeBytes() < index_size_target*optimize_limit){
          return true;
        }
        
        //Wait for all workers to complete. TODO, timer check for workers that hang
        while (workers.size() >0){
            checkAndRemoveFinishedWorkers();
            Thread.sleep(10*1000l); //Sleep 10 secs between checks   
        }
        
        
        //Do the optimize
        System.out.println(" Optimizing, size of index before optimize:"+solrClient.getStatus().getIndexSizeHumanReadable());
        long start=System.currentTimeMillis();
        solrClient.optimize();
        Thread.sleep(10*1000l);

        while (!solrClient.getStatus().isOptimized()){
           System.out.println("not optimized yet...");           
            Thread.sleep(10*1000l); //Sleep 10 secs between checks
        }
    
        SolrCoreStatus status = solrClient.getStatus();
        
        System.out.println("Optimize complete. Size of index after optimize:"+status.getIndexSizeHumanReadable() +". Optimize took in millis:"+(System.currentTimeMillis()-start));
        long indexSizeBytes = status.getIndexSizeBytes();
        
        //Too big? Stop with error
        if (indexSizeBytes >index_size_target){
            System.out.println("Total screw up. Index too large. Max allowed bytes="+index_size_target +" and index was:"+indexSizeBytes);
            System.exit(1);
        }
              
        //Big enough? stop with success
        if (indexSizeBytes > index_size_target*index_target_limit){
            float percentage= (1f*indexSizeBytes)/(1f*index_size_target)*100f;        
            System.out.println("Building of shardId="+shardId +" completed. Index limit percentage:"+percentage);  
            System.exit(0);            
        }
        return true;
    }


    private static void startNewIndexWorker() {
        String nextARC = archonClient.nextARC(""+shardId);
          if ("".equals(nextARC)){
              System.out.println("no more arc-files to index. Stopping index process. It can be continued when there are new arc-files");
              System.exit(1);
          }
                    
           IndexWorker newWorker = new IndexWorker(nextARC,  solr_url);
           workers.add(newWorker);
           new Thread(newWorker).start();
    }
    
    
    private static void checkAndRemoveFinishedWorkers(){
     
        HashSet<IndexWorker> finishedWorkers = new HashSet<IndexWorker>();
        
        //Cleanup in worker-pool
        for (IndexWorker worker : workers){
            RUN_STATUS workerStatus = worker.getStatus();
            if (workerStatus==RUN_STATUS.RUNNING || workerStatus==RUN_STATUS.NEW){
                //do nothing
            }
            if (workerStatus==RUN_STATUS.COMPLETED){
                System.out.println("Worker completed success:"+worker.getArcFile());
                finishedWorkers.add(worker);
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.COMPLETED);
                
            }
            if (workerStatus==RUN_STATUS.RUN_ERROR){
                System.out.println("Worker FAIL:"+worker.getArcFile());
                finishedWorkers.add(worker);
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.REJECTED);
            }                                             
        }         
        workers.removeAll(finishedWorkers);       
    }
    
}
