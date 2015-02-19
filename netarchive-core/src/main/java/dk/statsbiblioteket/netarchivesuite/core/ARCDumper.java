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

import java.io.*;
import java.net.URL;

/**
 * Lists the URLs from an ARC file.
 */
public class ARCDumper {

    public static void dump(String arcfile) throws IOException {
        File arc = getFile(arcfile);
        if (!arc.exists()) {
            throw new IOException("The file '" + arcfile + "' does not exist");
        }
        LineInputStream in = new LineInputStream(arc);

        String line;
        long oldOffset = 0;
        majorheader:
        while ((line = in.readLine()) != null) {
            if (!line.contains("</arcmetadata>")) {
                continue;
            }
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    break majorheader;
                }
                oldOffset = in.offset;
            }
        }
        if (line == null) {
            System.out.println("No recognized records");
            return;
        }

        while (line != null) {
            System.out.println(line + " (absolute offset: " + oldOffset + ")");
            final long delta = getDelta(line);
            if (in.skip(delta) != delta) {
                System.err.println("Could not skip " + delta + " bytes");
            }
            in.readLine();
            oldOffset = in.getOffset();
            line = in.readLine();
            //noinspection StatementWithEmptyBody
            //while ((line = in.readLine()) != null && line.isEmpty());
        }
        in.close();
    }

    public static class LineInputStream extends FileInputStream {
        private long offset = 0;
        public LineInputStream(File file) throws FileNotFoundException {
            super(file);
        }
        public String readLine() throws IOException {
            ByteArrayOutputStream by = new ByteArrayOutputStream();
            int b;
            while ((b = read()) != '\n' && b != -1) {
                by.write(b);
            }
            return by.size() == 0 && b == -1 ? null : by.toString("utf-8");
        }
        public long getOffset() {
            return offset;
        }

        @Override
        public int read() throws IOException {
            offset++;
            return super.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = super.read(b);
            offset += read;
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            offset += read;
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long read = super.skip(n);
            offset += read;
            return read;
        }
    }

    public static void main(String[] args) throws IOException {
        final String ARC = args.length == 0 ?
                "/home/te/tmp/warc/137542-153-20111129020925-00316-kb-prod-har-003.kb.dk.arc" :
                args[0];
        if (!new File(ARC).exists()) {
            System.err.println("Unable to locate ARC file " + ARC);
            return;
        }
        dump(ARC);
    }

    /// http://www.example.com/somepath 192.168.10.12 20111129020924 text/html 79022
    private static long getDelta(String line) {
        String tokens[] = line.split(" ");
        try {
            return Long.parseLong(tokens[tokens.length-1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to extract delta from line\n" + line);
        }
    }

    /**
     * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
     * @param resource a generic resource.
     * @return a File pointing to the resource.
     */
    private static File getFile(String resource) throws IOException {
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
