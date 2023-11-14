package kr.re.keti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.os.OSProcess;

public class ResponseProcess extends DataProcess{
	Database database;

	public ResponseProcess(Database database) {
		super(database);
		this.database = database;
	}
	

	public String responseInitEdgeList(String address) {
		String response = "{[{ANS::"+address+"::001::success}]}";
		return response;
	}
	public String responseEdgeList(String address) {
		String response = "{[{ANS::"+address+"::001::"+OSProcess.getEdgeListAsString()+"}]}";
		return response;
	}
	public String responseDeviceInformation(String address) {
		String response = "{[{ANS::"+address+"::001::"+deviceInformation()+"}]}";
		return response;
	}
	public String responseWholeDataInformation(String address) {
		String response = "{[{ANS::"+address+"::002::"+wholeDataInformation()+"}]}";
		System.out.println("response : "+response);
		return response;
	}
	public String responseMetaDataInformation(String address, String dataid) {
		String response = "{[{ANS::"+address+"::003::";
		
		FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
		if(dto != null) {
			String metaData = dto.getDataId()+"#"+dto.getTimestamp()+"#"+dto.getFileType()
			+"#"+dto.getSecurityLevel()+"#"+dto.getDataPriority()+"#"+dto.getAvailabilityPolicy()
			+"#"+dto.getDataSign()+"#"+dto.getCert()+"#"+dto.getDirectory()
			+"#"+dto.getLinkedEdge()+"#"+dto.getDataSize();
			response += metaData+"}]}";			
		}
		else {
			response += "none}]}";
		}
		return response;
	}
	public String responseIndividualDataRemove(String address, String dataid) {
		String response = "{[{ANS::"+address+"::006::";
		boolean check = database.delete("file_management", dataid);
		if(check) {
			response += "permission}]}";
		}
		else {
			response += "fail}]}";
		}
		return response;
	}
	public String responseChunkCreate(String address, String dataid, int startIdx, int finishIdx) {
	    final int CHUNK_SIZE = 1000;
	    FileManagementDto dto =(FileManagementDto) database.select("file_management", dataid);
	    String fileName = dataid + "." + dto.getFileType();
	    String filePath = Main.storageFolder + fileName;
	    String response = "{[{ANS::" + address + "::401::fail}]}";

	    try (InputStream inputStream = new FileInputStream(filePath)) {
	        File directory = new File(Main.storageFolder + "chunk/");
	        if (!directory.exists()) directory.mkdirs();

	        byte[] buffer = new byte[CHUNK_SIZE];
	        int chunkCount = 1; 
	        int bytesRead;
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            if (chunkCount >= startIdx && chunkCount <= finishIdx) { // 청크 개수로 범위 비교
	                String chunkFileName = fileName + "_" + chunkCount;
	                String chunkFilePath = directory.getPath() + File.separator + chunkFileName;
	                try (OutputStream outputStream = new FileOutputStream(chunkFilePath)) {
	                    outputStream.write(buffer, 0, bytesRead);
	                }
	            }
	            chunkCount++;

	            if (chunkCount > finishIdx) break;
	        }

	        response = "{[{ANS::" + address + "::401::success}]}";
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    return response;
	}



	public String responseSha(String address, String dataid) {
		String shaCode = sha(dataid);
		String response = "{[{ANS::"+address+"::444::"+shaCode+"}]}";
		return response;
	}
	public String responseSha(String address, String dataid, int startIdx, int finishIdx) {
		String response = "{[{ANS::"+address+"::405::sha::";
		FileManagementDto dto =(FileManagementDto) database.select("file_management", dataid);
		String fileName = dataid+"."+dto.getFileType();
		for(int i=startIdx; i<=finishIdx;i++) {
			String chunkFileName = "chunk/"+fileName+"_"+i;
			String shaCode = sha(chunkFileName);
			response += (shaCode+"::");
		}
		response+="}]}";
		System.out.println(response);
		return response;
	}
	

}
