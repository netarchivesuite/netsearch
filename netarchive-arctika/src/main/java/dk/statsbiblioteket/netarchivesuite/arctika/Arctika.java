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
 * Entry class for the Arctika project.
 */
public class Arctika {
    private static Log log = LogFactory.getLog(Arctika.class);

    public static void main(String[] args) {
        long totalTime = -System.currentTimeMillis();
        JobOptions options = new JobOptions(args);
        ArchonConnector archon = ArchonFactory.connect(options.getArchon());
        if (options.getShardID() == null) {
            options.setShardID(archon.nextShardID());
        }

        build(options, "fast", options.getMaxSlaves(), options.getFastLimit());
        build(options, "close", 1, options.getCloseLimit());
        optimizeIndex(options);

        totalTime += System.currentTimeMillis();
        log.info("Finished index update in " + getHumanTime(totalTime));
    }

    private static void build(JobOptions options, String designation, int slaves, int limit) {
        log.info(String.format("Starting %s build phase with %d slaves and %d MB index size limit",
                               designation, slaves, limit));
        long buildTime = -System.currentTimeMillis();
        JobController fastController = new JobController(options, slaves, limit);
        fastController.build();
        buildTime += System.currentTimeMillis();
        log.info(String.format("Finished %s build phase in %s", designation, getHumanTime(buildTime)));
    }

    private static void optimizeIndex(JobOptions options) {
        if (!options.shouldOptimize()) {
            log.info("Skipping index optimization as specified in options");
            return;
        }

        log.info("Optimizing index");
        long optimizeTime = -System.currentTimeMillis();
        // TODO: Implement optimize
        log.warn("Optimize not implemented yet");
        optimizeTime += System.currentTimeMillis();
        log.info("Optimize completed in " + getHumanTime(optimizeTime));
    }

    private static String getHumanTime(long ms) {
        if (ms <= 3000) {
            return ms + " ms";
        } else if (ms <= 80*1000) {
            return ((ms + 500) / 1000) + " seconds";
        } else if (ms <= 80*60*1000) {
            return ((ms + 500) / (60*1000)) + " minutes";
        }
        return ((ms + 500) / (60*60*1000)) + " hours";
    }
}
