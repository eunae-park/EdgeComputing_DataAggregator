package kr.re.keti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import kr.re.keti.agent.Agent;
import kr.re.keti.agent.AgentPacket;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;

public class RequestProcess extends DataProcess{
	Database database;

	public RequestProcess(Database database) {
		super(database);
		this.database = database;
	}
	// ------------------------------------------pub/sub--------------------------------------------------
	public void newEdge(String data) {
		String request = "{[{REQ::"+Main.deviceIP+"::010::"+data+"}]}";
		agent.send(request.getBytes());
	}
	// ------------------------------------------TCP--------------------------------------------------
	public void requestDeviceInformation(String address, int port) {		
		if(ipCheck(address)) {
			System.out.println("request to "+address);
			byte[] request = ("{[{REQ::"+address+"::001::DEV_STATUS}]}").getBytes();
			String response = new String(agent.send(address, request));
			
			String[] array = response.substring(8, response.indexOf("}]}")).split("::");
			String data = array[2];
			showDeviceInformation(data);
		}
		else {
			System.out.println("not found: "+address);
		}
	}
	public FileManagementDto requestMetaDataInformation(String address, int port, String dataid) {
		String request = "{[{REQ::"+address+"::003::"+dataid+"}]}";

		String response = new String(agent.send(address, request.getBytes()));
		System.out.println("requestProcess response : "+response);
		response = messageFormat(response);
		String data = response.split("::")[3];
		if(data.equals("none")) return null;
		
		String datas[] = data.split("#");
		FileManagementDto dto = new FileManagementDto();
		dto.setDataId(dataid);
		dto.setTimestamp(Timestamp.valueOf(datas[1]));
		dto.setFileType(datas[2]);
		dto.setDataType(Integer.parseInt(datas[3]));
		dto.setSecurityLevel(Integer.parseInt(datas[4]));
		dto.setDataPriority(Integer.parseInt(datas[5]));
		dto.setDataSign(datas[6]);
		dto.setCert(datas[7]);
		dto.setDirectory(datas[8]);
		dto.setLinkedEdge(datas[9]);
		dto.setDataSize(Integer.parseInt(datas[10]));
		return dto;
	}
	public void requestWholeDataInformation(String address, int port) {
		if(ipCheck(address)) {
			byte[] request = ("{[{REQ::"+address+"::002::DATA_INFO}]}").getBytes();
			String response = new String(agent.send(address, request));
			response = messageFormat(response);
			String data = response.split("::")[3];
			wholeDataInformation(data);			
		}
		else {
			System.out.println("not found: "+address);
		}
	}
	public void requestIndividualMetaDataInformation(int port, String dataid) {
		ArrayList<String> existsEdge = new ArrayList<>();
		System.out.println("request to mine");
		FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
		String fileAddress = "none";
		if(dto != null) {
			existsEdge.add(Main.deviceIP);
			fileAddress = "mine";
		}
		
		if(Main.mode.equals("slave")) {
			System.out.println("request to "+Main.masterIP);
			FileManagementDto temp = requestMetaDataInformation(Main.masterIP, port, dataid);
			if(temp != null) {
				existsEdge.add(Main.masterIP);
				if(dto ==null) {
					dto = temp;
					fileAddress = Main.masterIP;
				}
			}
		}
		
		String slaves = requestEdgeList(port);
		String[] slaveList = slaves.split(", ");
		if(!slaves.equals("none")) {
			for(String address : slaveList) {
				if(!Main.deviceIP.equals(address)) {
					System.out.println("request to "+address);
					FileManagementDto temp = requestMetaDataInformation(address, port, dataid);
					if(temp != null) {
						existsEdge.add(address);
						if(dto ==null) {
							dto = temp;
							fileAddress = address;
						}
					}
				}
			}
		}
		if(dto ==null) {
			System.out.println("* Anyone doesn't have MetaData.");
		}
		else {
			System.out.print("* [");
			for(int i=0; i<existsEdge.size();i++) {
				if(i != existsEdge.size()-1) {
					System.out.print(existsEdge.get(i)+", ");
				}
				else {
					System.out.println(existsEdge.get(i)+"] : have MetaData.");
				}
			}
			System.out.println("* Metadata information in "+fileAddress+" :");
			individualDataInformation(dto);			
		}
	}
	public boolean requestIndividualDataRemove(String address, int port, String dataid) {
		String request = "{[{REQ::"+address+"::006::"+dataid+"}]}";
		String response = new String(agent.send(address, request.getBytes()));
		if(response.indexOf("fail") != -1) {
			return false;
		}
		else {
			return true;
		}
	}
	public void requestIndividualDataRead(int port, String dataid){
		final int STANDARD_SIZE = 1;
//		final double STANDARD_SPLIT_SIZE = 4;
		
		String slaves = requestEdgeList(port);
		String[] slaveList = slaves.split(". ");
		ArrayList<String> edgeList = new ArrayList<>();
		edgeList.add(Main.masterIP);
		edgeList.addAll(Arrays.asList(slaveList));
		
		ArrayList<String> existsEdge = new ArrayList<>();
		Map<String, List<String>> edgeChunkMap = new HashMap<>();
	    Map<String, String> chunkSshMap = new HashMap<>();
		
		FileManagementDto metaData = (FileManagementDto) database.select("file_management", dataid);
		
		//-------------------------existsEdge-------------------------------------------
//		if(metaData != null) return null;
		for(String edge : edgeList) {
			if(edge.equals(Main.deviceIP)) continue;
			
			FileManagementDto dto = requestMetaDataInformation(edge, port, dataid);
			if(dto != null) {
				if(metaData == null) metaData = dto;
				existsEdge.add(edge);
			}
		}
		String fileName = dataid+"."+metaData.getFileType();
		
		int currentChunk = 1;
		int dataSize = (int) metaData.getDataSize();
		
		int remainingChunks = dataSize / STANDARD_SIZE - currentChunk + 1;
		int chunksPerEdge = remainingChunks / existsEdge.size();
		int extraChunks = remainingChunks % existsEdge.size();
		
		for (String edge : existsEdge) {
		    int edgeChunks = chunksPerEdge + (extraChunks > 0 ? 1 : 0);
		    extraChunks--;
		    List<String> edgeChunksList = new ArrayList<>();
		    for (int i = 0; i < edgeChunks; i++) {
		        edgeChunksList.add(fileName+"_" + currentChunk);
		        currentChunk++;
		    }
		    edgeChunkMap.put(edge, edgeChunksList);
		}
		
		//----------------------------chunk 401 message--------------------------------------------
		// [debug] chunk file list
//		for (Map.Entry<String, List<String>> entry : edgeChunkMap.entrySet()) {
//		    String edge = entry.getKey();
//		    List<String> chunkList = entry.getValue();
//	
//		    System.out.println("Edge: " + edge);
//		    System.out.println("Chunk List:");
//		    for (String chunk : chunkList) {
//		        System.out.println(chunk);
//		    }
//		    System.out.println();
//		}
		
		for (Map.Entry<String, List<String>> entry : edgeChunkMap.entrySet()) {
		    String edge = entry.getKey();
		    List<String> chunkList = entry.getValue();
		    
		    String chunk = chunkList.get(0);
		    String[] stringItem = chunk.split("_");
		    int start = Integer.parseInt(stringItem[stringItem.length-1]);

		    chunk = chunkList.get(chunkList.size() - 1);
		    stringItem = chunk.split("_");
		    int finish = Integer.parseInt(stringItem[stringItem.length-1]);

		    String request = "{[{REQ::"+edge+"::401::"+dataid+"::"+start+"::"+finish+"}]}";
			String response = new String(agent.send(edge, request.getBytes()));
		    response = messageFormat(response);
		    String[] responseArray = response.split("::");
		    String result = responseArray[responseArray.length-1];
		    if(!result.equals("success")) {
		    	edgeChunkMap.remove(edge);
		    	continue;
		    }
		}
		

	    System.out.println("* Edge List with Data Separation Completed : "+edgeChunkMap.keySet());
		for (Map.Entry<String, List<String>> entry : edgeChunkMap.entrySet()) {
		    String edge = entry.getKey();
		    List<String> chunkList = entry.getValue();
		    
		    String chunk = chunkList.get(0);
		    String[] stringItem = chunk.split("_");
		    int start = Integer.parseInt(stringItem[stringItem.length-1]);

		    chunk = chunkList.get(chunkList.size() - 1);
		    stringItem = chunk.split("_");
		    int finish = Integer.parseInt(stringItem[stringItem.length-1]);


		    System.out.println("Request to : "+edge+", chunk #"+start+" to #"+finish);
		}

		//----------------------------chunk 405 message--------------------------------------------
		for(Map.Entry<String, List<String>> entry : edgeChunkMap.entrySet()) {
			String edge = entry.getKey();
			List<String> chunkList = entry.getValue();
			

		    String startChunk = chunkList.get(0);
		    String[] stringItem = startChunk.split("_");
		    int start = Integer.parseInt(stringItem[stringItem.length-1]);

		    String finishChunk = chunkList.get(chunkList.size() - 1);
		    stringItem = finishChunk.split("_");
		    int finish = Integer.parseInt(stringItem[stringItem.length-1]);

		    String request = "{[{REQ::"+edge+"::405::"+dataid+"::"+start+"::"+finish+"}]}";
			String response = new String(agent.send(edge, request.getBytes()));
		    response = messageFormat(response);
		    String[] responseArray = response.split("::");
		    String[] sha = Arrays.copyOfRange(responseArray, 4, responseArray.length);
		    

		    for (int i = 0; i < chunkList.size(); i++) {
		        String chunk = chunkList.get(i);
		        String sshInfo = sha[i]; // SSH 정보는 sha 배열에서 가져옴
		        chunkSshMap.put(chunk, sshInfo);
		    }
		}
		
		// [debug] ssh data
//		for (Map.Entry<String, String> entry : chunkSshMap.entrySet()) {
//		    String chunk = entry.getKey();
//		    String sshInfo = entry.getValue();
//		    System.out.println(chunk + " -> " + sshInfo);
//		}
		//-------------------------------chunk 406 message------------------------------
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String filePath = Main.storageFolder+"chunk/";
		File chunkPath = new File(filePath);
		if(!chunkPath.exists()) chunkPath.mkdir();
		
		AtomicInteger downloadCompleteCount = new AtomicInteger();
		int chunkSize = (int) Math.ceil((double)metaData.getDataSize()/STANDARD_SIZE);
		Object lock = new Object();
		
		Thread downloadCompleteThread = new Thread(()->{
			int errorCheck = 0;
			int beforeCount = -1;
			while(true) {
				synchronized(lock) {
					try {
						lock.wait(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				int currentCount = downloadCompleteCount.get();
				if(beforeCount != currentCount && currentCount<chunkSize) {
					errorCheck = 0;
					beforeCount = currentCount;
					int percentage = (int) (((double) currentCount/chunkSize) * 100);
					System.out.println("\tReceive Rate of Chunk :"+currentCount+"/"+chunkSize+" = "+percentage+"%");
				}
				else {
					errorCheck ++;
					if(errorCheck > 10) {
						System.out.println("\tReceive Chunk fail");
						break;
					}
				}
				if(currentCount >= chunkSize) {
					System.out.println("\tReceive Rate of Chunk :"+currentCount+"/"+chunkSize+" = 100%");
					break;
				}
			}
		});
		downloadCompleteThread.setName("downloadCompleteThread");
		downloadCompleteThread.start();
		
		chunkSshMap.keySet().forEach(chunkName->{
			Thread downloadThread = new Thread(()->{
				String ssh = chunkSshMap.get(chunkName);
				AgentPacket packet = null;
				for (int i=0; i<10; i++) {
					try {
						Thread.sleep(500);
						packet = Agent.unitTable.get(chunkName);
						if(packet == null && i >8) {
							System.out.println("packet not data "+chunkName);
							return;
						}
						else if(packet == null) continue;
						Socket socket = packet.getSocket();
						String address = socket.getInetAddress().getHostAddress();
						OutputStream outputStream = socket.getOutputStream();
						byte[] data = packet.getData();
						if(ssh.equals(sha(data))) {
							try(FileOutputStream fileOutputStream = new FileOutputStream(filePath+chunkName)){
								if(ssh.equals(sha(data))) {
									fileOutputStream.write(data);
									outputStream.write(("{[{ANS::"+address+"::406::success}]}").getBytes());
									outputStream.flush();
									int currentCount = downloadCompleteCount.incrementAndGet();
									if(currentCount >= chunkSize) {
										synchronized (lock) {
											lock.notifyAll();
										}										
									}
								}
								else {
									outputStream.write(("{[{ANS::"+address+"::406::fail}]}").getBytes());
									outputStream.flush();
								}
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						else {
							continue;
						}
						
						socket.close();
						return;
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println("die "+chunkName);
			});
			downloadThread.setName(chunkName+"DownLoadThread");
			downloadThread.start();
		});
		
		try {
			downloadCompleteThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(chunkSize != downloadCompleteCount.get()) return;
		//-------------------------------chunk 444 message------------------------------
		Thread mergeThread = new Thread(()->{
			try (FileOutputStream outputStream = new FileOutputStream(Main.storageFolder+fileName);){
				for(int i=1; i<= chunkSize; i++) {
					String chunkName = fileName+"_"+i;
					FileInputStream inputStream = new FileInputStream(Main.storageFolder+"chunk/"+chunkName);
					byte[] buffer = new byte[1024];
					int length;
					while((length = inputStream.read(buffer)) >0 ) {
						outputStream.write(buffer, 0, length);
					}
					inputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		mergeThread.setName(fileName+"MergeThread");
		mergeThread.start();
		
		try {
			mergeThread.join();
			String fileSsh = sha(fileName);
			String responseSsh;
			int i=0;
			do {
				i++;
				Thread.sleep(100);
				String request = "{[{REQ::"+existsEdge.get(0)+"::444::"+fileName+"}]}";
				byte[] response = agent.send(existsEdge.get(0), request.getBytes());
				responseSsh = messageFormat(new String(response)).split("::")[3];
			}while(i < 10 && !responseSsh.equals(fileSsh));
			if(i>9) {
				System.out.println("file ssh not equals");
				return;
			}
			else {
				System.out.println("* Recieved Data SHA code is th same as original.");
				System.out.println("\tOriginal Data SHA code :\t"+responseSsh);
				System.out.println("\tRecieved Data SHA code :\t"+fileSsh);
				System.out.println("* "+existsEdge+" : have Data.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		return null;
	}
	
}
