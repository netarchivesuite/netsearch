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
import dk.statsbiblioteket.util.console.ProcessRunner;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * Spawns a new local JVM to process the (W)ARCs.
 */
public class IndexWorkerSpawnJVM extends IndexWorker {
    private static Log log = LogFactory.getLog(IndexWorkerSpawnJVM.class);

    public IndexWorkerSpawnJVM(Collection<String> arcFiles, String solrUrl, IndexBuilderConfig config) {
        super(arcFiles, solrUrl, config);
    }
    
    @Override
    protected void processARCs(Set<ARCStatus> arcs) throws Exception {
        //Example of final command (Belinda):
        //java -Xmx256M -Djava.io.tmpdir=/home/summanet/arctika2/arctica_tmp -jar /home/summanet/arctika2/warc-indexer-2.0.2-SNAPSHOT-jar-with-dependencies.jar -c /home/summanet/arctika2/config.conf -s  "http://localhost:9731/solr" /netarkiv/0101/filedir/15626-38-20070418024637-00385-sb-prod-har-001.statsbiblioteket.dk.arc

        if (!new File(configFile).exists()){
            throw new IllegalArgumentException("Warc indexer config file not found:'"+configFile+"'");
        }
        ProcessRunner runner = new ProcessRunner("java",
                                                 "-Dfile.encoding=UTF-8",
                                                 "-Xmx"+maxMemInMb+"M", //-Xmx256M etc
                                                 "-Djava.io.tmpdir="+tmpDir,
                                                 "-jar",
                                                 workerJarFile,
                                                 "-c",
                                                 configFile,
                                                 "-s",
                                                 solrUrl,
                                                 join(arcs, " "));

        runner.setTimeout(WORKER_TIMEOUT); // 1 hour
        runner.run(); //this will wait until native call returned
        int returnCode = runner.getReturnCode();

        if (returnCode == 0){
            for (ARCStatus arcStatus: arcs) {
                arcStatus.setStatus(ArchonConnector.ARC_STATE.COMPLETED);
            }
            setStatus(RUN_STATUS.COMPLETED);
            log.info("Completed indexing of " + arcs.size() + " ARC files: " + join(arcs));
        } else{
            log.info("Error processing: "+ join(arcs) + ", return code not expected: "+returnCode +
                     ", error output: "+runner.getProcessErrorAsString());
            for (ARCStatus arcStatus: arcs) {
                arcStatus.setStatus(ArchonConnector.ARC_STATE.REJECTED);
            }
            setStatus(RUN_STATUS.RUN_ERROR);
        }
    }
}
