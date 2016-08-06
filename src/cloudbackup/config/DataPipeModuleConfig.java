package cloudbackup.config;

public class DataPipeModuleConfig {
	private int seqNo;
	private String module;

	public DataPipeModuleConfig() {
	}

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("DataPipeModuleConfig: ");
		s.append("seqNo=" + seqNo);
		s.append(", module=" + module);
		return s.toString();
	}
}