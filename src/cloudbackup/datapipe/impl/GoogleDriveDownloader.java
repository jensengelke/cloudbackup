package cloudbackup.datapipe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataSource;
import cloudbackup.exception.CloudBackupDataPipeSourceInitException;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.model.RemoteFile;
import cloudbackup.remote.googledrive.GoogleDriveConnector;

public class GoogleDriveDownloader extends DataSource {

	private static final String CLASSNAME = GoogleDriveDownloader.class.getName();
	private static Logger log;
	private OutputStream out;
	private RemoteFile remoteFile;
	private String revisionId;

	public GoogleDriveDownloader(OutputStream out) {
		log = super.initializeLogging(CLASSNAME);
		final String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { out });
		}

		this.out = out;

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}
	}

	@Override
	public void initialize(DataPipeContext context) throws CloudBackupDataPipeSourceInitException {
		final String sourceMethod = "initialize";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { context });
		}
		
		this.remoteFile = context.getRemoteFile();
		long restoreTimestamp = -1L;
		try {
			restoreTimestamp = CloudBackupBackupOptions.getInstance().getRestoreTimestamp();
		} catch (CloudBackupException e1) {
			log.warning("cannot parse restoreTimestamp, restoring latest.");
			e1.printStackTrace();
		}
		
		this.revisionId = GoogleDriveConnector.getInstance().getRevisionIdForTimestamp(this.remoteFile.getFileId(), restoreTimestamp);
		if (null==revisionId) {
			throw new CloudBackupDataPipeSourceInitException("no revision before configured timestamp");
		}
		
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

		InputStream in = GoogleDriveConnector.getInstance().getFileContent(this.remoteFile.getFileId(), revisionId);
		try {

			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];

			int len = 0;
			long totalBytes = 0;
			while ((len = in.read(buffer)) > -1) {
				out.write(buffer, 0, len);
				totalBytes += (long) len;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("processed bytes: " + totalBytes);
				log.finest("time elapsed: " + (System.currentTimeMillis() - starttime));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (null != in) {
					in.close();
				}
				if (null != out) {
					out.flush();
					out.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod);
		}
	}

}
