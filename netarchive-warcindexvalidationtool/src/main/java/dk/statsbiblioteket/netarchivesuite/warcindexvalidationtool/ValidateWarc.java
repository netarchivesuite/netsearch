package dk.statsbiblioteket.netarchivesuite.warcindexvalidationtool;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
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
  private String warcFilePath;
  private boolean includeRevisits;
  private boolean isWarc = false; //else it is Arc
  private HashSet<Integer> statusCodePrefixes = new HashSet<Integer>();

  /**
   * @param args pathToWarcFile solrServerUrl (optional)
   * pathToWarcFile can be and Arc or Warc file (and gz'ed)
   * if solrServerUrl is given, it will be checked the Solr index has the expected number of records and list missing records
   * @throws Exception 
   */
  public static void main(String[] args) {    
    try{

      //Show help
      if (args.length == 0){
        System.out.println("Arguments are: pathToWarcFile(required) solrServerUrl(optional) revisits(boolean) httpstatus-prefixes (list, optional)");
        System.out.println("Example: filename.warc.gz    - This will list info about the warc-file such as number of different records, http-status codes etc. Will not validate against an index");
        System.out.println("Example: filename.warc.gz localhost:8983/solr/collectionName true 2 - validate the file against an index and expect all HTTP 2xx status codes are indexed. Will list missing records. Revisits included");
        System.out.println("Example: filename.warc.gz localhost:8983/solr/collectionName false 2 4 - expect both HTTP 2XX and HTTP 4XX status codes are indexed. No revisits.");
      }           
      else if (args.length ==1 ){
        String warcFile=args[0];
        System.out.println("Not validating against a solr index. Revisits are included");
        ValidateWarc valWarc = new ValidateWarc(warcFile,null,true, null);        
        valWarc.validate();
      }
      else if (args.length == 2  || args.length ==3 ){
        System.out.println("Missing revisits(boolean) and http codes. Example: filename.warc.gz localhost:8983/solr/collectionName false 2    (no, revisits and http 2xx status codes)");  
        System.exit(1);
      }
      else{
        String warcFile=args[0];        
        String solrUrl = args[1];
        String revisits = args[2].toLowerCase();
        if (! (revisits.equals("true") || revisits.equals("false"))){
          System.out.println("revists must be true or false (2. parameter)");
          System.exit(1);    
        }
        boolean includeRevisits = "true".equals(revisits);
        
        HashSet<Integer>  statusCodePrefixes = new HashSet<Integer>(); 
        try{
          //Parse http prefix.
          for (int i=3 ; i<args.length ;i++){
          System.out.println(args[i]);
            int httpPrefix = Integer.parseInt(args[i]); 
            statusCodePrefixes.add(httpPrefix);
          }
                 
        }
        catch(Exception e){
          System.out.println("Only numeric values for http status prefix");          
          System.exit(1);  
        }
        
        System.out.println("solrUrl:"+solrUrl);
        ValidateWarc valWarc = new ValidateWarc(warcFile,solrUrl,  includeRevisits, statusCodePrefixes);
        valWarc.validate();
        
      }      
    }
    catch(Exception e){      
      e.printStackTrace();
    }

  }


  public ValidateWarc(String warcFilePath, String solrServerUrl, boolean includeRevisits, HashSet<Integer> httpStatusPrefixAllowed) throws Exception{
    this.warcFilePath=warcFilePath;
    if (solrServerUrl != null){
      solrClient = new SolrClient(solrServerUrl);
      this.statusCodePrefixes = httpStatusPrefixAllowed;
      this.includeRevisits=includeRevisits;
    }
  }

  public void validate() throws Exception{
    System.out.println("Reading warc file:"+warcFilePath);
    String[] fileTokens = warcFilePath.split("/");
    String arcFile = fileTokens[fileTokens.length-1]; //Get the filename from the full path

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
      else if (isWarc){
        String contentType= (String) r.getHeader().getHeaderFields().get("Content-Type");
        
        // must be one of the two types of response. Heritrix uses first syntax, wget uses second
        if (!(contentType.startsWith("application/http; msgtype=response") || contentType.startsWith("application/http;msgtype=response") )){
          skip=true;          
        }        
        
      }
      if (!skip){ 
        //System.out.println(r.getHeader().getOffset());
        totalNumberOfRecordsInWarc++;
        //10000 characters is more than enough to read the header line
        int maxSize = Math.min(10000,  r.available());      
        byte[] rawData = IOUtils.toByteArray(r, maxSize);
        String httpCodeStr = getHttpStatusCode(isWarc, rawData);
        int httpCode = Integer.parseInt(httpCodeStr);                      
        increaseCount(httpCodeStr,httpCodeCount);
        String type = "";
        if (isWarc){ //No type for arc files
          type = (String) r.getHeader().getHeaderValue("WARC-Type");        
          increaseCount(type,contentTypeCount);
        }        
       
        // correct status and only revisits if includeRevists is true
        if( httpStatusInPrefixSet(httpCode,statusCodePrefixes) && ( ("revisit".equals(type) &&  includeRevisits ) || "response".equals(type)) ){ //only these records are sent to Solr                
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

      int solrRecords = solrClient.countRecordsForFile(warcFilePath,includeRevisits);
      if (solrRecords == expectedNumberOfDocsInSolr){
        System.out.println("The Solr index has the correct number of documents ("+expectedNumberOfDocsInSolr+")");
      }
      else{
        System.out.println("The Solr index does not have the correct number of documents! File:"+expectedNumberOfDocsInSolr +" solr index:"+solrRecords );
        ArrayList<String> solrIndexRecords = solrClient.lookupRecords(solrSourceFileRecord);
        solrSourceFileRecord.removeAll(solrIndexRecords); //will now only contain the missing records

        for (String rec : solrSourceFileRecord){
          System.out.println("Missing record:"+rec);
        }         
      }
    }

  }

  private boolean httpStatusInPrefixSet(int httpStatus, HashSet<Integer> statusCodePrefixes){
    for (int code: statusCodePrefixes){
      if (Integer.toString(httpStatus).startsWith(Integer.toString(code))){
        return true;
      }            
    }        
    return false;
  }
  
  private String getHttpStatusCode(boolean isWarc, byte[] rawData){

    String content = new String(rawData);
    if (isWarc){
      String contentStart = content.substring(0, Math.min(500,content.length()));

      String[] tokens = contentStart.split(" ");
      String httpCodeStr =tokens[1]; 
      if (httpCodeStr.indexOf("\n") >1){ //For some reason this can happen : 'HTTP/1.1 404\nContent-Type:'
        //System.out.println("new line detected:"+httpCodeStr);
        httpCodeStr = httpCodeStr.split("\n")[0];
      }
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