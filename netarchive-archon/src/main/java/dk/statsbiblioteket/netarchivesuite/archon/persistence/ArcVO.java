package dk.statsbiblioteket.netarchivesuite.archon.persistence;

public class ArcVO {

    private String fileName;
    private long createdTime;
    private int priority;
    private String arcState;
    private Integer shardId;
    private long modifiedTime;
    
    public ArcVO(){
        
    }
    
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public long getCreatedTime() {
        return createdTime;
    }
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
      
    public String getArcState() {
        return arcState;
    }
    public void setArcState(String arcState) {
        this.arcState = arcState;
    }
    public Integer getShardId() {
        return shardId;
    }
    public void setShardId(Integer shardId) {
        this.shardId = shardId;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
    
    public String calculateTimeSinceLastModified(){
        return formatRunningTime(System.currentTimeMillis()-modifiedTime);
    }
    
    public static String formatRunningTime(long milliseconds){
        
        int seconds = (int) ((milliseconds / 1000) % 60);
        int minutes = (int) ((milliseconds / 1000) / 60);    
        return minutes +" minutes "+seconds+" seconds";   
    }
    
    @Override
    public String toString() {
        return "ArcVO [fileName=" + fileName + ", createdTime=" + createdTime + ", priority=" + priority + ", arcState=" + arcState + ", shardId=" + shardId + ", modifiedTime=" + modifiedTime + "]";
    }
    
    
  


}

