package cloudbackup.datapipe.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataSource;
import cloudbackup.exception.CloudBackupException;

public class FileReader extends DataSource {

	private static final String CLASSNAME = FileReader.class.getName();
	private static Logger log;

	private OutputStream out;

	private String fileName;

	public FileReader(OutputStream os) {
		log = super.initializeLogging(CLASSNAME);
		final String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { os });
		}

		out = os;
		

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
			fileName = CloudBackupBackupOptions.getInstance().getBackupDir() + File.separator + context.getLocalFile().getRelativePath();
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
		
		context.getProps().put("filename", context.getLocalFile().getRelativePath().replace("\\", "/"));

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
		FileInputStream fis = null;

		try {
			File backupFile = new File(fileName);
			fis = new FileInputStream(backupFile);

			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];

			int len = 0;
			long totalBytes = 0;
			while ((len = fis.read(buffer)) > -1) {
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
				if (null != fis) {
					fis.close();
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
