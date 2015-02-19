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
import java.util.regex.Pattern;

public class ArcFileNameParserTest extends TestCase {
    private static Log log = LogFactory.getLog(ArcFileNameParserTest.class);

    public static final String ARC_TYPE_FIELD = "arc_type";
    public static final String HARVEST_TIME_FIELD = "arc_harvesttime";

    private static DateFormat arcDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    private static DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

    // Example 25666-33-20080221003533-00046-sb-prod-har-004.arc
    private static final Pattern arc_sb_Pattern = Pattern.compile(
            "([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(sb-prod-har)-([0-9]{1,3}).(statsbiblioteket.dk.warc|statsbiblioteket.dk.arc|arc)");

    // Example 15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc
    private static final Pattern arc_kb1_Pattern = Pattern.compile("([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(kb-prod-har|kb-prod-wb)-([0-9]{1,3}).(arc|kb.dk.arc|kb.dk.warc|kb228081.kb.dk.warc)");

    //Example 193305-197-20131111175547-00001-kb228081.kb.dk.warc
    private static final Pattern arc_kb2_Pattern = Pattern.compile("([0-9]+)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]+)-(kb228081.kb.dk.warc)");

    //Example kb-pligtsystem-36861-20121018210245-00000.warc
    private static final Pattern arc_kb_pligt_Pattern = Pattern.compile("(kb-pligtsystem)-([0-9]+)-([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})-([0-9]{1,5}).(warc)" );

    //Example 1298-metadata-2.arc
    private static final Pattern arc_metadata_Pattern = Pattern.compile("([0-9]+)-(metadata)-([0-9]+).(warc|arc)" );

    private static final String SB_RULES =
            arc_sb_Pattern + "\t" + ARC_TYPE_FIELD + ":sb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb1_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb2_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_kb_pligt_Pattern + "\t" + ARC_TYPE_FIELD + ":kb" + "\t" + HARVEST_TIME_FIELD + ":$3-$4-$5T$6:$7:$8.000Z\n" +
            arc_metadata_Pattern + "\t" + ARC_TYPE_FIELD + ":metadata\n" +
            "^.*$" + "\t" + ARC_TYPE_FIELD + ":unknown\n";

    public void testSBRules() {
        ArcFileNameParser parser = new ArcFileNameParser(SB_RULES);
        for (String test[]: new String[][]{
                {"[arc_type:sb, arc_harvesttime:2008-02-21T00:35:33.000Z]", "25666-33-20080221003533-00046-sb-prod-har-004.arc"},
                {"[arc_type:kb, arc_harvesttime:2007-04-18T16:37:59.000Z]", "15638-38-20070418163759-00235-kb-prod-har-002.kb.dk.arc"},
                {"[arc_type:kb, arc_harvesttime:2013-11-11T17:55:47.000Z]", "193305-197-20131111175547-00001-kb228081.kb.dk.warc"},
                {"[arc_type:kb, arc_harvesttime:2012-10-18T21:02:45.000Z]", "kb-pligtsystem-36861-20121018210245-00000.warc"},
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
