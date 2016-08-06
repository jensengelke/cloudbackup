package cloudbackup.datapipe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.config.CloudBackupSystemOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataPipeModule;
import cloudbackup.utils.CryptoUtils;

public class ContentHashCreator extends DataPipeModule {

	private static final String CLASSNAME = ContentHashCreator.class.getName();
	private InputStream in;
	private OutputStream out;
	private static Logger log;
	String contentHash;
	
	
	
	public ContentHashCreator(InputStream in, OutputStream out) {
		log = super.initializeLogging(CLASSNAME);
		final String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { in, out });
		}

		this.in = in;
		this.out = out;
		

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
		
		context.getProps().put("contentHash", contentHash);

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
			String algorithm = CloudBackupSystemOptions.getInstance().getHashAlgorithm();
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			
			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];
			int len = 0;
			long totalBytes = 0;
			while ((len = in.read(buffer)) > -1) {
				out.write(buffer, 0, len);
				digest.update(buffer, 0, len);
				totalBytes += (long) len;
			}
			contentHash = CryptoUtils.convertByteArrayToHexString(digest.digest());
			if (log.isLoggable(Level.FINEST)) {
				log.finest("processed bytes: " + totalBytes);
				log.finest("time elapsed: " + (System.currentTimeMillis() - starttime));
				log.finest("contentHash: " + contentHash);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
				out.flush();
				out.close();
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
