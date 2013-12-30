/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.netarchivesuite.arctika;

import org.apache.commons.cli.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.util.Properties;

/**
 * Arctika overall options.
 */
public class JobOptions {
    private static Log log = LogFactory.getLog(JobOptions.class);

    public static final int DEFAULT_MAX_SLAVES = 20;
    public static final int DEFAULT_FAST_LIMIT = 8000; // 8 GB
    public static final int DEFAULT_CLOSE_LIMIT = 10000; // 10 GB
    public static final boolean DEFAULT_OPTIMIZE = true;
    private static final int DEFAULT_JOB_TIMEOUT = 30*60; // 30 minutes

    public static final File PERSISTENT_FILE = new File("arctica.persistent");
    public static final String PERSISTENT_SHARD = "shard";

    private final String archon;
    private final String solr;
    private final File index;
    private String shard = null;
    private final int maxSlaves;
    private final int fastLimit;
    private final int closeLimit;
    private final int jobTimeout;
    private final boolean optimize;

    /**
     * Parses command line arguments and extract options. It is the responsibility of the caller to call
     * {@link #getShardID()} and check if it is assigned a non-null-value. If null is returned, the caller must
     * ensure that a proper shardID is assigned (normally done by requesting a new shardID from archon).
     *
     * @param args arguments as passed to a Java main method.
     */
    public JobOptions(String[] args) {
        CommandLine line;
        try {
            Options options = getOptions();
            // parse the command line arguments
            CommandLineParser parser = new BasicParser();
            line = parser.parse(options, args);

            // validate that block-size has been set
            if (line.hasOption("block-size")) {
                // print the value of block-size
                System.out.println(line.getOptionValue("block-size"));
            }
        } catch (ParseException exp) {
            usage();
            throw new IllegalStateException("Unable to proceed due to faulty options", exp);
        }
        archon = getMandatoryString(line, "archon", "The URL for the Archon server must be specified");
        solr = getMandatoryString(line, "solr", "The URL for the Solr server must be specified");
        String iString = getMandatoryString(line, "index", "The folder for the Solr index must be specified");
        index = new File(iString);
        if (!index.exists()) {
            usage();
            throw new IllegalArgumentException("The folder '" + index + "' does not exist");
        }
        maxSlaves = getOptionalInt(line, "maxslaves", DEFAULT_MAX_SLAVES);
        fastLimit = getOptionalInt(line, "fastlimit", DEFAULT_FAST_LIMIT);
        closeLimit = getOptionalInt(line, "closelimit", DEFAULT_CLOSE_LIMIT);
        jobTimeout = getOptionalInt(line, "jobtimeout", DEFAULT_JOB_TIMEOUT);
        optimize = line.hasOption("optimize") ?
                Boolean.parseBoolean(line.getOptionValue("optimize")) :
                DEFAULT_OPTIMIZE;
        if (!line.hasOption("shardid")) {
            shard = getPersistedShardID();
        } else {
            shard = line.getOptionValue("shardid");
            log.info("shard explicitly set to '" + shard + "'");
        }
        log.info("Constructed " + this);
    }

    private String getPersistedShardID() {
        if (!PERSISTENT_FILE.exists()) {
            return null;
        }
        Properties prop = new Properties();
        try {
            InputStream in = getClass().getResourceAsStream(PERSISTENT_FILE.getCanonicalPath());
            prop.load(in);
            in.close();
        } catch (IOException e) {
            throw new IllegalStateException("The file '" + PERSISTENT_FILE + "' exists but cannot be read", e);
        }
        String shardID = prop.getProperty(PERSISTENT_SHARD);
        log.info("Resolved shard ID '" + shardID + "' from '" + PERSISTENT_FILE + "'");
        return shardID;
    }

    private int getOptionalInt(CommandLine options, String key, int defaultValue) {
        if (options.hasOption(key)) {
            return defaultValue;
        }
        return Integer.parseInt(options.getOptionValue(key));
    }

    private String getMandatoryString(CommandLine options, String key, String message) {
        if (options.hasOption(key)) {
            return options.getOptionValue(key);
        }
        usage();
        throw new IllegalArgumentException(message + ". Missing value for key " + key);
    }

    private void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("arctika", getOptions());
    }

    // http://commons.apache.org/proper/commons-cli/usage.html
    private Options options = null;
    private Options getOptions() {
        if (options == null) {
            options = new Options();
            options.addOption(new Option("h", "help", false, "Display this text"));
            options.addOption(new Option("a", "archon", true, "The URL for an Archon instance (mandatory)"));
            options.addOption(new Option("s", "solr", true, "The URL for a Solr instance (mandatory)"));
            // TODO: Use the replicationhandler instead
            // http://wiki.apache.org/solr/SolrReplication
            // http://stackoverflow.com/questions/7934000/how-to-get-the-index-size-in-solr
            options.addOption(new Option("i", "index", true, "The folder for the Solr index (mandatory)"));
            options.addOption(new Option("d", "shardid", true, "The designation for the shard (optional)"));
            options.addOption(new Option("m", "maxslaves", true, "The maximum number of slaves to run in parallel in "
                                                                 + "the fast build phase (default: 20)"));
            options.addOption(new Option("f", "fastlimit", true, "The maximum index size in MB in the fast build phase "
                                                                 + "(default: 8000"));
            options.addOption(new Option("c", "closelimit", true, "The maximum index size in MB in the close build "
                                                                  + "phase (default: 10000)"));
            options.addOption(new Option("j", "jobtimeout", true, "The maximum number of seconds a single job is "
                                                                  + "allowed to run before being considered hanged "
                                                                  + "(default: 1800 (30 minutes))"));
            options.addOption(new Option("o", "optimize", true, "Boolean specifying whether or not an optimize should "
                                                                + "be performed after the close build phase "
                                                                + "(default: true)"));
        }
        return options;
    }

    /**
     * Setting the shardID has the side effect of persisting it.
     * @param shardID the new shard ID.
     */
    public void setShardID(String shardID) {
        this.shard = shardID;
        Properties prop = new Properties();
        prop.setProperty(PERSISTENT_SHARD, shardID);
        try {
            OutputStream out = new FileOutputStream(PERSISTENT_FILE);
            prop.store(out, "Arctika persistent options");
        } catch (IOException e) {
            throw new IllegalStateException("The file '" + PERSISTENT_FILE + "' could not be written", e);
        }
    }

    /* Plain getters */

    public String getArchon() {
        return archon;
    }

    public String getSolr() {
        return solr;
    }

    public File getIndex() {
        return index;
    }

    public int getMaxSlaves() {
        return maxSlaves;
    }

    public int getFastLimit() {
        return fastLimit;
    }

    public int getCloseLimit() {
        return closeLimit;
    }

    public boolean shouldOptimize() {
        return optimize;
    }
    public String getShardID() {
        return shard;
    }

    @Override
    public String toString() {
        return "JobOptions(archon='" + archon + '\'' + ", solr='" + solr + '\'' + ", index=" + index +
               ", shard='" + shard + '\'' + ", maxSlaves=" + maxSlaves + ", fastLimit=" + fastLimit +
               ", closeLimit=" + closeLimit + ", optimize=" + optimize + ')';
    }
}
