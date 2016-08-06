package cloudbackup.datapipe.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.api.client.util.Base64;

import cloudbackup.config.CloudBackupBackupOptions;
import cloudbackup.config.CloudBackupSystemOptions;
import cloudbackup.datapipe.DataPipeContext;
import cloudbackup.datapipe.DataPipeModule;
import cloudbackup.exception.CloudBackupException;

public class Decryptor extends DataPipeModule {

	private static final String CLASSNAME = Compressor.class.getName();
	private InputStream in;
	private OutputStream out;
	private CipherOutputStream cipherOs;
	private byte[] initializationVector;
	private SecretKeySpec secretKey;
	private byte[] secretKeyBytes;
	
	
	private static Logger log;

	public Decryptor(InputStream in, OutputStream out) {
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
		
		if (null != context.getProps()) {
			Object o = context.getProps().get("Encryptor.initializationVector");
			if (null != o) {
				this.initializationVector = Base64.decodeBase64((String)o);
			}
			o = context.getProps().get("Encryptor.secretKey");
			if (null != o) {
				this.secretKeyBytes = Base64.decodeBase64((String)o);
			}
		}
		
		try {
			if (null == initializationVector) {
				throw new CloudBackupException("no iv found in file metadata");
			}
			
			if (null == secretKeyBytes) {
				throw new CloudBackupException("no encryption key found in file metadata");	
			}
			
			this.secretKey = new SecretKeySpec(this.secretKeyBytes, "AES");
			Cipher cipher = Cipher.getInstance(CloudBackupSystemOptions.getInstance().getEncryptionAlgorithm());
			IvParameterSpec ivSpec = new IvParameterSpec(this.initializationVector);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
			this.cipherOs = new CipherOutputStream(this.out, cipher);
		} catch (CloudBackupException e) {
			// TODO Auto-generated catch block
			// TODO: rethrow?
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
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
		
		context.getProps().put("Encryptor.initializationVector", Base64.encodeBase64String(initializationVector));
		context.getProps().put("Encryptor.secretKey", Base64.encodeBase64String(secretKeyBytes));

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
			
			int bufferSize = CloudBackupBackupOptions.getInstance().getBackupPipe().getBufferSize();
			byte[] buffer = new byte[bufferSize];
			int len = 0;
			long totalBytes = 0;
			while ((len = in.read(buffer)) > -1) {
				cipherOs.write(buffer, 0, len);
				totalBytes += (long) len;
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("processed bytes: " + totalBytes);
				log.finest("time elapsed: " + (System.currentTimeMillis() - starttime));
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
