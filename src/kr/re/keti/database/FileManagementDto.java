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
	public FileManagementDto(String dataId, int availabilityPolicy, String cert, int dataPriority, String dataSign,
			long dataSize, int dataType, String directory, String fileType, String linkedEdge, int securityLevel,
			Timestamp timestamp) {
		super();
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
	public FileManagementDto(File file, String signature) {
		int dotIndex = file.getName().lastIndexOf(".");
		this.dataId = (dotIndex > 0) ? file.getName().substring(0, dotIndex) : file.getName();
		this.availabilityPolicy = 1;
		this.cert = Main.certFolder;
		this.dataPriority = 0;
		this.dataSign = signature;
		this.dataSize = (long) Math.ceil((double) file.length() / 1000);
	}
}
