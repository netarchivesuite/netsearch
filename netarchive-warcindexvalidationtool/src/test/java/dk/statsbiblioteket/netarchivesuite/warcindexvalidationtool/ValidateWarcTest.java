package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.util.HashSet;

public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       

  String warc="/media/teg/1200GB_SSD/netarkiv/warcs1/testharvest-00000.warc.gz";
    
    
    //String warc="/netarkiv/denstoredanske/denstoredanske1923.warc.gz";
    String solr=null;
    solr = "http://localhost:8983/solr/netarchivebuilder"; //Leave as null if you dont want to validate against a solr index
    
    //In config3, we index all http status codes.
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    httpStatusPrefix.add(3);
    httpStatusPrefix.add(4);
    httpStatusPrefix.add(5);
    httpStatusPrefix.add(6);
    httpStatusPrefix.add(7);
    httpStatusPrefix.add(8);
    httpStatusPrefix.add(9);        
    
    ValidateWarc validateWarc = new ValidateWarc(warc,solr,true, httpStatusPrefix);  
    validateWarc.validate();
  }

}
