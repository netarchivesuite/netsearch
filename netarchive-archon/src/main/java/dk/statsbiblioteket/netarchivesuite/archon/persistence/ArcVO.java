package dk.statsbiblioteket.netarchivesuite.archon.persistence;

public class ArcVO {

    
    private String fileId;
    private String folderName;
    private String fileName;
    private long createdTime;
    private int priority;
    private String arcState;
    private Integer shardId;
    private long modifiedTime;
    
    public ArcVO(){        
    }    
    
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArcVO other = (ArcVO) obj;
        if (fileName == null) {
            if (other.fileName != null)
                return false;
        } else if (!fileName.equals(other.fileName))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ArcVO [fileName=" + fileName + ", createdTime=" + createdTime + ", priority=" + priority + ", arcState=" + arcState + ", shardId=" + shardId + ", modifiedTime=" + modifiedTime + "]";
    }
    
    
  


}

