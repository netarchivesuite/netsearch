package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.statsbiblioteket.netarchivesuite.arctika.builder.ArcFileNameParser.ArcMetaData.ARC_TYPE;



public class ArcFileNameParser {

    private static DateFormat arcDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    private static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

    // Example 25666-33-20080221003533-00046-sb-prod-har-004.arc
    private static final Pattern arc_sb_Pattern = Pattern.compile("([0-9]+)-([0-9]+)-([0-9]+)-([0-9]+)-(sb-prod-har)-([0-9]{1,3}).(statsbiblioteket.dk.warc|statsbiblioteket.dk.arc|arc)");

    // Example 15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc
    private static final Pattern arc_kb1_Pattern = Pattern.compile("([0-9]+)-([0-9]+)-([0-9]+)-([0-9]+)-(kb-prod-har|kb-prod-wb)-([0-9]{1,3}).(arc|kb.dk.arc|kb.dk.warc)");

    //Example 193305-197-20131111175547-00001-kb228081.kb.dk.warc
    private static final Pattern arc_kb2_Pattern = Pattern.compile("([0-9]+)-([0-9]+)-([0-9]+)-([0-9]+)-(kb228081.kb.dk.warc)");    

    //Example kb-pligtsystem-36861-20121018210245-00000.warc
    private static final Pattern arc_kb_pligt_Pattern = Pattern.compile("(kb-pligtsystem)-([0-9]+)-([0-9]+)-([0-9]{1,5}).(kb228081.kb.dk.warc|warc)" );

    //Example 1298-metadata-2.arc
    private static final Pattern arc_metadata_Pattern = Pattern.compile("([0-9]+)-(metadata)-([0-9]+).(arc|warc)" );                      

    public static void main(String[] args) {
        //System.out.println(parseSbArcFile("25666-33-20080221003533-00046-sb-prod-har-004.arc"));
        // System.out.println(parseKb1ArcFile("15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc"));
        //System.out.println(parseKb2ArcFile("193305-197-20131111175547-00001-kb228081.kb.dk.warc"));
        //System.out.println(parseKbPligtArcFile("kb-pligtsystem-36861-20121018210245-00000.warc"));
        //System.exit(1);
    }


    public static ArcMetaData parseArcFileName(String fileName){
        if (isMetaDataArcFile(fileName)) {                      
            return parseMetaDataArcFile(fileName);
        } else if (isSbArcFile(fileName)) {      
            return parseSbArcFile(fileName);
        }
        else if (isKb1ArcFile(fileName)) {      
            return parseKb1ArcFile(fileName);
        }                       
        else if (isKb2ArcFile(fileName)) {      
            return parseKb2ArcFile(fileName);
        }        
        else if (isKbPligtArcFile(fileName)) {      
            return parseKbPligtArcFile(fileName);             
        } else {
            ArcMetaData meta = new ArcMetaData();
            meta.setType(ARC_TYPE.UNKNOWN);
            return meta;            
        }            
    }

    public static boolean isMetaDataArcFile(String fileName) {
        return arc_metadata_Pattern.matcher(fileName).matches();
    }

    public static boolean isSbArcFile(String fileName) {
        return arc_sb_Pattern.matcher(fileName).matches();
    }

    public static boolean isKb1ArcFile(String fileName) {
        return arc_kb1_Pattern.matcher(fileName).matches();
    }

    public static boolean isKb2ArcFile(String fileName) {
        return arc_kb2_Pattern.matcher(fileName).matches();
    }


    public static boolean isKbPligtArcFile(String fileName) {
        return arc_kb_pligt_Pattern.matcher(fileName).matches();
    }

    public static ArcMetaData parseKbPligtArcFile(String fileName) {
        ArcMetaData meta = new ArcMetaData();
        Matcher matcher =  arc_kb_pligt_Pattern.matcher(fileName);
        matcher.matches();
        String group1 = matcher.group(0); // whole string
        String group2 = matcher.group(1); 
        String group3 = matcher.group(2); 
        String group4 = matcher.group(3); // date
        String group5 = matcher.group(4); // 
        meta.setType(ARC_TYPE.KB);
        meta.setHarvestTimeIsoDate(convertArcDateFormatToIsoDate(group4));
        /*
         * System.out.println(group1); System.out.println(group2); System.out.println(group3); System.out.println(group4); System.out.println(group5);
         * System.out.println(group6);
         */
        return meta;
    }


    public static ArcMetaData parseMetaDataArcFile(String fileName) {
        ArcMetaData meta = new ArcMetaData();

        Matcher matcher = arc_metadata_Pattern.matcher(fileName);
        matcher.matches();
        String group1 = matcher.group(0); // whole string
        String group2 = matcher.group(1); // id


        meta.setType(ARC_TYPE.METADATA);
        return meta;


    }

    public static ArcMetaData parseSbArcFile(String fileName)  {
        ArcMetaData meta = new ArcMetaData();
        Matcher matcher = arc_sb_Pattern.matcher(fileName);
        matcher.matches();
        String group1 = matcher.group(0); 
        String group2 = matcher.group(1);
        String group3 = matcher.group(2);
        String group4 = matcher.group(3); //date
        String group5 = matcher.group(4);
        String group6 = matcher.group(5);

        meta.setType(ARC_TYPE.SB);
        meta.setHarvestTimeIsoDate(convertArcDateFormatToIsoDate(group4));

        /*
         * System.out.println(group1); System.out.println(group2); System.out.println(group3); System.out.println(group4); System.out.println(group5);
         * System.out.println(group6);
         */
        return meta;
    }

    public static ArcMetaData parseKb1ArcFile(String fileName) {
        ArcMetaData meta = new ArcMetaData();
        Matcher matcher = arc_kb1_Pattern.matcher(fileName);
        matcher.matches();
        String group1 = matcher.group(0); // whole string
        String group2 = matcher.group(1); // 
        String group3 = matcher.group(2); // 
        String group4 = matcher.group(3); // date
        String group5 = matcher.group(4); // 
        String group6 = matcher.group(5); // 
        meta.setHarvestTimeIsoDate(convertArcDateFormatToIsoDate(group4));

        meta.setType(ARC_TYPE.KB);
        return meta;
    }

    public static ArcMetaData parseKb2ArcFile(String fileName) {
        ArcMetaData meta = new ArcMetaData();
        Matcher matcher = arc_kb2_Pattern.matcher(fileName);
        matcher.matches();
        String group1 = matcher.group(0); // whole string
        String group2 = matcher.group(1); // 
        String group3 = matcher.group(2); // 
        String group4 = matcher.group(3); // date
        String group5 = matcher.group(4); // 
        String group6 = matcher.group(5); // 
        meta.setHarvestTimeIsoDate(convertArcDateFormatToIsoDate(group4));

        meta.setType(ARC_TYPE.KB);
        return meta;
    }

    private static synchronized String convertArcDateFormatToIsoDate(String arcDateFormatStr) {
        try{            
            Date d= arcDateFormat.parse(arcDateFormatStr);
            return  isoDateFormat.format(d);        
        }
        catch(Exception e){
            System.out.println("parseData failed:"+arcDateFormatStr);
            return null;
        }        
    }

    public static class ArcMetaData{

        public static enum ARC_TYPE {
            KB,SB,METADATA,UNKNOWN
        }

        private ARC_TYPE type;
        private String harvestTimeIsoDate;

        public ARC_TYPE getType() {
            return type;
        }
        public void setType(ARC_TYPE type) {
            this.type = type;
        }
        public String getHarvestTimeIsoDate() {
            return harvestTimeIsoDate;
        }
        public void setHarvestTimeIsoDate(String harvestTimeIsoDate) {
            this.harvestTimeIsoDate = harvestTimeIsoDate;
        }
        @Override
        public String toString() {
            return "ArcMetaData [type=" + type + ", harvestTimeIsoDate=" + harvestTimeIsoDate + "]";
        }
    }

}

