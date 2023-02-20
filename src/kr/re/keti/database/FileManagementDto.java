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
}
