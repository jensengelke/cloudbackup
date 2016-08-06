package cloudbackup;

public class FileMetaData {
	
	private String relativePath;
	private boolean deleted;
	private String contentHash;
	
	
	public FileMetaData(String relativePath, String contentHash, boolean deleted) {
		this.relativePath = relativePath;
		this.deleted = deleted;
		this.contentHash = contentHash;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("relativePath:" + relativePath);
		builder.append("; contentHash: " + contentHash);
		if (deleted) {
			builder.append("; deleted");
		} else {
			builder.append("; active");
		}
		
		return builder.toString();
	}

	public String getRelativePath() {
		return relativePath;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public String getContentHash() {
		return contentHash;
	}
	
	
}
