package dk.statsbiblioteket.netarchivesuite.archon.persistence;

public class ArcVO {

    private String fileName;
    private long createdTime;
    private int priority;
    private String arcState;
    private Integer shardId;

    
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
    @Override
    public String toString() {
        return "ArcVO [fileName=" + fileName + ", createdTime=" + createdTime + ", shardId=" + shardId + ", arcState=" + arcState + ", priority=" + priority + "]";
    }
  


}

