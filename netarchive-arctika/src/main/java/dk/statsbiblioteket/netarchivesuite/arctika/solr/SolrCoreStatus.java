package dk.statsbiblioteket.netarchivesuite.arctika.solr;

public class SolrCoreStatus {

    private String coreName;
    private long indexSizeBytes;
    private String indexSizeHumanReadable;
    private int segmentCount;
    private int numDocs;
        
    public long getIndexSizeBytes() {
        return indexSizeBytes;
    }
    
    public void setIndexSizeBytes(long indexSizeBytes) {
        this.indexSizeBytes = indexSizeBytes;
    }
    
    public int getSegmentCount() {
        return segmentCount;
    }
    
    public void setSegmentCount(int segmentCount) {
        this.segmentCount = segmentCount;
    }
        
    public String getIndexSizeHumanReadable() {
        return indexSizeHumanReadable;
    }
    
    public void setIndexSizeHumanReadable(String indexSizeHumanReadable) {
        this.indexSizeHumanReadable = indexSizeHumanReadable;
    }
    
    public String getCoreName() {
        return coreName;
    }
    public void setCoreName(String coreName) {
        this.coreName = coreName;
    }
    public int getNumDocs() {
        return numDocs;
    }
    public void setNumDocs(int numDocs) {
        this.numDocs = numDocs;
    }
   
    // This is so far the only way to detect if index is optimized;
    public boolean isOptimized() {
        return segmentCount <= 1;
    }

    @Override
    public String toString() {
        return "SolrCoreStatus [coreName=" + coreName + ", indexSizeBytes=" + indexSizeBytes + ", indexSizeHumanReadable=" + indexSizeHumanReadable + ", segmentCount=" + segmentCount + ", numDocs="
                + numDocs + "]";
    }           
}
