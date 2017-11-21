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

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnector;

import java.util.*;

public class MemoryArchon implements ArchonConnector {

    private final List<ARC> arcs = new ArrayList<ARC>();
    private int shardID = 0;

    private ARC getARC(String arcID) {
        for (ARC arc: arcs) {
            if (arc.getID().equals(arcID)) {
                return arc;
            }
        }
        throw new IllegalArgumentException("The ARC '" + arcID + "' is unknown");
    }

    @Override
    public String nextShardID() {
        return Integer.toString(shardID++);
    }

    @Override
    public synchronized void addARC(String arcID) {
        for (ARC arc: arcs) {
            if (arc.getID().equals(arcID)) {
                throw new IllegalArgumentException("An ARC with id '" + arcID + "' already exists");
            }
        }
        arcs.add(new ARC(arcID));
    }

    @Override
    public synchronized String nextARC(String shardID) {
        ARC best = null;
        for (ARC candidate: arcs) {
            if (candidate.getState() == ARC_STATE.NEW) {
                if (best == null ||
                    (!best.getShardID().equals(shardID) && candidate.getShardID().equals(shardID)) ||
                    (best.getPriority() < candidate.getPriority()) ||
                    (best.getID().compareTo(candidate.getID()) > 0)  ) {
                    best = candidate;
                }
            }
        }
        if (best == null) {
            return "";
        }
        return best.getID();
    }

    @Override
    public synchronized void setARCState(String arcID, ARC_STATE state) {
        ARC arc = getARC(arcID);
        arc.setState(state);
    }

    @Override
    public void setARCStates(Collection<String> arcIDs, ARC_STATE state) {
        for (String arcID: arcIDs) {
            setARCState(arcID, state);
        }
    }

    @Override
    public synchronized void clearIndexing(String shardID) {
        for (ARC arc: arcs) {
            if (arc.getShardID().equals(shardID) && arc.getState() == ARC_STATE.RUNNING) {
                arc.setState(ARC_STATE.NEW);
            }
        }
    }

    @Override
    public synchronized void removeARC(String arcID) {
        for (int i = 0 ; i < arcs.size() ; i++) {
            if (arcs.get(i).getID().equals(arcID)) {
                arcs.remove(i);
            }
        }
    }

    @Override
    public synchronized List<String> getShardIDs() {
        Set<String> ids = new LinkedHashSet<String>();
        for (ARC arc: arcs) {
            if (!ids.contains(arc.getShardID())) {
                ids.add(arc.getShardID());
            }
        }
        return new ArrayList<String>(ids);
    }

    @Override
    public synchronized List<String> getARCFiles(String shardID) {
        List<String> rarcs = new ArrayList<String>();
        for (ARC arc: arcs) {
            if (arc.getShardID().equals(shardID)) {
                rarcs.add(arc.getID());
            }
        }
        return rarcs;
    }

    @Override
    public synchronized void setARCProperties(String arcID, String shardID, ARC_STATE state, int priority) {
        ARC arc = getARC(arcID);
        arc.setShardID(shardID);
        arc.setState(state);
        arc.setPriority(priority);
    }

    @Override
    public synchronized void setShardState(String shardID, ARC_STATE state, int priority) {
        for (ARC arc: arcs) {
            if (arc.getShardID().equals(shardID)) {
                arc.setState(state);
                arc.setPriority(priority);
            }
        }
    }

    private class ARC {
        private final String id;
        private int priority = 1;
        private ARC_STATE state = ARC_STATE.NEW;
        private String shardID = null;

        public ARC(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return this == o ||
                   !(o == null || getClass() != o.getClass()) && id.equals(((ARC) o).getID());
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public String getID() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public ARC_STATE getState() {
            return state;
        }

        public void setState(ARC_STATE state) {
            this.state = state;
        }

        public String getShardID() {
            return shardID;
        }

        public void setShardID(String shardID) {
            this.shardID = shardID;
        }
    }
}
