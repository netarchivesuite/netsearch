package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.statsbiblioteket.util.console.ProcessRunner;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("WeakerAccess")
public abstract class IndexWorker implements Callable<IndexWorker> {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);
    // TODO: Make this a property
    public static final long WORKER_TIMEOUT = 60 * 60 * 1000L;
    
    // A bit ugly with globals...
    public static final AtomicInteger workerCount = new AtomicInteger(0);
    public static final AtomicLong workerTime = new AtomicLong(0);
    public static String timeStats() {
        return workerCount.get() == 0 ? "N/A" : String.format("%.1f", workerTime.get() / workerCount.get() / 1000.0);
    }

    public enum RUN_STATUS {NEW, RUNNING, COMPLETED, RUN_ERROR}
    private int numberOfErrors = 0;
    private Set<ARCStatus> arcStatuses;
    private RUN_STATUS status; // Overall status
    private long runtimeMS = 0;

    protected String solrUrl;
    protected String workerJarFile;
    protected String configFile;
    protected int maxMemInMb;
    protected String tmpDir;

    public IndexWorker(Collection<String> arcFiles, String solrUrl, IndexBuilderConfig config){
        this.arcStatuses = ARCStatus.createList(arcFiles);
        this.solrUrl=solrUrl;
        this.maxMemInMb=config.getWorker_maxMemInMb();
        this.workerJarFile=config.getWorker_jar_file();
        this.tmpDir=config.getWorker_temp_dir();
        this.configFile=config.getWarcIndexerConfigFile();
        status=RUN_STATUS.NEW;         
    }
        
    public int getNumberOfErrors() {
        return numberOfErrors;
    }

    public void increaseErrorCount() {
        numberOfErrors++;
    }

    public RUN_STATUS getStatus() {
        return status;
    }

    public void setStatus(RUN_STATUS status) {
        this.status = status;
    }

    public Set<ARCStatus> getArcStatuses() {
        return arcStatuses;
    }

    public void setStatuses(Set<ARCStatus> statuses) {
        this.arcStatuses = statuses;
    }

    public List<String> getArcFiles() {
        List<String> arcs = new ArrayList<String>(arcStatuses.size());
        for (ARCStatus ac: arcStatuses) {
            arcs.add(ac.getArc());
        }
        return arcs;
    }


    public String getSolrUrl() {
        return solrUrl;
    }

    public void setSolrUrl(String solrUrl) {
        this.solrUrl = solrUrl;
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
        final long workerID = workerCount.incrementAndGet();
        log.info("Started indexing of " + arcStatuses.size() + " ARC files with worker #" + workerID + ": " +
                 join(arcStatuses));
        
        try{
            processARCs(arcStatuses);
        } catch(Exception e){
            log.info("Job Exception processing " + arcStatuses.size() + " (W)ARCs: "+ join(arcStatuses));
            for (ARCStatus arcStatus: arcStatuses) {
                arcStatus.setStatus(ArchonConnector.ARC_STATE.REJECTED);
            }
            status = RUN_STATUS.RUN_ERROR;
        }
        runtimeMS = System.currentTimeMillis() - startTime;
        workerTime.addAndGet(runtimeMS);
        return this;
    }

    /**
     * Process the given ARCs and set their status.
     * @throws Exception if thrown, all arcs will be marked as rejected.
     */
    protected abstract void processARCs(Set<ARCStatus> arcs) throws Exception;

    // only arcFile attribute used for hashCode and equal
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((arcStatuses == null) ? 0 : join(arcStatuses).hashCode());
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
        if (arcStatuses == null) {
            if (other.arcStatuses != null)
                return false;
        } else if (!join(arcStatuses).equals(join(other.arcStatuses)))
            return false;
        return true;
    }

    public static class ARCStatus {
        private final String arc;
        private ArchonConnector.ARC_STATE status = ArchonConnector.ARC_STATE.NEW;

        public ARCStatus(String arc) {
            this.arc = arc;
        }

        public String getArc() {
            return arc;
        }

        public ArchonConnector.ARC_STATE getStatus() {
            return status;
        }

        public void setStatus(ArchonConnector.ARC_STATE status) {
            this.status = status;
        }

        @Override
        public int hashCode() {
            return arc.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && (obj instanceof ARCStatus) && arc.equals(((ARCStatus) obj).arc);
        }

        // Hack! This _must_ be arc and nothing else as it is used for arguments to calls
        @Override
        public String toString() {
            return arc;
        }

        public static Set<ARCStatus> createList(Collection<String> arcs) {
            Set<ARCStatus> arches = new HashSet<ARCStatus>();
            for (String arc: arcs) {
                arches.add(new ARCStatus(arc));
            }
            return arches;
        }
    }

    public static String join(Collection<?> objects) {
        return join(objects, ",");
    }
    public static String join(Collection<?> objects, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Object o: objects) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }
            sb.append(o.toString());
        }
        return sb.toString();
    }
}
