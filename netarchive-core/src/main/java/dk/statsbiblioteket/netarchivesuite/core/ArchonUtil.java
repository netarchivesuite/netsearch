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
package dk.statsbiblioteket.netarchivesuite.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

public class ArchonUtil {

    /**
     * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
     * @param resource a generic resource.
     * @return a File pointing to the resource.
     */
    public static File getFile(String resource) throws IOException {
        File directFile = new File(resource);
        if (directFile.exists()) {
            return directFile;
        }
        URL classLoader = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (classLoader == null) {
            throw new FileNotFoundException("Unable to locate '" + resource + "' as direct File or on classpath");
        }
        String fromURL = classLoader.getFile();
        if (fromURL == null || fromURL.isEmpty()) {
            throw new FileNotFoundException("Unable to convert URL '" + fromURL + "' to File");
        }
        return new File(fromURL);
    }
}
