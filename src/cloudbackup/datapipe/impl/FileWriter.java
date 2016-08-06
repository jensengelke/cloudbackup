package cloudbackup.datapipe.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataSink;
import cloudbackup.exception.CloudBackupException;
import cloudbackup.model.LocalFile;
import cloudbackup.model.LocalFileVersion;
import cloudbackup.model.RemoteFileVersion;

public class FileWriter extends DataSink {

	private static final String CLASSNAME = FileWriter.class.getName();
	private static Logger log;

	private InputStream in;

	private String fileName;
	private LocalFile localFile;

	public FileWriter(InputStream in) {
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

		try {
			this.fileName = CloudBackupBackupOptions.getInstance().getRestoreDir() + File.separator + context.getProps().get("filename");
			
			this.localFile = new LocalFile();
			this.localFile.setDeleted(false);
			this.localFile.setFileId(context.getRemoteFile().getBackupId());
			this.localFile.setRelativePath(context.getRemoteFile().getRelativePath());
			
			LocalFileVersion lfv = new LocalFileVersion();
			if (context.getRemoteFile().getRevisions() == null ||
					context.getRemoteFile().getRevisions().isEmpty()) {
				lfv.setModifiedTimestamp(context.getRemoteFile().getLastRevisionTimestamp());
			} else {
				long latestTimestampBefore = -1L;
				RemoteFileVersion relevantRevision = null;
				Iterator<RemoteFileVersion> revisonsIt = context.getRemoteFile().getRevisions().iterator();
				while (revisonsIt.hasNext()) {
					RemoteFileVersion revision = revisonsIt.next();
					if (revision.getTimestamp() < CloudBackupBackupOptions.getInstance().getRestoreTimestamp() &&
							revision.getTimestamp() > latestTimestampBefore) {
						latestTimestampBefore = revision.getTimestamp();
						relevantRevision = revision;
					}
				}
				
				if (null != relevantRevision) {
					lfv.setModifiedTimestamp(relevantRevision.getTimestamp());
				}
			}			
			this.localFile.addVersion(lfv);			

		} catch (CloudBackupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

		context.setLocalFile(this.localFile);
		try {
			Files.setLastModifiedTime(Paths.get(fileName), FileTime.fromMillis(this.localFile.getVersions().get(0).getModifiedTimestamp()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
		FileOutputStream fos = null;

		try {
			
			File fileSystemFile = new File(fileName);
			fos = FileUtils.openOutputStream(fileSystemFile);
			

			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];

			int len = 0;
			long totalBytes = 0;
			while ((len = in.read(buffer)) > -1) {
				fos.write(buffer, 0, len);
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
				if (null != fos) {
					fos.close();
				}
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
		log.info("file restored: " + context.getLocalFile());
	}

}
