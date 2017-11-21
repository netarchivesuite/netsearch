package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class IndexBuilderConfig {
    private static final Logger log = LoggerFactory.getLogger(IndexBuilderConfig.class);

    public enum WORKER_TYPE {jvm, shell}

    private static final long GB = 1073741824L;
    private static final long MB = 1048576L;

    private int shardId = -1;
    private String worker_jar_file;
    private int max_worker_tries=3;
    private int worker_maxMemInMb = -1;
    private int max_concurrent_workers = -1;
    private int batch_size = 1; // Number of arc files to process in each job
    private long index_max_size = -1; //This will be combined with the unit below
    private long index_max_size_unit = 0; //will point to gB or mB
    
    private double optimize_limit = 0.96d; // 96% default value
    private double index_target_limit = 0.95d; // 95% default value
    private String archon_url;
    private String warcIndexerConfigFile;
    private String solr_url;
    private String solr_core_name;
    private String worker_temp_dir="/tmp"; 

    private WORKER_TYPE worker_type;
    private String worker_shell_command;

    public IndexBuilderConfig(String configFilePath) throws Exception {
        initProperties(configFilePath);
    }

    public String getWorker_jar_file() {
        return worker_jar_file;
    }

    public void setWorker_jar_file(String worker_jar_file) {
        this.worker_jar_file = worker_jar_file;
    }

    public int getWorker_maxMemInMb() {
        return worker_maxMemInMb;
    }
    
    public String getWorker_temp_dir() {
        return worker_temp_dir;
    }

    public void setWorker_temp_dir(String worker_temp_dir) {
        this.worker_temp_dir = worker_temp_dir;
    }

    public void setWorker_maxMemInMb(int worker_maxMemInMb) {
        this.worker_maxMemInMb = worker_maxMemInMb;
    }

    public int getMax_concurrent_workers() {
        return max_concurrent_workers;
    }

    public void setMax_concurrent_workers(int max_concurrent_workers) {
        this.max_concurrent_workers = max_concurrent_workers;
    }

    public int getBatch_size() {
        return batch_size;
    }

    public void setBatch_size(int batch_size) {
        this.batch_size = batch_size;
    }

    public WORKER_TYPE getWorker_type() {
        return worker_type;
    }

    public void setWorker_type(WORKER_TYPE worker_type) {
        this.worker_type = worker_type;
    }

    public String getWorker_shell_command() {
        return worker_shell_command;
    }

    public void setWorker_shell_command(String worker_shell_command) {
        this.worker_shell_command = worker_shell_command;
    }

    public int getMax_worker_tries() {
        return max_worker_tries;
    }

    public void setMax_worker_tries(int max_worker_tries) {
        this.max_worker_tries = max_worker_tries;
    }

    public long getIndex_max_size() {
        return index_max_size;
    }

    public void setIndex_max_size(long index_max_size) {
        this.index_max_size = index_max_size;
    }

    public long getIndex_max_sizeInBytes(){
        return index_max_size_unit*index_max_size;        
    }
        
    public double getOptimize_limit() {
        return optimize_limit;
    }

    public void setOptimize_limit(double optimize_limit) {
        this.optimize_limit = optimize_limit;
    }

    public double getIndex_target_limit() {
        return index_target_limit;
    }

    public void setIndex_target_limit(double index_target_limit) {
        this.index_target_limit = index_target_limit;
    }

    public String getArchon_url() {
        return archon_url;
    }

    public void setArchon_url(String archon_url) {
        this.archon_url = archon_url;
    }

    public String getSolr_url() {
        return solr_url;
    }

    public void setSolr_url(String solr_url) {
        this.solr_url = solr_url;
    }
    
    public String getCoreName() {
        return solr_core_name;
    }
   
    public void setCoreName(String solr_core_name) {
        this.solr_core_name = solr_core_name;
    } 

    public String getWarcIndexerConfigFile() {
        return warcIndexerConfigFile;
    }

    public void setWarcIndexerConfigFile(String warcIndexerConfigFile) {
        this.warcIndexerConfigFile = warcIndexerConfigFile;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }

    
    
    private void initProperties(String configFilePath) throws Exception {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(new File(configFilePath)), "ISO-8859-1");

        Properties serviceProperties = new Properties();
        serviceProperties.load(isr);
        isr.close();

        worker_maxMemInMb = Integer.parseInt(serviceProperties.getProperty("actika.worker.maxMemInMb"));
        max_concurrent_workers = Integer.parseInt(serviceProperties.getProperty("actika.max_concurrent_workers"));
        batch_size = Integer.parseInt(serviceProperties.getProperty("actika.batch_size"));

        String index_max_size_str = serviceProperties.getProperty("arctika.index_max_size");
        String unit;
        if (index_max_size_str.indexOf("MB")>0){           
            unit ="MB";
            index_max_size_unit = MB;
            index_max_size_str=index_max_size_str.replace("MB","");
            index_max_size=Long.parseLong(index_max_size_str.trim());            
        } else if (index_max_size_str.indexOf("GB")>0){
            unit ="GB";
            index_max_size_unit = GB;
            index_max_size_str=index_max_size_str.replace("GB","");
            index_max_size=Long.parseLong(index_max_size_str.trim());           
        } else throw new IllegalArgumentException(
                "Unknown arctika.index_max_size. MB or GB not defined: "+index_max_size_str);
        
        optimize_limit = Double.parseDouble(serviceProperties.getProperty("arctika.optimize_limit"));
        index_target_limit = Double.parseDouble(serviceProperties.getProperty("arctika.index_target_limit"));
        max_worker_tries = Integer.parseInt(serviceProperties.getProperty("actika.worker.tries"));
        shardId = Integer.parseInt(serviceProperties.getProperty("arctika.shardId"));
        archon_url = serviceProperties.getProperty("arctika.archon_url");
        solr_url = serviceProperties.getProperty("arctika.solr_url");
        solr_core_name=serviceProperties.getProperty("arctika.solr_core_name");
        warcIndexerConfigFile = serviceProperties.getProperty("arctika.worker.warcindexer.configfile");        
        worker_jar_file = serviceProperties.getProperty("arctika.worker.index.jar.file");
        worker_temp_dir = serviceProperties.getProperty("arctika.worker.tmp.dir");
        worker_type = WORKER_TYPE.valueOf(serviceProperties.getProperty("arctika.worker_type"));
        worker_shell_command = serviceProperties.getProperty("actika.worker_shell.command");

        log.info("Property: worker_jar_file = " + worker_jar_file);
        log.info("Property: max_worker_tries = " +max_worker_tries);
        log.info("Property: arctika.worker.tmp.dir = " +  worker_temp_dir);
        log.info("Property: actika.worker.maxMemInMb = " + worker_maxMemInMb);
        log.info("Property: max_concurrent_workers = " + max_concurrent_workers);
        log.info("Property: index_max_size = " + index_max_size+unit);
        log.info("Property: optimize_limit= = " + optimize_limit);
        log.info("Property: index_target_limit = " + index_target_limit);
        log.info("Property: shardId = " + shardId);
        log.info("Property: archon_url = " + archon_url);
        log.info("Property: warcIndexerConfigFile = " +  warcIndexerConfigFile );
        log.info("Property: solr_url = " + solr_url);
        log.info("Property: solr_core_name = " + solr_core_name);
    }

}
