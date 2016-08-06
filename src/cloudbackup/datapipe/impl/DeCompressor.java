package cloudbackup.datapipe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataPipeModule;

public class DeCompressor extends DataPipeModule {
	
	private static final String CLASSNAME = DeCompressor.class.getName();
	private InputStream in;
	private OutputStream out;
	private ZipInputStream zipIn;
	private static Logger log;
	
	public DeCompressor(InputStream in, OutputStream out) {
		log = super.initializeLogging(CLASSNAME);
		final String sourceMethod = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod, new Object[] { in, out });
		}

		this.in = in;
		this.out = out;
		this.zipIn = new ZipInputStream(in);
		

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
			ZipEntry zipEntry = zipIn.getNextEntry();
			
			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];
			int len = 0;
			long totalBytes = 0;
			while ((len = zipIn.read(buffer)) > -1) {
				out.write(buffer, 0, len);
				totalBytes += (long) len;
			}
			zipIn.closeEntry();
			
			if (log.isLoggable(Level.FINEST)) {
				log.finest("processed bytes: " + totalBytes);
				log.finest("time elapsed: " + (System.currentTimeMillis() - starttime));
				log.finest("original size: " + zipEntry.getSize() + " > compressed size: " + zipEntry.getCompressedSize());
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
