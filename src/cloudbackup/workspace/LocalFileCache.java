package cloudbackup.workspace;

import java.util.Map;

import cloudbackup.model.LocalFile;

public class LocalFileCache {

	private Map<String, LocalFile> localFiles;
	
	public Map<String, LocalFile> getLocalFiles() {
		return localFiles;
	}

	public void setLocalFiles(Map<String, LocalFile> localFiles) {
		this.localFiles = localFiles;
	}

	public LocalFileCache() {
		
	}
}
