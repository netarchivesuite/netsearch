package statsbiblioteket.netarchivesuite.core;

import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;


//This is a manual integration-test and not a unittest. It requires a running server with the netarchive-archon deployed.
public class ArchonConnectorClientIntegrationTest {
    public static void main(String[] args){
      ArchonConnectorClient client = new ArchonConnectorClient("http://localhost:8080/netarchive-archon/services");
    System.out.println(client.nextShardID());
        //  client.addARC("home/netarc/test1s.arc");
        //  client.nextARC("1");
        //System.out.println( client.getShardIDs());
        //  client.setARCState("home/netarc/test1s.arc", ARC_STATE.NEW);
        //
        //   client.setARCProperties("home/netarc/test2s.arc", "1", ARC_STATE.RUNNING, 5);
        // client.clearIndexing("1");
        //client.setShardState("1", ARC_STATE.NEW, 6);
        //  System.out.println(client.getARCFiles("1"));
        //  client.removeARC("home/netarc/test2s.arc");        
    }

    
    
}
