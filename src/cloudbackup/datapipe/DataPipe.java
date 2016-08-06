package cloudbackup.datapipe;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import cloudbackup.config.DataPipeConfig;
import cloudbackup.exception.CloudBackupDataPipeSourceInitException;
import cloudbackup.exception.CloudBackupException;

public class DataPipe implements Callable<DataPipeContext> {

	private static final String CLASSNAME = DataPipe.class.getName();
	private static Logger log = Logger.getLogger(CLASSNAME);

	private DataSource dataSource;
	private DataSink dataSink;
	private Map<Integer, DataPipeModule> dataPipe;
	private DataPipeContext context = null;
	private DataPipeConfig config = null;

	public DataPipe(DataPipeConfig config, DataPipeContext context) throws CloudBackupException {
		final String methodName = "constructor";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, methodName, new Object[] { config });
		}
		this.context = context;
		this.config = config;

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, methodName);
		}
	}

	public DataPipeContext getContext() {
		return this.context;
	}
	
	
	public DataPipeContext call() {
		final String sourceMethod = "run";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, sourceMethod);
		}
		
		dataPipe = new HashMap<Integer, DataPipeModule>(config.getDataPipe().length);
		try {
			PipedInputStream previousOut = new PipedInputStream(config.getBufferSize());
			PipedOutputStream outOfDataSource = new PipedOutputStream(previousOut);

			dataSource = createDataSourceObject(config.getDataSource(), outOfDataSource);
			dataSource.initialize(context);

			for (int i = 0; i < config.getDataPipe().length; i++) {
				PipedInputStream in = new PipedInputStream(config.getBufferSize());
				PipedOutputStream out = new PipedOutputStream(in);

				DataPipeModule module = createDataPipeModuleObject(config.getDataPipe()[i].getModule(), previousOut, out);
				module.initialize(context);
				dataPipe.put(Integer.valueOf(i), module);
				previousOut = in;
			}

			dataSink = createDataSinkObject(config.getDataSink(), previousOut);
			dataSink.initialize(context);

		} catch (CloudBackupDataPipeSourceInitException e) {
			log.warning("failed to initialize DataPipe: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
//			throw new CloudBackupException("Error creating DataPipe: " + e.getMessage());

		}

		Thread dataSourceThread = new Thread(dataSource);
		dataSourceThread.start();

		Thread[] dataPipeModuleThreads = new Thread[dataPipe.keySet().size()];
		for (int i = 0; i < dataPipe.keySet().size(); i++) {
			dataPipeModuleThreads[i] = new Thread(dataPipe.get(Integer.valueOf(i)));
			dataPipeModuleThreads[i].start();
		}

		Thread dataSinkThread = new Thread(dataSink);
		dataSinkThread.start();
		try {
			dataSinkThread.join();
			dataSource.contributeResultToContext(context);
			for (int i = 0; i < dataPipe.keySet().size(); i++) {
				dataPipe.get(Integer.valueOf(i)).contributeResultToContext(context);
			}
			dataSink.contributeResultToContext(context);
			dataSink.processContext(context);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, sourceMethod, context);
		}
		return context;
	}

	private DataSource createDataSourceObject(String className, OutputStream out) throws Exception {
		final String methodName = "createDataSourceObject";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, methodName, new Object[] { className, out });
		}

		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor(OutputStream.class);
		DataSource source = (DataSource) constructor.newInstance(out);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, methodName, source);
		}
		return source;
	}

	private DataSink createDataSinkObject(String className, InputStream in) throws Exception {
		final String methodName = "createDataSinkObject";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, methodName, new Object[] { className, in });
		}

		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor(InputStream.class);
		DataSink sink = (DataSink) constructor.newInstance(in);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, methodName, sink);
		}
		return sink;
	}

	private DataPipeModule createDataPipeModuleObject(String className, InputStream in, OutputStream out) throws Exception {
		final String methodName = "createDataPipeModuleObject";
		if (log.isLoggable(Level.FINE)) {
			log.entering(CLASSNAME, methodName, new Object[] { className, in, out });
		}

		Class<?> clazz = Class.forName(className);
		Constructor<?> constructor = clazz.getConstructor(InputStream.class, OutputStream.class);
		DataPipeModule module = (DataPipeModule) constructor.newInstance(in, out);

		if (log.isLoggable(Level.FINE)) {
			log.exiting(CLASSNAME, methodName, module);
		}
		return module;
	}
}
