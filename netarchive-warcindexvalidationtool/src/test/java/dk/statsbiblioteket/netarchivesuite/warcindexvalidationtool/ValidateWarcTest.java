package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.HashSet;

public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       
    String warc="/netarkiv/0212/filedir/276693-272-20170622081520337-00007-kb-prod-har-002.kb.dk.warc.gz";               
    //String warc="/netarkiv/archiveit/ARCHIVEIT-7800-TEST-JOB285755-20170323022423574-00002.warc.gz";
    
    //String warc="/netarkiv/denstoredanske/denstoredanske1923.warc.gz";
    
    String solr = "http://localhost:8983/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
   // String solr = "http://localhost:8983/solr/collection1";
//   String solr = "http://ariel:52300/solr/collection1";
    
    
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    httpStatusPrefix.add(404);
    ValidateWarc validateWarc = new ValidateWarc(warc,solr,httpStatusPrefix);  
    validateWarc.validate();
  }

}
