package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.*;

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
public class IndexWorkerShellCallTest {

    @Test
    public void testReturnCode() {
        Matcher matcher = IndexWorkerShellCall.STATUS_CODE.matcher("0 foo");
        assertTrue("Matcher should match", matcher.matches());
        assertEquals("Result should be correct", 0, Integer.parseInt(matcher.group(1)));
    }

}