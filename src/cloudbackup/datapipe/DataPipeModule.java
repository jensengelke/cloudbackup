package cloudbackup.datapipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cloudbackup.config.CloudBackupSystemOptions;

public abstract class DataPipeModule implements Runnable {

	protected Logger initializeLogging(String sourceClass) {

		try {
			String loogingPropertiesFile = CloudBackupSystemOptions.getInstance().getLoggingProperties();
			if (null != loogingPropertiesFile) {
				final InputStream inputStream = new FileInputStream(new File(loogingPropertiesFile));
				LogManager.getLogManager().readConfiguration(inputStream);
				return Logger.getLogger(sourceClass);
			} else {
				System.out.println("no logging ... silent mode ;-)");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public abstract void initialize(DataPipeContext context);

	public abstract void contributeResultToContext(DataPipeContext context);

	@Override
	public abstract void run();
}
