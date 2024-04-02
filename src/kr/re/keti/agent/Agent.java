package kr.re.keti.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import kr.re.keti.DataProcess;
import kr.re.keti.FileHandler;
import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.RamDiskManager;
import kr.re.keti.ResponseProcess;
import kr.re.keti.Ssl;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.database.FileUuidDto;
import kr.re.keti.os.OSProcess;
import kr.re.keti.tcp.UnitEdge;
import kr.re.keti.tcp.UnitShared;

public class Agent extends EdgeDataAggregator{
	private static final Agent instance = new Agent();
	public Database database;
	public static Hashtable<String, AgentPacket> unitTable;
	private ResponseProcess responseProcess;
	private List<Integer> logFilters;
	private List<Integer> responseFilters;
	private RamDiskManager ramDiskManager;
	private Agent() {
		logFilters = Arrays.asList(-1, 10, 200, 300, 399, 400, 405, 406, 444, 19, 20);
		responseFilters = Arrays.asList();
		setStandard(1000);
	}
	
	public static Agent getInstance() {
		return instance;
	}
	public void setDatabase(Database database) {
		instance.database = database;
		responseProcess = new ResponseProcess(database);
		ramDiskManager = RamDiskManager.getInstance(Main.ramFolder, Main.storageFolder);
	}
	@Override
	void receive(AgentPacket packet) {
		byte[] originalData = packet.getData();
		String dataString = new String(originalData);
		if(!(dataString.startsWith("{[{") && dataString.endsWith("}]}"))) return;
		
		String message = DataProcess.messageFormat(dataString);
		String datas[] = message.split("::");
		String address = datas[1];
		int code = Integer.parseInt(datas[2]);
//		if(!address.equals(Main.deviceIP)) {
//			if(code==400) {
//				if(dataString.length()>500) {
//					String[] temp = dataString.split("::");
//					temp[temp.length-1] = "...}]}";
//					System.out.println(String.join("::", temp));					
//				}
//				else {
//					System.out.println(dataString);
//				}
//			}
//			else {
//				System.out.println(dataString);
//			}
//		}
		
//		if(!address.equals(Main.deviceIP) && code == 400) {
//			System.out.println("1================================================================================================");
//			System.out.println(address+"::"+datas[3]+": "+LocalTime.now().format(DateTimeFormatter.ofPattern("H:m:s.SSS")));
////		System.out.println(dataString);
//			System.out.println("2================================================================================================");
//			System.out.println();
//			
//		}
		//-------------------Accept Log--------------------
		if(shouldLog(code)) {
			System.out.println();
			log(message);			
		}
		
		//------------------process----------------------
		Socket socket = packet.getSocket();
		if(socket == null) {
			messageProcess(originalData);				
		}
		else {
			tcpProcess(packet);		
		}
		
		//-----------------Complete Log------------------
		if(shouldLog(code)) {
			logLine("complete", address);
			System.out.print(">>>");
		}
	}

	private void tcpProcess(AgentPacket packet) {
		byte[] originalData = packet.getData();
		String dataString = new String(originalData);
		String requestData = DataProcess.messageFormat(dataString);
		String[] datas = requestData.split("::");
		String address = datas[1];
		int requestCode = Integer.parseInt(datas[2]);
		String response = "none";
		switch(requestCode) {
			case 1:
				String type = datas[3];
				switch(type) {
					case "EDGE_LIST": 
						response = responseProcess.responseInitEdgeList(address); break;
					case "SLAVE_LIST": 
						response = responseProcess.responseEdgeList(address); break;
					case "DEV_STATUS":
						response = responseProcess.responseDeviceInformation(address); break;
				}
				break;
			case 2:
				response = responseProcess.responseWholeDataInformation(address); break;
			case 3: {
				String dataid = datas[3];
				if(requestCode == 3) {
					response = responseProcess.responseMetaDataInformation(address, dataid);								
				}
				break;
			}
			case 6:{
				String dataid = datas[3];
				response = responseProcess.responseIndividualDataRemove(address, dataid);
				break;
			}
			case 20:{
				String uuid = datas[3].trim();
				String data = datas[4];
				String path = Ssl.getPath();
				path = path+"keys/"+uuid+".key";
				
				Ssl.addKey(address, uuid, data);
				response = "{[{ANS::"+address+"::020::success}]}";
				break;
			}
			case 399:{
				String fileName = datas[3];
				String uuid = datas[4];
				UnitShared unit = UnitShared.getInstance(fileName, uuid);
				unit.addReadyCount();
				response = "{[{ANS::"+address+"::399::success}]}";
				break;
			}
			case 400:{
				String uuid = datas[3];
				uuid = uuid.substring(0, uuid.indexOf("_"));
				final String chunkUuid = uuid;
				UnitShared unit = UnitShared.getInstanceUuid(chunkUuid);
				unit.countAdd(address, datas[3]);
				response = "{[{ANS::"+address+"::400::"+datas[3]+"::success}]}";
				break;				
			}
			case 401:
			case 405:
				String dataid = datas[3];
				int startIdx = Integer.parseInt(datas[4]);
				int finishIdx = Integer.parseInt(datas[5]);
				if(requestCode == 401) {
					response = responseProcess.responseChunkCreate(address, dataid, startIdx, finishIdx);									
				}
				else if(requestCode == 405) {
					response = responseProcess.responseSha(address, dataid, startIdx, finishIdx);
					UnitEdge unitEdge = new UnitEdge(database, address, dataid, startIdx, finishIdx);
					unitEdge.start();
				}
				break;
			case 406:
				Socket socket = packet.getSocket();
				if(socket == null) return;
				
				String chunkName = datas[3];
				int chunkLength = Integer.parseInt(datas[4]);
				int chunkEndIdx = originalData.length - 3;
				int chunkStartIdx = chunkEndIdx - chunkLength;
				byte[] data = new byte[chunkLength];
				System.arraycopy(originalData, chunkStartIdx, data, 0, chunkLength);
				AgentPacket unitPacket = new AgentPacket(socket, data);
				unitTable.put(chunkName, unitPacket);
				break;
			case 444:
				String fileName = datas[3];
				response = responseProcess.responseSha(address, fileName);
				break;
			default:
				System.out.println("TCP ["+requestCode+"] is undefined ");
		}

		//-----------------------------------------------------------------------------
		if(packet.getSocket() != null) {
			send(packet.getSocket(), response.getBytes());
//			System.out.println("receive: "+response+" : "+LocalTime.now().format(DateTimeFormatter.ofPattern("H:m:s.SSS")));
		}
		if(shouldRespond(requestCode)) {
			send(response.getBytes());
		}
	}
	private void messageProcess(byte[] originalMessage) {
		String message = DataProcess.messageFormat(new String(originalMessage));
		String[] datas = message.split("::");
		String address = datas[1];
		int code = Integer.parseInt(datas[2]);
		
		if(address.equals(Main.deviceIP) && code != 200) {
			return;
		}
		switch(code) {
			case -1:
				break;
			case 1:
				break;
			case 10:{
				String data = datas[3];
				FileManagementDto dto = DataProcess.metaDataToDto(data);
				database.insert(dto);
				break;
			}
			case 11:{
				String dataid = datas[3];
				FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
				String extension = dto.getFileType();
				String fileName = dataid + "." +extension;
				byte[] data = getData(originalMessage);
//				ramDiskManager.createFile(fileName, data);
				ramDiskManager.download(fileName, data);
				
//				byte[] sign = Base64.getDecoder().decode(dto.getDataSign());
				String sign = dto.getDataSign();
				String ipAddress = dto.getLinkedEdge();
				try {
					Thread.sleep(100);
					if(!Ssl.verify(ipAddress, sign, Main.storageFolder+fileName)) {
						System.out.println("sign verify fail");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
			case 12:{
				String dataid = datas[3];
				FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
				dto.setDataSize(Long.parseLong(datas[4]));
				String extension = dto.getFileType();
				String fileName = dataid + "." +extension;
				byte[] data = getData(originalMessage);
				ramDiskManager.createFile(fileName, data);
				database.update(dto);
				break;
			}
			case 13:{
				String dataid = datas[3];
				FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
				if(dto == null) break;
				database.delete("file_management", dataid);
				if(dto.getLinkedEdge().equals(Main.deviceIP)) {
					dataid = ((FileUuidDto) database.executeQuery("select * from file_uuid where fileUuid='"+dataid+"';").get(0)).getFileName();
				}
				String extension = dto.getFileType();
				String fileName = dataid+"."+extension;
				ramDiskManager.remove(fileName, "");
				break;
			}
			case 14:
				break;
			case 15:
				break;
			case 16:
				break;
			case 19:{
				String uuid = datas[3];
				String data = datas[5];
				Ssl.addKey(address, uuid, data);

				
				Agent agent = Agent.getInstance();
				String keyData = Ssl.getKey(Main.deviceIP);
				agent.send(address, ("{[{REQ::"+Main.deviceIP+"::020::"+Main.uuid+"::"+keyData+"}]}").getBytes());
				for(String ip : OSProcess.edgeList) {
					if(!ip.equals(address)) {
						keyData = Ssl.getKey(ip);
						String keyUuid = Ssl.getUuid(ip);
						agent.send(address, ("{[{REQ::"+ip+"::020::"+keyUuid+"::"+keyData+"}]}").getBytes());				
					}
				}
				break;
			}
			case 200:{
				logSave(message);
				break;
			}
			case 500:{
				String data = datas[3];
				String[] logData = data.split("#");
				FileHandler handler = FileHandler.getInstance();
				if(handler.isExists(logData[1])) {
					handler.deleteRecord(logData[1]);
				}
				handler.addRecord(data);
				break;
			}
			case 399:{
				String fileName = datas[3];
				String uuid = datas[4];
				int chunkLength = Integer.parseInt(datas[5]);
				
				UnitShared unit = UnitShared.getInstance(fileName, uuid);
				unit.setLength(chunkLength);
				
				Agent agent = Agent.getInstance();
				byte[] requestMessage = DataProcess.messageCreate("REQ", Main.deviceIP, "399", fileName, "success");
				agent.send(address, requestMessage);
				break;
			}
			case 400:{
				String chunk = datas[3];
				String dataid = chunk.substring(0, chunk.indexOf("_"));
				byte[] data = getData(originalMessage);
				
				UnitShared unit = UnitShared.getInstanceUuid(dataid);
				unit.receive(address, chunk, data);
				
				break;				
			}
			default:
				System.out.println("message ["+code+"] is undefined ");
		}
		
	}
	private void log(String message) {
		String datas[] = message.split("::");
		String address = datas[1];
		int requestCode = Integer.parseInt(datas[2]);
		logLine("Accept",address);
		switch(requestCode) {
		}
	}
}
