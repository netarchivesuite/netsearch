package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

@SuppressWarnings("WeakerAccess")
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

    public enum STATE {starting, indexing, checking, optimizing, finishing}
    private STATE builderState = STATE.starting;

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
    public void buildIndex() throws Exception {
        SolrCoreStatus status = solrClient.getStatus();
        log.info("Starting building index for shardID "+config.getShardId() + " with status " + status);
        builderState = STATE.indexing;

        //Clear shardId for old jobs that hang(status RUNNING)
        archonClient.clearIndexing(""+config.getShardId());

        //Check index has target size before we start indexing further
        if (isIndexingFinished()) {
            log.info("Exiting without any updates as index size has been reached for shardID " + config.getShardId());
            builderState = STATE.finishing;
            return;
        }

        out: do {
            while (!isOptimizeLimitReached()) { // Contract: #activeWorkers < max
                // Start up new workers until pool is full or there are no more ARCs
                // TODO: Change this to jobController.getTaskCount to get both queued, active and waiting
                while (jobController.getActiveCount() < config.getMax_concurrent_workers()) {
                    if (!startNewIndexWorker()) {
                        log.info("buildIndex: Could not start new worker (probably due to no more un-indexed ARCs or " +
                                 "an ARC-file does not exist");
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

        builderState = STATE.finishing;
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

    private boolean isOptimizeLimitReached() throws IOException, SolrServerException {
        long indexSizeBytes = solrClient.getStatus().getIndexSizeBytes();
        long optimizeLimit = (long) (config.getIndex_max_sizeInBytes() * config.getOptimize_limit());
        boolean limitReached = indexSizeBytes > optimizeLimit;
        
        if (limitReached){
            log.info(String.format(
                    "isOptimizeLimitReached: Index size %d bytes exceeds optimize limit %d bytes",
                    indexSizeBytes, optimizeLimit));
        }
        
        return limitReached;
    }

    private boolean isIndexingFinished() throws InterruptedException, IOException, SolrServerException {
        builderState = STATE.checking;
        if (config.getMax_worker_tries() == 0) {
            log.warn("isIndexingFinished: config.getMax_worker_tries() == 0 with " + jobController.getActiveCount()
                     + " running jobs. Optimize will probably be skipped");
        }

        // Wait up to retries*timeout for the workers to finish
        int active = jobController.getActiveCount();
        if (jobController.getActiveCount() > 0) {
            log.info("isIndexingFinished: Waiting for " + active + " workers to finish");
            int emptyRun = 0;
            List<Future<IndexWorker>> poppedJobs = new ArrayList<Future<IndexWorker>>(jobController.getActiveCount());
            while (emptyRun++ < config.getMax_worker_tries() && jobController.getActiveCount() > 0) {
                log.debug(String.format(
                        "isIndexingFinished: Calling popAll #%d/%d on jobController with %d active jobs",
                          emptyRun, config.getMax_worker_tries(), jobController.getActiveCount()));
                poppedJobs.addAll(jobController.popAll(IndexWorker.WORKER_TIMEOUT, TimeUnit.MILLISECONDS));
            }
            log.info("isIndexingFinished: Popped " + poppedJobs.size() + " workers out of expected " + active +
                     " workers");
        } else {
            log.info("isIndexingFinished: No active workers");
        }

        // Exit if there are still pending workers
        active = jobController.getActiveCount();
        if (active > 0) {
            log.error("There are still " + active + " workers after popAll with timeout " + IndexWorker.WORKER_TIMEOUT
                      + " ms. Optimization will not be run and index building will exit");
            builderState = STATE.finishing;
            return true;
        }

        performOptimize();
        SolrCoreStatus status = solrClient.getStatus();
        long indexSizeBytes = status.getIndexSizeBytes();

        //Too big? Stop with error
        if (indexSizeBytes > config.getIndex_max_sizeInBytes()){
            String message = String.format(
                    "isIndexingFinished: Total screw up. Index too large. Max allowed bytes=%d, but index was %d bytes",
                    config.getIndex_max_sizeInBytes(), indexSizeBytes);
            builderState = STATE.finishing;
            log.error(message);
            log.info(getStatus());
            System.err.println(message);
            throw new IllegalStateException(message);
        }

        //Big enough?
        boolean isFinished = solrClient.getStatus().isOptimized() &&
                             indexSizeBytes > config.getIndex_target_limit()*config.getIndex_max_sizeInBytes();
        builderState = isFinished ? STATE.finishing : STATE.indexing;
        log.info("isIndexingFinished: Returning isFinished=" + isFinished);
        return isFinished;
    }

    private void performOptimize() throws IOException, SolrServerException, InterruptedException {
        long start = System.currentTimeMillis();
        builderState = STATE.optimizing;
        log.debug("performOptimize: Flushing before checking for isOptimized");
        solrClient.flush();
        if (solrClient.getStatus().isOptimized()) {
            log.debug("performOptimize: Index already optimized");
            return;
        }

        jobProfiler.pause();
        //Sometimes solr will automerge segments. Optimize is very time expensive, so check if it is really necessary.
        if (isOptimizeLimitReached()){
            log.info("performOptimize: Issuing optimize call. Size of index before optimize: " +
                     solrClient.getStatus().getIndexSizeHumanReadable());
            optimizeProfiler.unpause();
            solrClient.optimize();
            long sleeptime = 10000; //10 sec
            while (!solrClient.getStatus().isOptimized()){
                log.debug("performOptimize: Index not optimized yet. Waiting " + sleeptime + " ms before next check");
                Thread.sleep(sleeptime);
                sleeptime = sleeptime >= WAIT_OPTIMIZE / 2 ? WAIT_OPTIMIZE : sleeptime * 2;
            }
            optimizeProfiler.beat();
            optimizeProfiler.pause();
        } else{
            log.info("performOptimize: Skipping optimize as index-size is currently below the optimize limit");
        }

        jobProfiler.unpause();
        SolrCoreStatus status = solrClient.getStatus();
        log.info(String.format(
                "performOptimize: Optimize %d complete. Size of index after optimize: %s. Optimize took %d ms",
                optimizeProfiler.getBeats(), status.getIndexSizeHumanReadable(),
                System.currentTimeMillis()-start));
    }

    private boolean startNewIndexWorker() throws FileNotFoundException {
        // Sanity check
        if (!canStartJobs()) {
            log.error(String.format(
                    "startNewIndexWorker: Logic error: Cannot start new workers as state is %s, while it should be %s",
                    builderState, STATE.indexing));
            return false;
        }

        // Request jobs from Archon
        List<String> arcs = new ArrayList<String>(config.getBatch_size());
        String nextARC;
        while (arcs.size() < config.getBatch_size() && (nextARC = getNextArcWithWait(config.getShardId())) != null) { //will wait and sleep for 10 min if archon is paused

            //Check if file exist, or exit! Something is serious wrong.
            File f = new File(nextARC);
            if (!f.exists()){
                String message = "startNewIndexWorker: Arc-file does not exist. Indexing will exit when workers are" +
                                 " finished. Missing file: " + nextARC;
                log.error(message);
                System.out.println(message);
                return false;
            }

            arcs.add(nextARC);
        }

        // Exit is no new jobs were available
        if (arcs.size() == 0){
            String message = "startNewIndexWorker: No more arc-files to index. Stopping index process. " +
                             "It can be continued when there are new arc-files";
            log.warn(message);
            System.out.println(message);
            return false;
        }

        // Start the worker
        String solrUrl = getSolrUrlWithCollection(config.getSolr_url(),config.getCoreName());
        jobController.submit(createWorker(arcs, solrUrl, config));
        log.debug("startNewIndexWorker: Started worker with " + arcs.size() + " ARCs");
        return true;
    }

    
    private String getNextArcWithWait(int shardid){
      
        String nextARC = archonClient.nextARC(""+shardid);
        while (nextARC.equals(ArchonConnectorClient.WAIT_MODE)){
          log.info("Sleeping 10 minutes, Archon is on pause mode");
          try{
             Thread.sleep(10*60*1000); //10 min
          }
          catch(Exception e){
            log.error("Error sleeping for 10 minutes while archon is in paused mode.");
          }
          nextARC = archonClient.nextARC(""+shardid); //try again
        }

        return nextARC;
      
    }
    
    private Callable<IndexWorker> createWorker(List<String> arcs, String solrUrl, IndexBuilderConfig config)
            throws FileNotFoundException {
        switch (config.getWorker_type()) {
            case jvm:
                return new IndexWorkerSpawnJVM(arcs, solrUrl, config);
            case shell:
                return new IndexWorkerShellCall(arcs, solrUrl, config);
            default:
                throw new UnsupportedOperationException(
                        "The worker type '" + config.getWorker_type() + "' is currently unsupported");
        }
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
        // TODO: Change stats to be per WARC-file
        String progress = String.format(
                "Finished job: %d with %d (W)ARCs (%s) in %d ms. Average worker processing speed: %s seconds/job. " +
                "Average clock speed: %s seconds/job",
                jobProfiler.getBeats(), worker.getArcStatuses().size(), IndexWorker.join(worker.getArcStatuses()),
                worker.getRuntime(), IndexWorker.timeStats(), toFinalTime(jobProfiler, true));
        RUN_STATUS workerStatus = worker.getStatus();
        if (workerStatus == RUN_STATUS.NEW || workerStatus == RUN_STATUS.RUNNING) {
            log.error("Logic error: Worker status was " + worker + ". Expected " + RUN_STATUS.COMPLETED +
                      " or " + RUN_STATUS.RUN_ERROR + ". Worker (W)ARCs in unknown state: "
                      + IndexWorker.join(worker.getArcFiles()));
            return;
        }

        Set<IndexWorker.ARCStatus> arcStatuses = worker.getArcStatuses();
        Set<IndexWorker.ARCStatus> arcRetry = new HashSet<IndexWorker.ARCStatus>(arcStatuses.size());
        for (IndexWorker.ARCStatus arcStatus: arcStatuses) {
            if (arcStatus.getStatus() == ArchonConnector.ARC_STATE.COMPLETED) {
                log.info("(W)ARC finished with success: " + arcStatus.getArc() + ". " + progress);
                archonClient.setARCState(arcStatus.getArc(), ArchonConnector.ARC_STATE.COMPLETED);
            } else if (arcStatus.getStatus() == ArchonConnector.ARC_STATE.REJECTED) {
                if (worker.getNumberOfErrors() < config.getMax_worker_tries() && canStartJobs()) {
                    log.info("(W)ARC failed. Will re-try. Error count: " + worker.getNumberOfErrors() + " arcfile: "
                             + arcStatus + " " + progress);
                    arcRetry.add(arcStatus);
                } else {
                    log.info("(W)ARC FAIL: Error count: " + worker.getNumberOfErrors() + " arcfile: "
                             + arcStatus + " " + progress);
                    failedWorkers++;
                    archonClient.setARCState(arcStatus.getArc(), ArchonConnector.ARC_STATE.REJECTED);
                }
            }
        }
        if (!arcRetry.isEmpty()) {
            worker.increaseErrorCount();
            worker.setStatus(IndexWorker.RUN_STATUS.NEW);
            worker.setStatuses(arcRetry);
            jobController.submit(worker);
        }
    }

    /**
     * @return true if failes jobs can be re-issued. This will be false during optimization and similar operations.
     */
    private boolean canStartJobs() {
        return builderState == STATE.indexing;
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
