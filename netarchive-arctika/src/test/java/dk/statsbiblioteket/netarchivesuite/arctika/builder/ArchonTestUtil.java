package dk.statsbiblioteket.netarchivesuite.arctika.util;

import java.io.File;
import java.util.ArrayList;

import dk.statsbiblioteket.netarchivesuite.arctika.solr.ArctikaSolrJClient;
import dk.statsbiblioteket.netarchivesuite.arctika.solr.SolrCoreStatus;
import dk.statsbiblioteket.netarchivesuite.core.ArchonConnectorClient;

public class ArchonTestUtil {

    static ArchonConnectorClient client = new ArchonConnectorClient("http://localhost:8080/netarchive-archon/services");

    public static void main(String[] args)throws Exception{

              
      addAllArcFilesFromDirectory("/media/teg/500GB/netarchive_servers/tv1");
        
        //ArctikaSolrJClient. getNumberOfDocs();
    //    ArctikaSolrJClient.getIndexSizeInBytes();
   
      //  SolrCoreStatus status = new ArctikaSolrJClient("http://localhost:8983/solr").getStatus();
       // System.out.println(status);
        
    }

    public static void addAllArcFilesFromDirectory(String directory){

        ArrayList<String> arcFilesInFolder = getArcFilesInFolder(directory);

        for (String file : arcFilesInFolder){
            client.addARC(file);
        }

    }

    //Returns the fullpath of the .arc files in the folder
    public static ArrayList<String> getArcFilesInFolder(String folderPath){

        ArrayList<String> files = new ArrayList<String>();
        File folder = new File(folderPath);

        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            if (file.isFile()) {
                if (file.getName().endsWith(".arc"));

                System.out.println("File " + listOfFiles[i].getAbsolutePath());
                files.add(listOfFiles[i].getAbsolutePath());
            } 
        }

        return files;
    }

}

