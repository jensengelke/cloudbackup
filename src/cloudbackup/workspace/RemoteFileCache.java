package cloudbackup.workspace;

import java.util.Map;

import cloudbackup.model.RemoteFile;

public class RemoteFileCache {

	private Map<String, RemoteFile> remoteFiles;
	
	public Map<String, RemoteFile> getRemoteFiles() {
		return remoteFiles;
	}

	public void setRemoteFiles(Map<String, RemoteFile> remoteFiles) {
		this.remoteFiles = remoteFiles;
	}

	public RemoteFileCache() {
		
	}
}
