package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;


public class ValidateWarcTest {

  public static void main(String[] args)  throws Exception{
       
    //String warc="/netarkiv/0111/filedir/269879-242-20170108010756416-00045-sb-prod-har-001.statsbiblioteket.dk.warc";               
    String warc="/netarkiv/0211/filedir/48477-91-20090610093830-00033-sb-prod-har-005.arc";        
    String solr = "http://localhost:8983/solr/collection1"; //Leave as null if you dont want to validate against a solr index
                
    ValidateWarc validateWarc = new ValidateWarc(warc,solr);  
    validateWarc.validate();
  }

}
