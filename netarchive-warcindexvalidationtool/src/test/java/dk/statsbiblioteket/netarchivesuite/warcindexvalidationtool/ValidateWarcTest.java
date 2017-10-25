package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.HashSet;

public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       
   // String warc="/netarkiv/0105/filedir/272829-30-20170318193124175-00168-sb-prod-har-001.statsbiblioteket.dk.warc.gz";               
    //String warc="/netarkiv/0105/filedir/272074-267-20170310205622784-00003-kb-prod-har-001.kb.dk.warc.gz";
    //String warc="/netarkiv/denstoredanske/denstoredanske_pagesq-00006.warc.gz";
   String warc="/home/teg/Downloads/solrwayback_2017-10-25-15-17-47.warc";
    
    //  String warc="/netarkiv/denstoredanske/denstoredanske_pagesq-00000.warc.gz";  
   // String warc="/netarkiv/WEB-20171016120740059-00000-17407~kaah~8443.warc";  //PROBLEM
    //String warc="/netarkiv/archiveit/ARCHIVEIT-7800-TEST-JOB285755-20170323022423574-00002.warc.gz";
    
    
    
    //String warc="/netarkiv/denstoredanske/denstoredanske1923.warc.gz";
    String solr=null;
    //String solr = "http://narcana-data10.statsbiblioteket.dk:9000/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
    solr = "http://localhost:8983/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
   //String solr = "http://localhost:8983/solr/collection1";   
 //    String solr = "http://ariel:52300/solr/collection1";

    
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    httpStatusPrefix.add(4);
    httpStatusPrefix.add(5);
    ValidateWarc validateWarc = new ValidateWarc(warc,solr,true, httpStatusPrefix);  
    validateWarc.validate();
  }

}
