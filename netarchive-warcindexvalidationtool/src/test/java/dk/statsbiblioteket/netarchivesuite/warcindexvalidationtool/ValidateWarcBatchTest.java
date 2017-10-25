package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

public class ValidateWarcBatchTest {

  
  /*
   * This is just a batched version of the validate warc method.
   * The input is a file with 1 arc file pr. line
   * 
   */
  
  public static void main(String[] args) throws Exception{
    
      
    //String solr = "http://ariel:52300/solr/collection1"; //Leave as null if you dont want to validate against a solr index
    String solr = "http://localhost:8983/solr/netarchivebuilder/";
    
          
    //Will just validate all filenames in this text-file. (1 warcfile pr. line)
    String warcFileList= "warcs.txt"; //in resources folder
    Paths.get(warcFileList);
  
    HashSet<Integer> httpStatusPrefix = new HashSet<Integer>();
    httpStatusPrefix.add(2); //2XX
    httpStatusPrefix.add(404); //2XX
    List<String> lines = Files.readAllLines(Paths.get(warcFileList));

    for (String line : lines){      
      ValidateWarc validateWarc = new ValidateWarc(line.trim(),solr,true,httpStatusPrefix);
      validateWarc.validate();      
    }
  }
   
}
