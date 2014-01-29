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

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Controls the build of a single Solr index up to a given size.
 */
public class JobController {
    private static Log log = LogFactory.getLog(JobController.class);

    private final JobOptions options;
    private final int maxBuilders;
    private final int maxSize; // MB

    public JobController(JobOptions options, int maxBuilders, int maxSize) {
        this.options = options;
        this.maxBuilders = maxBuilders;
        this.maxSize = maxSize;
        log.info("Constructed " + this);
    }

    /**
     * Starts the build process.
     */
    public void build() {

    }

    @Override
    public String toString() {
        return "JobController{maxBuilders=" + maxBuilders + ", maxSize=" + maxSize + ", options=" + options + ")";
    }
}
