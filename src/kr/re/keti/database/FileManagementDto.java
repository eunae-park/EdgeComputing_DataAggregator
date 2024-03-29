package kr.re.keti.database;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import kr.re.keti.Main;

public class FileManagementDto{
	private String dataId;
	private int availabilityPolicy;
	private String cert;
	private int dataPriority;
	private String dataSign;
	private long dataSize;
	private int dataType;
	private String directory;
	private String fileType;
	private String linkedEdge;
	private int securityLevel;
	private Timestamp timestamp;

	public FileManagementDto() {
	}
	public void setting(String dataId, int availabilityPolicy, String cert, int dataPriority, String dataSign,
			long dataSize, int dataType, String directory, String fileType, String linkedEdge, int securityLevel,
			Timestamp timestamp) 
	{
		this.dataId = dataId;
		this.availabilityPolicy = availabilityPolicy;
		this.cert = cert;
		this.dataPriority = dataPriority;
		this.dataSign = dataSign;
		this.dataSize = dataSize;
		this.dataType = dataType;
		this.directory = directory;
		this.fileType = fileType;
		this.linkedEdge = linkedEdge;
		this.securityLevel = securityLevel;
		this.timestamp = timestamp;
		
	}
	public FileManagementDto(String dataId, int availabilityPolicy, String cert, int dataPriority, String dataSign,
			long dataSize, int dataType, String directory, String fileType, String linkedEdge, int securityLevel,
			Timestamp timestamp) {
		setting(dataId, availabilityPolicy, cert, dataPriority, dataSign, dataSize, dataType, directory, fileType, linkedEdge, securityLevel, timestamp);
	}
	public FileManagementDto(File file, String uuid, String signature) {
		long dataSize = (long) Math.ceil((double) file.length() / 1000);
		String directory = file.getParent();
		String fileType = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
		
		setting(uuid, 1, Main.certFolder, 0, signature, dataSize, dataType, directory, fileType, null, 1, timestamp);
	}
//	public FileManagementDto(File file) {
//		this(file, "sign");
//	}
	public String toString() {
		String result = dataId+"#"+timestamp+"#"+fileType+"#"+dataType+"#"+securityLevel+"#"+dataPriority+"#"
				+availabilityPolicy+"#"+dataSign+"#"+cert+"#"+directory+"#"+linkedEdge+"#"+dataSize;
		return result;
	}
	
	public void setDataId(String dataId) {
		this.dataId = dataId;
	}
	public String getDataId() {
		return dataId;
	}

	public int getAvailabilityPolicy() {
		return availabilityPolicy;
	}

	public void setAvailabilityPolicy(int availabilityPolicy) {
		this.availabilityPolicy = availabilityPolicy;
	}

	public String getCert() {
		return cert;
	}

	public void setCert(String cert) {
		this.cert = cert;
	}

	public int getDataPriority() {
		return dataPriority;
	}

	public void setDataPriority(int dataPriority) {
		this.dataPriority = dataPriority;
	}

	public String getDataSign() {
		return dataSign;
	}

	public void setDataSign(String dataSign) {
		this.dataSign = dataSign;
	}

	public long getDataSize() {
		return dataSize;
	}

	public void setDataSize(long dataSize) {
		this.dataSize = dataSize;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getLinkedEdge() {
		return linkedEdge;
	}

	public void setLinkedEdge(String linkedEdge) {
		this.linkedEdge = linkedEdge;
	}

	public int getSecurityLevel() {
		return securityLevel;
	}

	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	
}
