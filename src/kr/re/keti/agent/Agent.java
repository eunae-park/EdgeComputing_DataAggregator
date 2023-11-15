package kr.re.keti.agent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import kr.re.keti.DataProcess;
import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.RamDiskManager;
import kr.re.keti.ResponseProcess;
import kr.re.keti.Ssl;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.os.OSProcess;
import kr.re.keti.os.UdpReceptor;
import kr.re.keti.tcp.UnitEdge;

public class Agent extends EdgeDataAggregator{
	private static final Agent instance = new Agent();
	public Database database;
	public static Hashtable<String, AgentPacket> unitTable;
	private ResponseProcess responseProcess;
	private List<Integer> logFilters;
	private List<Integer> responseFilters;
	private RamDiskManager ramDiskManager;
	private Agent() {
		logFilters = Arrays.asList(10, 405, 406, 444, 19, 20);
		responseFilters = Arrays.asList();
		setStandard(1024*1024);
	}
	
	public static Agent getInstance() {
		return instance;
	}
	public void setDatabase(Database database) {
		instance.database = database;
		responseProcess = new ResponseProcess(database);
		ramDiskManager = RamDiskManager.getInstance(Main.ramFolder, Main.storageFolder, 4 * 1024 * 1024 * 1024);
	}
	@Override
	void receive(AgentPacket packet) {
		byte[] originalData = packet.getData();
		String dataString = new String(originalData);
		System.out.println("================================================================================================");
		System.out.println(dataString);
		System.out.println("================================================================================================");
		if(!(dataString.startsWith("{[{") && dataString.endsWith("}]}"))) return;
		
		String message = DataProcess.messageFormat(dataString);
		String datas[] = message.split("::");
		String address = datas[1];
		int code = Integer.parseInt(datas[2]);
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
				
//				byte[] keyData = Base64.getDecoder().decode(data);
//				try (OutputStream out = new FileOutputStream(path)) {
//			        out.write(keyData);
//			        out.flush();
//			    } catch (IOException e) {
//			        e.printStackTrace();
//			    }
				Ssl.addKey(address, uuid, data);
				response = "{[{ANS::"+address+"::020::success}]}";
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
		
		if(address.equals(Main.deviceIP)) {
			return;
		}
		switch(code) {
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
				ramDiskManager.createFile(fileName, data);
				
//				byte[] sign = Base64.getDecoder().decode(dto.getDataSign());
				String sign = dto.getDataSign();
				String ipAddress = dto.getLinkedEdge();
				try {
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
				database.delete("file_management", dataid);
				if(dto == null) break;
				String extension = dto.getFileType();
				String fileName = dataid+"."+extension;
				ramDiskManager.deleteFile(fileName);
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
			case 1:
				String type = datas[3];
				switch(type) {
					case "EDGE_LIST": logFun("Edge List Update"); break;
					case "SLAVE_LIST": logFun("Newest Edge List"); break;
					case "DEV_STATUS": logFun("Device Information"); break;
				}
				break;
			case 2: logFun("Whole Data Information"); break;
			case 3: {
				String dataid = datas[3];
				logData("MetaData Information", dataid);
				break;
			}
			case 6: {
				String dataid = datas[3];
				logData("Data Remove", dataid);
				break;
			}
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:{
				String dataid = datas[3];
				String logMessage[] = {
						"create File",
						"Update File",
						"Delete File",
						"Create Directory",
						"Update Directory",
						"Delete Directory"
				};
				int messageIdx = requestCode - 11;
				logData(logMessage[messageIdx], dataid);
				break;
			}
			case 401:{
				String dataid = datas[3];
				int startIdx = Integer.parseInt(datas[4]);
				int finishIdx = Integer.parseInt(datas[5]);
				logChunk("Data read", dataid, startIdx, finishIdx);
				break;
				
			}
		}
		
	}
	private void logLine(String type, String address) {
		Date now = new Date(System.currentTimeMillis());
		System.out.println(" <-- "+logFormat.format(now)+"\tData Processing Request from "+address+" : "+type+" -->");
		
	}
	private void logFun(String type) {
		System.out.println("\tRequest Function : "+type);
	}
	private void logData(String type, String dataid) {
		logFun(type);
		System.out.println("\tDataID : "+dataid);
	}
	private void logChunk(String type, String dataid, int startIdx, int finishIdx) {
		logData(type, dataid);
		System.out.println("\tchunk : #"+startIdx+" to #"+finishIdx);
	}
	private boolean shouldLog(int code) {
		if(!logFilters.contains(code)) {
			return true;
		}
		return false;
	}
	private boolean shouldRespond(int code) {
		if(responseFilters.contains(code)) {
			return true;
		}
		return false;
		
	}
	private byte[] getData(byte[] originalData) {
		String message = DataProcess.messageFormat(new String(originalData));
		String[] datas = message.split("::");
		
		int fileSize = Integer.parseInt(datas[4]);
		
		int dataEndIdx = originalData.length -3;
		int dataStartIdx = dataEndIdx - fileSize;
		byte[] data = new byte[fileSize];
		System.arraycopy(originalData, dataStartIdx, data, 0, fileSize);

		return data;
	}
//	private boolean createFile(String fileName, byte[] data) {
//		String path = Main.ramFolder+fileName;
//        int bufferSize = 1024;
//        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path))) {
//            int start = 0;
//            while (start < data.length) {
//                int end = Math.min(start + bufferSize, data.length);
//                bos.write(data, start, end - start);
//                start = end;
//            }
//            return true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return true;
//	}
	@Override
	int portCategorization(String massage) {
		return PortNum.KETI_PORT;
	}
}
