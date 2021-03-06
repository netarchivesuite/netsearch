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
package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls a shell script and parses the result for statuses.
 * The shell script must take the Solr endpoint as first argument followed by n (W)ARCs
 * It is expected to output the status for the (W)ARCs on stdout with the pattern
 * {@code <return_code> <warc>}
 * one line per WARC. Lines not matching the pattern are ignored.
 */
public class IndexWorkerShellCall extends IndexWorker {
    private static Log log = LogFactory.getLog(IndexWorkerShellCall.class);

    private final String workerShellCommand;

    public IndexWorkerShellCall(Collection<String> arcFiles, String solrUrl, IndexBuilderConfig config)
            throws FileNotFoundException {
        super(arcFiles, solrUrl, config);
        workerShellCommand = config.getWorker_shell_command();
        checkExistenceOfScript();
    }
    
    @Override
    protected void processARCs(Set<ARCStatus> arcs) throws Exception {
        //Example of final command (Belinda):
        //java -Xmx1024M -Djava.io.tmpdir=/home/summanet/arctika2/arctica_tmp -jar /home/summanet/arctika2/warc-indexer-3.0.-SNAPSHOT-jar-with-dependencies.jar -c /home/summanet/arctika2/config.conf -s  "http://localhost:9731/solr" /netarkiv/0101/filedir/15626-38-20070418024637-00385-sb-prod-har-001.statsbiblioteket.dk.arc

        if (!new File(configFile).exists()){
            throw new IllegalArgumentException("Warc indexer config file not found:'"+configFile+"'");
        }

        List<String> commands = getCallArguments(arcs);
        log.debug("processARCs: Calling " + join(commands, "  "));
        ProcessRunner runner = new ProcessRunner(commands);

        try {
            runner.setTimeout(WORKER_TIMEOUT); // 1 hour
            runner.run(); //this will wait until native call returned
        } catch (Exception e) {
            String message = "processARCs: ProcessRunner with arguments " + Strings.join(commands) + " failed";
            log.warn(message, e);
            throw new RuntimeException(message, e);
        }
        int returnCode = runner.getReturnCode();

        log.debug("processARCs: Shell call finished: " + join(commands, "  "));
        String code = runner.getProcessOutputAsString();
        log.info("processARCs: Response from shell call:\n-----------------\n" + code + "-----------------");

        if (returnCode == 0){
            String[] result = code.split("\n");
            for (ARCStatus arcStatus: arcs) {
                arcStatus.setStatus(ArchonConnector.ARC_STATE.REJECTED); // Default
                boolean found = false;
                for (String line: result) {
                    if (line.contains(arcStatus.getArc())) {
                        Matcher matcher = STATUS_CODE.matcher(line);
                        if (matcher.matches()) {
                            int rc = Integer.parseInt(matcher.group(1));
                            log.info("Got return code " + rc + " for " + arcStatus.getArc());
                            arcStatus.setStatus(rc == 0 ? ArchonConnector.ARC_STATE.COMPLETED :
                                                        ArchonConnector.ARC_STATE.REJECTED);
                            found = true;
                            break;
                        } else {
                            log.debug("Encountered line with warc, but without leading return code: " + line);
                        }
                    }
                }
                if (!found) {
                    log.warn("Unable to locate a return code for WARC " + arcStatus.getArc());
                }
            }
            setStatus(RUN_STATUS.COMPLETED);
            log.info("Completed indexing of " + arcs.size() + " ARC files: " + join(arcs));
        } else {
            log.info("Error processing: "+ join(arcs) + ", return code not expected: "+returnCode +
                     ", setting all (W)ARCs to rejected, error output: "+runner.getProcessErrorAsString());
            for (ARCStatus arcStatus: arcs) {
                arcStatus.setStatus(ArchonConnector.ARC_STATE.REJECTED);
            }
            setStatus(RUN_STATUS.RUN_ERROR);
        }
    }

    // $SOLR: Solr URL
    // $WARCS: All WARC files in the batch, each as a new argument
    // $MAX_MEM_MB: The value of arctika.worker.maxMemInMb
    // $TMP_DIR: The value of arctika.worker.tmp.dir
    // $INDEX_JAR: The value of arctika.worker.index.jar.file
    // $INDEXER_CONFIG: The property file specified with -DArctikaPropertyFile
    private List<String> getCallArguments(Set<ARCStatus> arcs) throws FileNotFoundException {
        List<String> arguments = new ArrayList<String>();
        String[] templates = workerShellCommand.split(" ");
        for (String template: templates) {
            if ("$WARCS".equals(template)) {
                for (ARCStatus arc: arcs) {
                    arguments.add(arc.getArc());
                }
            } else {
                arguments.add(template.
                        replace("$SOLR", solrUrl).
                        replace("$MAX_MEM_MB", Integer.toString(maxMemInMb)).
                        replace("$TMP_DIR", tmpDir).
                        replace("$INDEX_JAR", workerJarFile).
                        replace("$INDEXER_CONFIG", configFile)
                );
            }
        }
        return arguments;
    }

    private void checkExistenceOfScript() throws FileNotFoundException {
        final String script = workerShellCommand.split(" ")[0];
        if (Thread.currentThread().getContextClassLoader().getResource(script) == null &&
                !new File(script).exists()) {
            throw new FileNotFoundException(String.format("Unable to locate script '%s'", script));
        }
    }

    public static final Pattern STATUS_CODE = Pattern.compile("^([0-9]+)\\s.*");
}
