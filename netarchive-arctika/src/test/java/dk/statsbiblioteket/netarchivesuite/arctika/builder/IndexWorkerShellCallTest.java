package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.Assert.*;

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
public class IndexWorkerShellCallTest {

    @Test
    public void testReturnCode() {
        Matcher matcher = IndexWorkerShellCall.STATUS_CODE.matcher("0 foo");
        assertTrue("Matcher should match", matcher.matches());
        assertEquals("Result should be correct", 0, Integer.parseInt(matcher.group(1)));
    }

    @Test
    public void testSuccess() throws Exception {
        final int ARCS = 10;

        IndexBuilderConfig config = new IndexBuilderConfig(locate("arctika.shell.properties"));
        config.setWarcIndexerConfigFile(locate("warc_test_config.conf"));
        config.setWorker_shell_command(config.getWorker_shell_command().replace(
                "ssh_test_script.sh", locate("ssh_test_script.sh"))); // Expand location
        List<String> arcs = new ArrayList<String>(ARCS);
        for (int i = 0 ; i < ARCS ; i++) {
            arcs.add("dummy_" + i);
        }
        IndexWorker worker = new IndexWorkerShellCall(arcs, "solrdummy", config);
        for (IndexWorker.ARCStatus arcStatus: worker.getArcStatuses()) {
            assertEquals("The state for arc '" + arcStatus.getArc() + "' before processing should be correct",
                         ArchonConnector.ARC_STATE.NEW, arcStatus.getStatus());
        }
        worker.call();
        for (IndexWorker.ARCStatus arcStatus: worker.getArcStatuses()) {
            assertEquals("The state for arc '" + arcStatus.getArc() + "' should be correct",
                         ArchonConnector.ARC_STATE.COMPLETED, arcStatus.getStatus());
        }
    }

    public String locate(String resource) throws FileNotFoundException {
        if (new File(resource).exists()) {
            return resource;
        }
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url != null) {
            return url.getFile();
        }
        throw new FileNotFoundException("Unable to locate '" + resource + "'");
    }
}