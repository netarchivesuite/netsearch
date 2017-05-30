/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArcFileNameParserTest extends TestCase {
    private static Log log = LogFactory.getLog(ArcFileNameParserTest.class);

    public static final String ARC_TYPE_FIELD = "arc_type";
    public static final String HARVEST_TIME_FIELD = "arc_harvesttime";

    private static DateFormat arcDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    private static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

    // Example 25666-33-20080221003533-00046-sb-prod-har-004.arc
    private static final Pattern arc_sb_Pattern = Pattern.compile(
            "(?:.*[^\\d])?([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(sb-prod-har)-([0-9]{1,3}).(statsbiblioteket.dk.warc.gz|statsbiblioteket.dk.warc|statsbiblioteket.dk.arc.gz|statsbiblioteket.dk.arc|arc.gz|arc)");
    // Example 25666-33-20080221003533123-00046-sb-prod-har-004.arc
    private static final Pattern arc_sb_ms_Pattern = Pattern.compile(
            "(?:.*[^\\d])?((\\d+)-(\\d+)-(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})-(\\d+)-(sb-prod-har)-(\\d{1,3}).(statsbiblioteket.dk.warc.gz|statsbiblioteket.dk.warc|statsbiblioteket.dk.arc.gz|statsbiblioteket.dk.arc|arc.gz|arc))");

    // Example 15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc
    private static final Pattern arc_kb1_Pattern = Pattern.compile(
            "(?:.*[^\\d])?([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(kb-prod-har|kb-prod-wb)-([0-9]{1,3}).(kb.dk.arc.gz|kb.dk.arc|kb.dk.warc.gz|kb.dk.warc|kb228081.kb.dk.warc.gz|kb228081.kb.dk.warc|arc.gz|arc)");
    private static final Pattern arc_kb1_ms_Pattern = Pattern.compile(
            "(?:.*[^\\d])?((\\d+)-(\\d+)-(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})-(\\d+)-(kb-prod-har|kb-prod-wb)-(\\d{1,3}).(kb.dk.arc.gz|kb.dk.arc|kb.dk.warc.gz|kb.dk.warc|kb228081.kb.dk.warc.gz|kb228081.kb.dk.warc|arc.gz|arc))");

    //Example 193305-197-20131111175547-00001-kb228081.kb.dk.warc
    private static final Pattern arc_kb2_Pattern = Pattern.compile(
            "(?:.*[^\\d])?([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(kb228081.kb.dk.warc.gz|kb228081.kb.dk.warc)");
    private static final Pattern arc_kb2_ms_Pattern = Pattern.compile(
            "(?:.*[^\\d])?((\\d+)-(\\d+)-(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})-(\\d+)-(kb228081.kb.dk.warc.gz|kb228081.kb.dk.warc))");

    //Example kb-pligtsystem-36861-20121018210245-00000.warc
    private static final Pattern arc_kb_pligt_Pattern = Pattern.compile(
            "(?:.*[^\\d])?(kb-pligtsystem)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]{1,5}).(warc.gz|warc)" );
    private static final Pattern arc_kb_pligt_ms_Pattern = Pattern.compile(
            "(?:.*[^\\d])?((kb-pligtsystem)-(\\d+)-(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})-(\\d{1,5}).(warc.gz|warc))");
    
    //Example 1298-metadata-2.arc
    private static final Pattern arc_metadata_Pattern = Pattern.compile(
            "(?:.*[^\\d])?([0-9]+)-(metadata)-([0-9]+).(warc.gz|warc|arc.gz|arc)" );

    private static final Pattern arc_archiveit_Pattern = Pattern.compile(
            "(?:.*)(ARCHIVEIT-(\\d+)-[A-Z_]+-JOB(\\d+)-(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{3})-(\\d+).(arc.gz|arc|warc.gz|warc))");
    private static final String SAMPLE_ARCHIVEIT = "ARCHIVEIT-4897-ONE_TIME-JOB270764-20170303033836937-00000.warc.gz";

    private static final String SB_RULES =
            arc_sb_Pattern + "\t" + ARC_TYPE_FIELD + ":sb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_sb_ms_Pattern + "\t" + ARC_TYPE_FIELD + ":sb" + "\t" + HARVEST_TIME_FIELD + ":$4-$5-$6T$7:$8:$9.$10Z\n" +
            arc_kb1_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb1_ms_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$4-$5-$6T$7:$8:$9.$10Z\n" +
            arc_kb2_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb2_ms_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$4-$5-$6T$7:$8:$9.$10Z\n" +
            arc_kb_pligt_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb_pligt_ms_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$4-$5-$6T$7:$8:$9.$10Z\n" +
            arc_archiveit_Pattern + "\t" + ARC_TYPE_FIELD + ":archiveit" + "\t" + HARVEST_TIME_FIELD + ":$4-$5-$6T$7:$8:$9.$10Z\n" +
            arc_metadata_Pattern + "\t" + ARC_TYPE_FIELD + ":metadata\n" +
            "^.*$" + "\t" + ARC_TYPE_FIELD + ":unknown\n";

    public void testSpecific() {
        // (?:.*[^\\d])?([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(sb-prod-har)-([0-9]{1,3}).(statsbiblioteket.dk.warc.gz|statsbiblioteket.dk.warc|statsbiblioteket.dk.arc.gz|statsbiblioteket.dk.arc|arc.gz|arc)
        String path = "/netarkiv/0212/filedir/271327-254-20170222182130113-00001-sb-prod-har-003.statsbiblioteket.dk.warc.gz";
        String match = getMatch(path);
        assertNotNull("There should be a match for '" + path + "'", match);
        log.info("Match: " + match);
    }

    // Iterates known patterns. The first match is returned in the form of output $1 $2 $3...
    // If no pattern matches, null is returned
    private String getMatch(String path) {
        List<Pattern> patterns = Arrays.asList(
                arc_sb_Pattern, arc_sb_ms_Pattern,
                arc_kb1_Pattern, arc_kb1_ms_Pattern,
                arc_kb2_Pattern, arc_kb2_ms_Pattern,
                arc_kb_pligt_Pattern, arc_kb_pligt_ms_Pattern,
                arc_metadata_Pattern, arc_archiveit_Pattern);
        for (Pattern pattern: patterns) {
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0 ; i < matcher.groupCount() ; i++) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(matcher.group(i));
                }
                return sb.toString();
            }
        }
        return null;
    }

    public void testArchiveITMatch() {
        assertTrue("The ArchiveIT pattern should match the sample",
                   arc_archiveit_Pattern.matcher(SAMPLE_ARCHIVEIT).matches());
    }

    public void testSBMSMatch() {
        final String SAMPLE_SB_MS = "25666-33-20080221003533123-00046-sb-prod-har-004.arc";
        assertTrue("The 'sb' pattern should match the sample",
                   arc_sb_ms_Pattern.matcher(SAMPLE_SB_MS).matches());
        String result = getMatch(SAMPLE_SB_MS);
        System.out.println(result);
    }

    public void testSBRules() {
        ArcFileNameParser parser = new ArcFileNameParser(SB_RULES);
        for (String test[]: new String[][]{
                {"[arc_type:sb, arc_harvesttime:2008-02-21T00:35:33.000Z]", "25666-33-20080221003533-00046-sb-prod-har-004.arc"},
                {"[arc_type:sb, arc_harvesttime:2008-02-21T00:35:33.123Z]", "25666-33-20080221003533123-00046-sb-prod-har-004.arc"},
                {"[arc_type:sb, arc_harvesttime:2008-02-21T00:35:33.000Z]", "25666-33-20080221003533-00046-sb-prod-har-004.arc.gz"},
                {"[arc_type:kb, arc_harvesttime:2007-04-18T16:37:59.000Z]", "15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc"},
                {"[arc_type:kb, arc_harvesttime:2007-04-18T16:37:59.000Z]", "15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc.gz"},
                {"[arc_type:kb, arc_harvesttime:2007-04-18T16:37:59.123Z]", "15638-38-20070418163759123-00235-kb-prod-har-002.kb.dk.arc.gz"},
                {"[arc_type:kb, arc_harvesttime:2013-11-11T17:55:47.000Z]", "193305-197-20131111175547-00001-kb228081.kb.dk.warc"},
                {"[arc_type:kb, arc_harvesttime:2013-11-11T17:55:47.000Z]", "193305-197-20131111175547-00001-kb228081.kb.dk.warc.gz"},
                {"[arc_type:kb, arc_harvesttime:2013-11-11T17:55:47.321Z]", "193305-197-20131111175547321-00001-kb228081.kb.dk.warc.gz"},
                {"[arc_type:kb, arc_harvesttime:2012-10-18T21:02:45.000Z]", "kb-pligtsystem-36861-20121018210245-00000.warc"},
                {"[arc_type:kb, arc_harvesttime:2012-10-18T21:02:45.000Z]", "kb-pligtsystem-36861-20121018210245-00000.warc.gz"},
                {"[arc_type:archiveit, arc_harvesttime:2017-03-03T03:38:36.937Z]", SAMPLE_ARCHIVEIT}, // Always gz
                {"[arc_type:metadata]", "1298-metadata-2.arc"},
                {"[arc_type:unknown]", "ksjvksjfvsk"}
        }) {
            assertEquals("Input " + test[1], test[0], parser.expandFilename(test[1]).toString());
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
