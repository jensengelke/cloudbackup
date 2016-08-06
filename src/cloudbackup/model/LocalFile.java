package cloudbackup.model;

import java.util.ArrayList;
import java.util.List;

import cloudbackup.utils.CryptoUtils;

public class LocalFile {

	private String relativePath;
	private Boolean isDeleted;
	private List<LocalFileVersion> versions;

	private boolean toStringDirty = true;
	private String toStringRepresentation = null;

	public LocalFile() {

	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public void setVersions(List<LocalFileVersion> versions) {
		this.versions = versions;
	}

	public LocalFile(String relativePath) {
		this.relativePath = relativePath;
		this.isDeleted = false;

		this.toStringDirty = true;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getFileId() {
		try {
			return CryptoUtils.hashStringToHex(getRelativePath());
		} catch (Exception e) {
			return null;
		}
	}

	public void setFileId(String fileId) {
		// ignore
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
		this.toStringDirty = true;
	}

	public void addVersion(LocalFileVersion newRevision) {
		if (null != newRevision) {
			getVersions().add(newRevision);
		}
		this.toStringDirty = true;
	}

	public List<LocalFileVersion> getVersions() {
		if (null == versions) {
			versions = new ArrayList<LocalFileVersion>();
		}
		return versions;
	}


	@Override
	public String toString() {
		if (toStringDirty) {
			StringBuffer sb = new StringBuffer("LocalFile");
			sb.append(" (" + getFileId() + ")");
			sb.append(", relativePath=" + relativePath);
			if (isDeleted) {
				sb.append(", deleted");
			} else {
				sb.append(", active");
			}
			sb.append(", number of versions: " + getVersions().size());
			toStringRepresentation = sb.toString();
			toStringDirty = false;
		}
		return toStringRepresentation;
	}

	@Override
	public boolean equals(Object obj) {
		if (null != obj) {
			if (obj instanceof LocalFile) {
				LocalFile otherFile = (LocalFile) obj;
				return (getFileId().equals(otherFile.getFileId()));
			}
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return getFileId().hashCode();
	}
}
