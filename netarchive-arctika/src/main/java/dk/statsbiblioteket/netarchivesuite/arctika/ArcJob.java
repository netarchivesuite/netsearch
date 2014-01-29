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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Controls the handling of the content from a single ARC-file through Tika and into Solr.
 */
public class ArcJob {
    private static Log log = LogFactory.getLog(ArcJob.class);

    private final JobOptions options;
    private final File ARCFile;
    private final String solr;

    public ArcJob(JobOptions options, String ARCFile, String solr) {
        this.options = options;
        this.ARCFile = new File(ARCFile);
        this.solr = solr;
        log.debug("Constructed " + this);
    }

    @Override
    public String toString() {
        return "ArcJob(ARCFile='" + ARCFile + "', solr='" + solr + "', options=" + options + ")";
    }
}
