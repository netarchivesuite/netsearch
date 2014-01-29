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

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;

import java.util.HashMap;
import java.util.Map;

public class ArchonFactory {
    private static Log log = LogFactory.getLog(ArchonFactory.class);

    private static Map<String, ArchonConnector> archons = new HashMap<String, ArchonConnector>();

    /**
     * Creates a connection to the given Archon URL (see the Archon sister project).
     * @param archon the address for a Archon server.
     * @return an ArchonConnector that acts as a plain Archon.
     */
    public static ArchonConnector connect(String archon) {
        if (archons.containsKey(archon)) {
            log.info("Returning stored archon with name '" + archon + "'");
            return archons.get(archon);
        }
        // TODO: Implement constructor for externam archon
        throw new UnsupportedOperationException("Remote Archon capability not implemented yet");
    }

    /**
     * Add a named Archon to the factory.
     * @param name   the name of the archon, used for lookup in {@link #connect(String)}.
     * @param archon the archon, ready for requests.
     */
    public static void addArchon(String name, ArchonConnector archon) {
        archons.put(name, archon);
    }

}
