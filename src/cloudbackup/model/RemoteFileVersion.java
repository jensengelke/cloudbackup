package cloudbackup.model;

public class RemoteFileVersion {

	private String revisionId;
	private long timestamp;
	private String contentHash;
	public String getRevisionId() {
		return revisionId;
	}
	public void setRevisionId(String revisionId) {
		this.revisionId = revisionId;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getContentHash() {
		return contentHash;
	}
	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}
	
	public RemoteFileVersion() {}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RemoteFileVersion: ");
		sb.append(" revisionId=" + revisionId);
		sb.append(", timestamp=" + timestamp);
		sb.append(", contentHash=" + contentHash);
		return sb.toString();
	}

	
}
