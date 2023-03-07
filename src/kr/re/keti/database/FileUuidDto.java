package kr.re.keti.database;

public class FileUuidDto {
	String fileName;
	String fileUuid;
	public FileUuidDto() { }
	public FileUuidDto(String fileName, String fileUuid) {
		super();
		this.fileName = fileName;
		this.fileUuid = fileUuid;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileUuid() {
		return fileUuid;
	}
	public void setFileUuid(String fileUuid) {
		this.fileUuid = fileUuid;
	}
}
