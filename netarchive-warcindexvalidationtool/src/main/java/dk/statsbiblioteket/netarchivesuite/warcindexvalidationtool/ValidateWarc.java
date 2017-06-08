package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.warc.WARCReaderFactory;


/**
 * Take a Arc/Warc file as input and validate records are in the Solr index (using the WarcIndexer) 
 *
 * @author Thomas Egense
 */
public class ValidateWarc {


  private SolrClient solrClient = null;
  private  String warcFilePath;
  private boolean isWarc = false; //else it is Arc

  /**
   * @param args pathToWarcFile solrServerUrl (optional)
   * pathToWarcFile can be and Arc or Warc file (and gz'ed)
   * if solrServerUrl is given, it will be checked the Solr index has the expected number of records and list missing records
   * @throws Exception 
   */
  public static void main(String[] args) {

    try{
      if (args.length== 0 || args.length > 2){       
        System.out.println("Arguments are: pathToWarcFile solrServerUrl(optional)");
        System.exit(1);
      }
      String warcFile=args[0];
      String solrUrl = null;
      if(args.length == 2){
        solrUrl = args[1];
      }     

      ValidateWarc valWarc = new ValidateWarc(warcFile,solrUrl);
      valWarc.validate();
    }
    catch(Exception e){      
      e.printStackTrace();
    }

  }


  public ValidateWarc(String warcFilePath, String solrServerUrl){
    this.warcFilePath=warcFilePath;
    solrClient = new SolrClient(solrServerUrl);    
  }

  public void validate() throws Exception{
    System.out.println("Reading warc file:"+warcFilePath);
    String[] fileTokens = warcFilePath.split("/");
    String arcFile = fileTokens[fileTokens.length-1]; //Get the filename from the full path
    System.out.println("File:"+arcFile);

    // Set up a local compressed WARC file for reading 
    BufferedInputStream bis = new BufferedInputStream (new FileInputStream(warcFilePath));

    ArchiveReader ar = null;
    if(arcFile.endsWith(".warc") || arcFile.endsWith(".warc.gz")){
      isWarc=true;
      ar = WARCReaderFactory.get(warcFilePath, bis, true);
    }
    else{
      ar = ARCReaderFactory.get(warcFilePath, bis, true);  
    }

    // The list has entries of the form: Filename@offset    
    //269879-242-20170108010756416-00045-sb-prod-har-001.statsbiblioteket.dk.warc@1460
    //In the Solr Index this field is: source_file_s
    ArrayList<String> solrSourceFileRecord= new ArrayList<String>(); 

    TreeMap<String, Integer> httpCodeCount = new TreeMap<String, Integer>(); 
    TreeMap<String, Integer> contentTypeCount = new TreeMap<String, Integer>();

    int expectedNumberOfDocsInSolr = 0;
    int totalNumberOfRecordsInWarc=0;
    boolean firstRecord=true;
    for(ArchiveRecord r : ar) {
      if (!isWarc && firstRecord){//Skip first record in Arc file(meta data)
        firstRecord=false;
        continue;
      }

      boolean skip = false; 
      if (!isWarc && "text/dns".equals(r.getHeader().getHeaderFields().get("content-type"))){ //Arc, skip these
        skip = true;
      }
      else if (isWarc && !"application/http; msgtype=response".equals(r.getHeader().getHeaderFields().get("Content-Type"))){  //Warc, only read these
        skip=true;       
      }

      if (!skip){ 
        totalNumberOfRecordsInWarc++;
        //50 characters is more than enough to read the header line
        byte[] rawData = IOUtils.toByteArray(r, r.available());

        String httpCodeStr = getHttpStatusCode(isWarc, rawData);
        int httpCode = Integer.parseInt(httpCodeStr);                      
        increaseCount(httpCodeStr,httpCodeCount);
        String type = "";
        if (isWarc){ //No type for arc files
          type = (String) r.getHeader().getHeaderValue("WARC-Type");        
          increaseCount(type,contentTypeCount);
        }        

        if(200 <= httpCode &&  httpCode < 300 && !"revisit".equals(type) ){ //only these records are sent to Solr                
          String solrRecord = arcFile+"@"+r.getHeader().getOffset();          
          solrSourceFileRecord.add(solrRecord);          
          expectedNumberOfDocsInSolr++;        
        }
      }
    }

    System.out.println("Read complete");
    //Log all parse results for the warc-file.
    System.out.println("File:"+warcFilePath);
    System.out.println("Total records:"+totalNumberOfRecordsInWarc);
    System.out.println("Expected Solr documents:"+expectedNumberOfDocsInSolr);
    System.out.println("Warc types:");
    for (String key: contentTypeCount.keySet()){
      System.out.println("  "+key+":"+contentTypeCount.get(key));
    }

    System.out.println("Http codes:");
    for (String key: httpCodeCount.keySet()){
      System.out.println("  "+key+":"+httpCodeCount.get(key));
    }



    if(solrClient != null){
      System.out.println("Validating records are found in Solr...");

      //Check first if solr has the correct number of documents. If not, check them one at a time

      int solrRecords = solrClient.countRecordsForFile(warcFilePath);
      if (solrRecords == expectedNumberOfDocsInSolr){
        System.out.println("The Solr index has the correct number of documents ("+expectedNumberOfDocsInSolr+")");
      }
      else{
        System.out.println("The Solr index does not have the correct number of documents! File:"+expectedNumberOfDocsInSolr +" solr index:"+solrRecords );
        System.out.println("Checking every document... (can takes up to 30 minutes)");

        boolean missing=false;
        int numberMissing=0;
        for (String r : solrSourceFileRecord){
          boolean found = solrClient.lookupRecord(r);
          if (!found){
            missing=true; //at least one is missing
            numberMissing++;
            System.out.println("Missing solr document:"+r);
          }
        }      
        if (missing){
          System.out.println("Validation error! Number of missing documents:"+numberMissing);
        }
        else{
          System.out.println("No missing documents in Solr");
        }
      }
    }

  }


  private String getHttpStatusCode(boolean isWarc, byte[] rawData){

    String content = new String(rawData);
    if (isWarc){
      String contentStart = content.substring(0, Math.min(500,content.length()));

      String[] tokens = contentStart.split(" ");
      String httpCodeStr =tokens[1]; 

      return httpCodeStr.trim();
    }
    else{
      // line example: HTTP/1.1 302 Found
      String contentStart = content.substring(0, Math.min(500,content.length()));
      String[] lines = contentStart.split("\n");
      String httpLine = lines[0];
      String[] tokens = httpLine.split(" ");      
      String httpCodeStr =tokens[1]; 
      return httpCodeStr.trim();

    }

  }

  public void increaseCount(String key, TreeMap<String,Integer> map){
    if(map.get(key)== null){
      map.put(key,0);
    }
    int oldCount = map.get(key);
    map.put(key, ++oldCount); // just increase count       
  }

}