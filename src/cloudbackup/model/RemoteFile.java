package cloudbackup.model;

import java.util.ArrayList;
import java.util.List;

public class RemoteFile {

	private String fileId;
	private String relativePath;
	private boolean deleted;
	private String backupId;
	private long lastRevisionTimestamp;
	private long deletionFilestamp;
	private List<RemoteFileVersion> revisions;

	public RemoteFile() {
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public String getBackupId() {
		return backupId;
	}

	public void setBackupId(String backupId) {
		this.backupId = backupId;
	}

	public long getLastRevisionTimestamp() {
		return lastRevisionTimestamp;
	}

	public void setLastRevisionTimestamp(long lastRevisionTimestamp) {
		this.lastRevisionTimestamp = lastRevisionTimestamp;
	}

	public List<RemoteFileVersion> getRevisions() {
		if (null == revisions) {
			revisions = new ArrayList<RemoteFileVersion>();
		}
		return revisions;
	}

	public void setRevisions(List<RemoteFileVersion> revisions) {
		this.revisions = revisions;
	}

	public long getDeletionTimestamp() {
		return deletionFilestamp;
	}

	public void setDeletionTimestamp(long deletionFilestamp) {
		this.deletionFilestamp = deletionFilestamp;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RemoteFile: ");
		sb.append(" fileId=" + fileId);
		sb.append(", deleted=" + deleted);
		sb.append(", backupId=" + backupId);
		sb.append(", lastRevisionTimestamp=" + lastRevisionTimestamp);
		sb.append(", deletionFilestamp=" + deletionFilestamp);
		sb.append(", relativePath=" + relativePath);
		sb.append(", revisions=" + revisions);
		return sb.toString();
	}

}
