package cloudbackup.model;

public class LocalFileVersion {
	
	private long fileSize;
	private long modifiedTimestamp;
	private String contentHash;
	
	public LocalFileVersion(){
		
	}
	
	public LocalFileVersion(long fileSize, long modifiedTimestamp, String contentHash) {
		this.contentHash = contentHash;
		this.modifiedTimestamp = modifiedTimestamp;
		this.fileSize = fileSize;
	}
	
	
	
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public void setModifiedTimestamp(long modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp;
	}

	public LocalFileVersion(long fileSize, long modifiedTimestamp) {
		this.modifiedTimestamp = modifiedTimestamp;
		this.fileSize = fileSize;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public long getModifiedTimestamp() {
		return modifiedTimestamp;
	}
	public String getContentHash() {
		return contentHash;
	}
	
	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}

	@Override
	public String toString() {
		
		StringBuffer sb = new StringBuffer("LocalFileVersion");
		sb.append(" fileSize=" + fileSize);
		sb.append(", modifiedTimestamp=" + modifiedTimestamp);
		sb.append(", contentHash=" +contentHash);
		
		return sb.toString();
	}
}
