package cloudbackup.config;

public class DataPipeConfig {
	private String dataSource;
	private String dataSink;
	private DataPipeModuleConfig[] dataPipe;
	private int bufferSize;

	public DataPipeConfig() {
	}

	public String getDataSource() {
		return dataSource;
	}

	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public String getDataSink() {
		return dataSink;
	}

	public void setDataSink(String dataSink) {
		this.dataSink = dataSink;
	}

	public DataPipeModuleConfig[] getDataPipe() {
		return dataPipe;
	}

	public void setDataPipe(DataPipeModuleConfig[] dataPipe) {
		this.dataPipe = dataPipe;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("DataPipeConfig: ");
		s.append("dataSource=" + dataSource);
		s.append(", dataPipe=[");
		if (null != dataPipe && dataPipe.length>0) {
			int module = 0;
			do {
				s.append("\n > " + dataPipe[module].getSeqNo() + ": " + dataPipe[module].getModule());
			} while (++module < dataPipe.length);	
		}
		s.append("]");
		s.append(", dataSink=" + dataSink);
		s.append(", bufferSize=" + bufferSize);
		
		return s.toString();
	}
}