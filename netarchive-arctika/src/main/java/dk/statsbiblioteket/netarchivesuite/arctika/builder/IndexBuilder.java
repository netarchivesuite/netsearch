package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

import dk.statsbiblioteket.util.JobController;
import dk.statsbiblioteket.util.Profiler;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.IndexWorker.RUN_STATUS;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;

public class IndexBuilder {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilder.class);

    /**
     * The number of milliseconds to wait between each poll for index.isOptimized.
     */
    private static final long WAIT_OPTIMIZE = 60 * 1000L;


    private final JobController<IndexWorker> jobController;
    private final IndexBuilderConfig config;
    private final ArctikaSolrJClient solrClient;
    private final ArchonConnectorClient archonClient;

    private final Profiler fullProfiler = new Profiler();
    private final Profiler optimizeProfiler = new Profiler(100); // Unrealistically high to ensure full stats
    {
        optimizeProfiler.pause();
    }
    private final Profiler jobProfiler = new Profiler(9999, 10); // 9999 as expected ARCS is unknown (unusable ETA)
    private int failedWorkers = 0;

    public static void main (String[] args) throws Exception {
        String propertyFile = System.getProperty("ArctikaPropertyFile");
        if (propertyFile == null || "".equals(propertyFile)){
            String message = "Property file location must be set. Use -DArctikaPropertyFile={path to file}";
            System.out.println(message);
            log.error(message);
            System.exit(1);
        }
        String log4JFile = System.getProperty("log4j.configuration");

        if (log4JFile  == null || "".equals(log4JFile )){
            String message = "Log4j configuration not defined, using default. Use -Dlog4j.configuration={path to file}";
            log.info(message);
            System.out.println(message);
        }

        IndexBuilderConfig config = new IndexBuilderConfig(propertyFile);
        IndexBuilder builder = new IndexBuilder(config);
        builder.buildIndex();
         
        log.info("Index build job finished.");
        System.out.println("Index build job finished.");
        
    }

    public IndexBuilder(IndexBuilderConfig config){
        this.config = config;
        solrClient = new ArctikaSolrJClient(config.getSolr_url(),config.getCoreName());
        archonClient =  new ArchonConnectorClient(config.getArchon_url());
        jobController = new JobController<IndexWorker>(config.getMax_concurrent_workers(), true, true) {
            @Override
            protected void afterExecute(Future<IndexWorker> finished) {
                super.afterExecute(finished);
                IndexBuilder.this.taskFinished(finished);
            }
        };
    }


    @SuppressWarnings("StatementWithEmptyBody")
    public void buildIndex() throws Exception{
        SolrCoreStatus status = solrClient.getStatus();
        log.info("Starting building index for shardID "+config.getShardId() + " with status " + status);

        //Clear shardId for old jobs that hang(status RUNNING)
        archonClient.clearIndexing(""+config.getShardId());

        //Check index has target size before we start indexing further
        if (isIndexingFinished()) {
            log.info("Exiting without any updates as index size has been reached for shardID " + config.getShardId());
            return;
        }

        out: do {
            while (!isOptimizeLimitReached()) { // Contract: #activeWorkers < max
                // Start up new workers until pool is full or there are no more ARCs
                int newJobs = 0;
                while (jobController.getActiveCount() < config.getMax_concurrent_workers() && newJobs++ < 25) {
                    if (!startNewIndexWorker()) {
                        log.info("Could not start new worker (probably due to no more un-indexed ARCs or an ARC-file does not exist");
                        break out;
                    }
                }
                // Wait for at least one worker to finish or timeout
                if (jobController.poll(IndexWorker.WORKER_TIMEOUT, TimeUnit.MILLISECONDS) == null) {
                    log.error("Time exceeded " + IndexWorker.WORKER_TIMEOUT + " while waiting for any worker to finish."
                              + " Exiting");
                    break out;
                }
            }
        } while (!isIndexingFinished());

        // TODO: Avoid double timeout from isIndexingFinished in case of problems
        jobController.popAll(IndexWorker.WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
        int active = jobController.getActiveCount();
        if (active > 0) {
            log.warn("There are still " + active + " workers after popAll with timeout " + IndexWorker.WORKER_TIMEOUT
                     + " ms. The shutdown will leave some ARCs in Archon marked as currently being indexed");
        }

        log.info(getStatus());
    }
    private String toFinalTime(Profiler profiler, boolean useCurrentSpeed) {
        return profiler.getBeats() == 0 ? "N/A" : String.format("%.1f", 1 / profiler.getBps(useCurrentSpeed));
    }

    private boolean isOptimizeLimitReached() throws ExecutionException, InterruptedException, IOException, SolrServerException {
        long indexSizeBytes = solrClient.getStatus().getIndexSizeBytes();
        boolean limitReached= indexSizeBytes > config.getIndex_max_sizeInBytes()*config.getOptimize_limit();
        
        if (limitReached){
            log.info("index size over optimize limit, size: "+indexSizeBytes);
        }
        
        return limitReached;
    }

    private boolean isIndexingFinished() throws ExecutionException, InterruptedException, IOException, SolrServerException {
        int emptyRun = 0;
        if (config.getMax_worker_tries() == 0) {
            log.warn("isIndexingFinished: config.getMax_worker_tries() == 0 with " + jobController.getActiveCount()
                     + " running jobs. Optimize will probably be skipped");
        }
        while (emptyRun++ < config.getMax_worker_tries() && jobController.getActiveCount() > 0) {
            log.debug("isIndexingFinished: Calling popAll on jobController with active jobs: "
                      + jobController.getActiveCount());
            jobController.popAll(IndexWorker.WORKER_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        int active = jobController.getActiveCount();
        if (active > 0) {
            log.error("There are still " + active + " workers after popAll with timeout " + IndexWorker.WORKER_TIMEOUT
                      + " ms. Optimization will not be run and index building will exit");
            return true;
        }
        long start = System.currentTimeMillis();
        if (solrClient.getStatus().isOptimized()) {
            log.debug("isOptimizedLimitReached: Index already optimized");
        } else {
            //Do the optimize
            log.info("Optimizing, size of index before optimize: " + solrClient.getStatus().getIndexSizeHumanReadable());
            jobProfiler.pause();
            optimizeProfiler.unpause();
            //Sometimes solr will automerge segments. Optimize is very time expensive, so check if it is really necessary.
            if (isOptimizeLimitReached()){               
              solrClient.optimize();
              long sleeptime = 500;
              while (!solrClient.getStatus().isOptimized()){
                  log.debug("Index not optimized yet. Waiting " + sleeptime + " ms before next check");
                  Thread.sleep(sleeptime);
                  sleeptime = sleeptime >= WAIT_OPTIMIZE / 2 ? WAIT_OPTIMIZE : sleeptime * 2;
              }            
            }
            else{
                log.info("Skipping optimize as index-size is currently below the optimize limit.");  
            }            
           
            optimizeProfiler.beat();
            optimizeProfiler.pause();
            jobProfiler.unpause();
            SolrCoreStatus status = solrClient.getStatus();
            log.info(String.format("Optimize %d complete. Size of index after optimize: %s. Optimize took %d ms",
                                   optimizeProfiler.getBeats(), status.getIndexSizeHumanReadable(),
                                   System.currentTimeMillis()-start));
        }
        SolrCoreStatus status = solrClient.getStatus();
        long indexSizeBytes = status.getIndexSizeBytes();

        //Too big? Stop with error
        if (indexSizeBytes >config.getIndex_max_sizeInBytes()){
            String message = "Total screw up. Index too large. Max allowed bytes="+config.getIndex_max_sizeInBytes()
                             +" but index was: "+indexSizeBytes;
            log.error(message);
            log.info(getStatus());
            System.err.println(message);
            throw new IllegalStateException(message);
        }

        //Big enough?
        return (solrClient.getStatus().isOptimized() && indexSizeBytes > config.getIndex_target_limit()*config.getIndex_max_sizeInBytes());
    }

    private boolean startNewIndexWorker() {
        String nextARC = archonClient.nextARC(""+config.getShardId());
        if ("".equals(nextARC)){
            String message = "No more arc-files to index. Stopping index process. " +
                             "It can be continued when there are new arc-files";
            log.warn(message);
            System.out.println(message);
            return false;
        }
 
        //Check file exist, or exit! Something is serious wrong.
        File f = new File(nextARC);
        if (!f.exists()){
            String message = "Arc-file does not exist. Indexing will exit when workers are finished. Missing file:"+nextARC;
            log.error(message);
            System.out.println(message);
            return false;            
        }

        String solrUrl = getSolrUrlWithCollection(config.getSolr_url(),config.getCoreName());
        jobController.submit(new IndexWorker(
                nextARC, solrUrl,
                config.getWorker_maxMemInMb(),
                config.getWorker_jar_file(),
                config.getWarcIndexerConfigFile(),
                config.getWorker_temp_dir()));                
        return true;
    }

    private void taskFinished(Future<IndexWorker> finished)  {
        IndexWorker worker;
        try {
            worker = finished.get();
        } catch (InterruptedException e) {
            log.warn("taskFinished: Interrupted while getting IndexWorker", e);
            return;
        } catch (ExecutionException e) {
            log.error("taskFinished: ExecutionException while getting IndexWorker", e);
            return;
        }

        jobProfiler.beat();
        String progress = String.format(
                "Finished ARC: %d (%s) in %d ms. Average worker processing speed: %s seconds/ARC. " +
                "Average clock speed: %s seconds/ARC",
                jobProfiler.getBeats(), worker.getArcFile(), worker.getRuntime(),
                IndexWorker.timeStats(), toFinalTime(jobProfiler, true));
        RUN_STATUS workerStatus = worker.getStatus();
        if (workerStatus==RUN_STATUS.COMPLETED){
            log.info("Worker completed success: " + worker.getArcFile() + " " + progress);
            archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.COMPLETED);

        }
        if (workerStatus==RUN_STATUS.RUN_ERROR){
                
            if (worker.getNumberOfErrors()<= config.getMax_worker_tries()){
                log.info("Worker failed. Will re-try. Error count: " + worker.getNumberOfErrors() +" arcfile: "
                         + worker.getArcFile() + " " + progress);
                worker.increaseErrorCount();
                worker.setStatus(IndexWorker.RUN_STATUS.NEW);
                jobController.submit(worker);
            } else {
                log.info("Worker FAIL: Error count: " + worker.getNumberOfErrors() + " arcfile: "
                         + worker.getArcFile() + " " + progress);
                failedWorkers++;
                archonClient.setARCState(worker.getArcFile(), ArchonConnector.ARC_STATE.REJECTED);
            }
        }
    }

    public static String getSolrUrlWithCollection(String solrUrl,String collectionName){    
      String newUrl = solrUrl;
      if (!newUrl.endsWith("/")){
        newUrl += "/";
      }
      
      return solrUrl+ collectionName;    
    }
    
    public String getStatus() throws IOException, SolrServerException {
        SolrCoreStatus status = solrClient.getStatus();
        long indexSizeBytes = status.getIndexSizeBytes();
        double percentage= (1d*indexSizeBytes)/(1d*config.getIndex_max_sizeInBytes())*100d;
        log.info("Building of shardId="+config.getShardId()+" completed. Index limit percentage: "+percentage);

        return String.format("Index status: %s. Processed ARCs: %d (%d failed) with average processing time " +
                             "%s seconds/ARC and average clock time %s seconds/ARC. " +
                             "Optimizations: %d with average time %s seconds/optimize. Total time spend: %s",
                             status, jobProfiler.getBeats(), failedWorkers, IndexWorker.timeStats(),
                             toFinalTime(jobProfiler, false),
                             optimizeProfiler.getBeats(), toFinalTime(optimizeProfiler, false),
                             fullProfiler.getSpendTime());
    }
}
