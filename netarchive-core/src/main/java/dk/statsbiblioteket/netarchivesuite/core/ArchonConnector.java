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
package dk.statsbiblioteket.netarchivesuite.core;

import java.util.List;

public interface ArchonConnector {

    enum ARC_STATE {NEW, RUNNING, COMPLETED, REJECTED}

    /**
     * The nextShardID is guaranteed to be unique for the Archon and should be a logical successor to the previous
     * shardID: If the previous shardID was 'shard03', the next shard ID should be 'shard04'.
     * @return the next logical shard ID. Used when a new shard is about to be created.
     */
    String nextShardID();

    /**
     * Adds a new ARC file to the collection. An new ARC file will have no shardID assigned and state NEW.
     * @param arcID a unique identifier that can be resolved to an ARC file somewhere. Normally this if a file path.
     */
    void addARC(String arcID);

    /**
     * Only ARCs in state NEW and with matching shardID or no shardID are returned.
     * ARCs are selected by priorities:<br/>
     * 1) ARC files with the stated shardID are selected before those with no ID.<br/>
     * 2) ARC files with higher priorities are selected before those with lower.<br/>
     * 3) Lexicographical order (which hopefully matches chronological order).
     * </p><p>
     * It is the responsibility of Archon to set the shardID for the returned ARC to the given
     * shardID and to set the state of the returned ARC to RUNNING.
     * @param shardID the shardID helps the Archon to select the right ARC file.
     * @return the next free ARC file or the empty String if no more ARC files are available.
     */
    String nextARC(String shardID);

    /**
     * Set the state for the given ARC. This will normally be called when an ARC file has been indexed or has failed
     * indexing.
     * @param arcID the unique ID for the ARC.
     * @param state the new state.
     */
    void setARCState(String arcID, ARC_STATE state);

    /**
     * Iterates all ARC files that has the given shardID and state=RUNNING and sets the state to NEW.
     * This is normally used when Arctika has crashed and needs to re-process the active ARC files.
     * @param shardID the shardID for the ARC files that must have their state cleared.
     */
    void clearIndexing(String shardID);

    /* The methods below are for expected advanced future use */

    /**
     * Removes an ARC file to the collection.
     * @param arcID the ID for an existing ARC.
     */
    void removeARC(String arcID);

    /**
     * @return all unique shardIDs in the Archon. If used with rest, newline delimits the IDs.
     */
    List<String> getShardIDs();

    /**
     * @param shardID the shardID that the ARCs must match.
     * @return all ARCs with the given shardID. If used with rest, newline delimits the ARCs.
     */
    List<String> getARCFiles(String shardID);

    /**
     * Full property change of an ARC file. Not intended for use with standard index building.
     * @param arcID    the ID of the ARC file
     * @param shardID  the ID of the shard that the ARC belongs to.
     * @param state    the state of the ARC file.
     * @param priority the priority of the ARC file. Must be >= 0, higher numbers = higher priority.
     * @see {@link #setARCState(String, ARC_STATE)}.
     */
    void setARCProperties(String arcID, String shardID, ARC_STATE state, int priority);

    /**
     * Locates all ARC files with the given shardID and sets state and priority. Not intended for use with standard
     * index building.
     * @param shardID  the shardID that the ARC files must match.
     * @param state    the new state for the ARC files.
     * @param priority the new priority for the ARC files.
     * @see {@link #clearIndexing(String)}.
     */
    void setShardState(String shardID, ARC_STATE state, int priority);
}
