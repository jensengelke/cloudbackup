package cloudbackup.datapipe;

import java.util.Properties;

import cloudbackup.model.LocalFile;
import cloudbackup.model.RemoteFile;

public class DataPipeContext {
	
	private LocalFile localFile;
	private RemoteFile remoteFile;
	private Properties props = new Properties();
	
	public LocalFile getLocalFile() {
		return localFile;
	}
	public void setLocalFile(LocalFile localFile) {
		this.localFile = localFile;
	}
	public RemoteFile getRemoteFile() {
		return remoteFile;
	}
	public void setRemoteFile(RemoteFile remoteFile) {
		this.remoteFile = remoteFile;
	}
	public Properties getProps() {
		return props;
	}
	public void setProps(Properties props) {
		this.props = props;
	}
	
	@Override
	public String toString() {
		return "DataPipeContext LocalFile: " + localFile.toString() + " \nRemoteFile: " + remoteFile.toString() + "\nProps: " + props.toString();
	}
	
}
