package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.HashSet;

public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       
    String warc="/netarkiv/temp/dsn_revisit-00000.warc.gz";  //PROBLEM              
    //String warc="/netarkiv/denstoredanske/denstoredanske_pagesq-00006.warc.gz";  //PROBLEM
   // String warc="/netarkiv/denstoredanske/denstoredanske_pagesq-00007.warc.gz";  //PROBLEM
    //String warc="/netarkiv/archiveit/ARCHIVEIT-7800-TEST-JOB285755-20170323022423574-00002.warc.gz";
    
    //String warc="/netarkiv/denstoredanske/denstoredanske1923.warc.gz";
    
    String solr = "http://localhost:8983/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
   //String solr = "http://localhost:8983/solr/collection1";   
 //    String solr = "http://ariel:52300/solr/collection1";

    
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    httpStatusPrefix.add(404);
    ValidateWarc validateWarc = new ValidateWarc(warc,solr,true, httpStatusPrefix);  
    validateWarc.validate();
  }

}
