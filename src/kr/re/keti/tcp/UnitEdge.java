package kr.re.keti.tcp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;

public class UnitEdge{
	private String address;
    private List<String> chunkList;

    public UnitEdge(Database database, String address, String dataid, int startIdx, int finishIdx) {
    	chunkList = new ArrayList<>();
    	FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
    	this.address = address;
    	String fileName = dataid+"."+dto.getFileType();
    	for(int i=startIdx; i<=finishIdx;i++) {
    		chunkList.add(fileName+"_"+i);
	}
}

    public void start() {
        for (String fileName : chunkList) {
            Thread thread = new Thread(new UnitEdgeRunnable(fileName));
            thread.setName(fileName+"UnitEdgeThread");
            thread.start();
}
}

    private class UnitEdgeRunnable implements Runnable {
        private String fileName;

        public UnitEdgeRunnable(String fileName) {
            this.fileName = fileName;
}

        @Override
        public void run() {
        	while(true) {
        		try {
					Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
		}
        		try (Socket socket = new Socket(address, PortNum.KETI_PORT);){
        			socket.setSoTimeout(3000);
        			
        			String path = Main.storageFolder+"chunk/";
        			String filePath = path+fileName;
        			
        			File file = new File(filePath);
        			byte[] data = Files.readAllBytes(Paths.get(file.toURI()));
        			
        			byte[] request = message(address, fileName, data);
        			OutputStream outputStream = socket.getOutputStream();
        			outputStream.write(request);
        			outputStream.flush();
        			
        			//---------------------------response----------------------------------
        			InputStream inputStream = socket.getInputStream();
        			byte[] buffer = new byte[1024];
        			int length = inputStream.read(buffer);
        			String response = new String(buffer, 0, length); 
        			if(response.indexOf("fail") !=-1 ) {
        				continue;
			}
        			else if(response.indexOf("success") != -1) {        				
        				socket.close();
//        				System.out.println("File transfer completed for: " + filePath);
        				return;
							}
        			// Close the connections
//        			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
	    public static byte[] message(String address, String fileName, byte[] data) throws IOException {
			int fileSize = data.length;
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    baos.write(("{[{REQ::" + address + "::406::" + fileName + "::" + fileSize + "::").getBytes("UTF-8"));
		    baos.write(data);
		    baos.write("}]}".getBytes("UTF-8"));
	}
}
}
