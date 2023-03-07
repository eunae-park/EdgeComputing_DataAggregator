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
	}
}
