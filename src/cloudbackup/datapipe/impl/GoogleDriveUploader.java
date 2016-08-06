package cloudbackup.datapipe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataSink;
import cloudbackup.model.LocalFile;
import cloudbackup.model.RemoteFile;
import cloudbackup.model.RemoteFileVersion;
import cloudbackup.remote.googledrive.GoogleDriveConnector;

public class GoogleDriveUploader extends DataSink {
	
	private static final String CLASSNAME = GoogleDriveUploader.class.getName();
	private static Logger log;
	private InputStream in;
	private LocalFile localFile;
	private RemoteFile remoteFile;
	private boolean undeleted=false;
	
	public GoogleDriveUploader(InputStream in) {
		log = super.initializeLogging(CLASSNAME);
		final String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { in });
		}

		this.in = in;

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}
	}

	@Override
	public void initialize(DataPipeContext context) {
		final String sourceMethod = "initialize";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { context });
		}
		
		this.localFile = context.getLocalFile();
		this.remoteFile = context.getRemoteFile();
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}

	}

	@Override
	public void contributeResultToContext(DataPipeContext context) {
		final String sourceMethod = "contributeResultToContext";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { context });
		}
		
		if (remoteFile.isDeleted()) {
			log.info("A file that was marked deleted previously is now back. Changing state in local cache.");
			remoteFile.setDeletionTimestamp(-1l);
			remoteFile.setDeleted(false);
			this.undeleted=true;
		}
		
		context.setRemoteFile(remoteFile);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}

	}

	@Override
	public void run() {
		final String sourceMethod = "run";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod);
		}
		long starttime = System.currentTimeMillis();
		try {
			
			if (null == this.remoteFile) { //no remote file yet ... this is backup
				this.remoteFile = GoogleDriveConnector.getInstance().createFile(this.localFile, in);
			} else { // if there is a remote file already, create a revision by updating the file
				RemoteFileVersion rfv = GoogleDriveConnector.getInstance().updateFile(this.localFile, this.remoteFile, this.in);
				this.remoteFile.setLastRevisionTimestamp(this.localFile.getVersions().get(0).getModifiedTimestamp());
				this.remoteFile.getRevisions().add(rfv);
			}
			
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Google Drive file Id: " + remoteFile.getFileId());
				log.finest("time elapsed: " + (System.currentTimeMillis() - starttime));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}		
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}
	}
	
	@Override
	public void processContext(DataPipeContext context) {
		final String sourceMethod = "processContext";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod);
		}
		ObjectMapper mapper = new ObjectMapper();
		try {
			GoogleDriveConnector.getInstance().addCommentToFile(this.remoteFile.getFileId(), mapper.writeValueAsString(context.getProps()));
			if (this.undeleted) {
				log.info("A file that was marked deleted previously is now back. Changing state in Google Drive.");
				GoogleDriveConnector.getInstance().markUndeleted(remoteFile);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}
	}

}
