package kr.re.keti;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kr.re.keti.agent.Agent;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.os.OSProcess;

public class DataProcess {
	private Database database;
	protected Agent agent = Agent.getInstance();
	public DataProcess(Database database) {
		this.database = database;
	}
	
	//-----------------------------------------REQ and ANS------------------------------------------
	public void initEdgeList(int port) {
		String edgeData = OSProcess.getEdgeListAsString();
		ArrayList<String> edgeList = OSProcess.getEdgeListAsArrayList();
		edgeData = Main.deviceIP+":"+edgeData;
		for(String clientAddress : edgeList) {
			String request = "{[{REQ::"+clientAddress+"::001::EDGE_LIST::"+edgeData+"}]}";
			agent.send(clientAddress,request.getBytes());;
		}
	}

	public String requestEdgeList(int port) {
		String slaveList = "none";
		if(Main.mode.equals("master")) {
			slaveList = OSProcess.getEdgeListAsString();
			slaveList = slaveList.replace(":", ", ");
		}
		else {
			String request = "{[{REQ::"+Main.deviceIP+"::001::SLAVE_LIST}]}";
			byte[] requestData = request.getBytes();
			byte[] responseData = agent.send(Main.masterIP, requestData);
			String response = messageFormat(new String(responseData));
			String responses[] = response.split("::");
			slaveList = responses[responses.length-1].replace(":", ", ");
		}
		
		return slaveList;
	}
	public void shareKey(String address, int port) {
		if(!Main.mode.equals("master")) {
			try {
				throw new Exception("shareKey error");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		ArrayList<String> keyList = Ssl.getKeyList();
		for(String data : keyList) {
			String[] datas = data.split(":");
			String ip = datas[0];
			String uuid = datas[1];
			String keyData = datas[2];
			
			String request = "{[{REQ::"+Main.deviceIP+"::020"+uuid+"::"+keyData+"}]}";
			byte[] requestData = request.getBytes();
			for(int i=0; i<3; i++) {
				if(ip.equals(address)) continue;
				byte[] responseData = agent.send(address, requestData);
				if(new String(responseData).indexOf("success") != -1) {
					break;
				}
				
			}
		}
	}
	
	//-----------------------------------------ANS-----------------------------------------

	//-----------------------------------------local-----------------------------------------
	public String localEdgeList() {
		String edgeList = "none";
		if(Main.mode.equals("master")) {
			String slaveList = OSProcess.getEdgeListAsString();
			edgeList = slaveList.replace(":", ", ");
		}
		return edgeList;
	}
	public void initWholeDataInformation() {
		ArrayList<Object> objectDto = database.select("file_management");
		ArrayList<FileManagementDto> dtos = new ArrayList<>();
		for(Object dto : objectDto) {
			if(dto instanceof FileManagementDto) {
				dtos.add((FileManagementDto) dto);
			}
		}
		System.out.println();
		for(int i=0; i<dtos.size(); i++) {
			System.out.println("\t#"+(i+1));
			individualDataInformation(dtos.get(i));
			System.out.println("------------------------------------------------------------------");
		}
		System.out.println("==================================================================");
	}
	public void individualDataInformation(String pk) {
		FileManagementDto dto = (FileManagementDto) database.select("file_management", pk);
		individualDataInformation(dto);
	}
//	static protected String getSlaveList() {
//		String result = "none";
//		for(String slave : UdpReceptor.edgeList) {
//			if(result.equals("none")) 
//				result = slave;
//			else
//				result += ":"+slave;
//		}
//		return result;
//	}
	public String deviceInformation() {
		String result = "none";	
		final double mb = 1024.0 * 1024.0;
		final double gb = mb * 1024.0;
		
		// 프로세서 부하량
		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		/* getSystemLoadAverage ()
		// 1보다 낮은 값을 유지하면 서버의 상태 원활하고, 1보다 높으면 서버가 부하를 받기 시작한다고 판단할 수 있다. 
		// 평균적으로 5 이상을 시스템에 과부하가 있다고 판단하지만 절대적인 판단의 기준은 절대 아니다.*/
		int processLoad=0;
		if (osBean.getSystemLoadAverage() != -1) // window 에선 -1 
			// cpu사용량 - osBean.getSystemCpuLoad() * 100;
			processLoad = (int) Math.ceil(osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100); 
		
		// 물리적 실제 메모리 
		// memory 사용량 : free -m	
		String s="";
		Process process;
		try {
			process = Runtime.getRuntime().exec("free");
	        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
	        s = br.readLine();
	        s = br.readLine();
	        process.destroy();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		while(true){
			String s_c = s.replace("  ", " ");
			if(s_c.equals(s)){
				s = s_c;
				break;				
			}
			s = s_c;
		}
		String[] array = s.split(" ");
		double memTotal = Double.parseDouble(array[1]);
		double memFree = Double.parseDouble(array[1]) - Double.parseDouble(array[2]);
		double memUsage = Math.ceil((memTotal-memFree) / memTotal * 100);		
		
		memTotal /= gb;
		
		
		// memory 사용량 : free -m				
		// 스토리지 여유량
		int storageTotal=0, storage_free=0;
		try{
			File root = new File(Main.storageFolder);
			storageTotal = (int)Math.round(root.getTotalSpace() / gb); // 우분투 = round
			storage_free = (int)Math.round(root.getUsableSpace() / gb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String cpu_id=cpuShellCmd();
		if(cpu_id.length() < 8)
			cpu_id = cpu_id + "    ";
		else if(cpu_id.length() > 8)
			cpu_id = cpu_id.substring(0, 8);
		
		int net_load = netloadShellCmd();
		
		if(memTotal > 99) memTotal = 99;
		if(storageTotal > 9999) storageTotal = 9999;
		if(storage_free > 9999) storage_free = 9999;
		if(processLoad > 99) processLoad = 99;
		if(memUsage > 99) memUsage = 99;
		
		
		result = Main.uuid + cpu_id + String.format("%02.0f", memTotal) + String.format("%04d", storageTotal) + "        " + String.format("%02d", processLoad) + String.format("%02.0f", memUsage) + String.format("%02d", net_load) + String.format("%04d", storage_free) + "  ";
		return result;
	}

	//--------------------------------------formatting----------------------------------------
	public static String messageFormat(String message) {
		return message.substring(3, message.indexOf("}]}"));
	}
	public String wholeDataInformation() {
		ArrayList<Object> objectDto = database.select("file_management");
		ArrayList<FileManagementDto> dtos = new ArrayList<>();
		for(Object dto : objectDto) {
			if(dto instanceof FileManagementDto) {
				dtos.add((FileManagementDto) dto);
			}
		}
		String data = Main.uuid;
		data+=dtos.size();
		for(FileManagementDto dto : dtos) {
			data += "#"+dto.getDataId();
			data += "#"+dto.getDataSize();
			data += "#"+dto.getSecurityLevel();
		}
		return data;
	}
	public void individualDataInformation(FileManagementDto dto) {
		System.out.println("\tDataID: "+dto.getDataId());
		System.out.println("\tTimeStamp: "+dto.getTimestamp());
		System.out.println("\tFileType: "+dto.getFileType());
		System.out.println("\tDataType: "+dto.getDataType());
		System.out.println("\tSecurityLevel: "+dto.getSecurityLevel());
		System.out.println("\tDataPriority: "+dto.getDataPriority());
		System.out.println("\tAvailabilityPolicy: "+dto.getAvailabilityPolicy());
		System.out.println("\tDataSignature: "+dto.getDataSign());
		System.out.println("\tCert: "+dto.getCert());
		System.out.println("\tDirectory: "+dto.getDirectory());
		System.out.println("\tLinked_edge: "+dto.getLinkedEdge());
		System.out.println("\tDataSize: "+dto.getDataSize());
	}
	public void showDeviceInformation(String data) {
		String uuid = data.substring(0,36);
		String deviceCPU = data.substring(36,36+8).trim();
		int deviceMemory = Integer.parseInt(data.substring(44,44+2));
		int deviceStorage =  Integer.parseInt(data.substring(46,46+4));
		int deviceLoad =  Integer.parseInt(data.substring(58,58+2));
		int deviceMemUsage =  Integer.parseInt(data.substring(60,60+2));
		int deviceNet =  Integer.parseInt(data.substring(62,62+2));
		int deviceFreeStorage =  Integer.parseInt(data.substring(64,64+4));
		
		System.out.print("\tdevice information : \n\t\tuuid=" + uuid + ",     "
				+ "cpu=" + deviceCPU + ",     "
				+ "memory=" + deviceMemory + "[GB],     "
				+ "storage=" + deviceStorage);
		System.out.println("[GB],     process load=" + deviceLoad + "[%],     "
				+ "memory usage=" + deviceMemUsage + "[%],     "
				+ "network load=" + deviceNet + "[Mbps],     "
				+ "free storage=" + deviceFreeStorage + "[GB]");
		
	}
	public void wholeDataInformation(String data) {
		String[] datas = data.split("#");
		String uuid = datas[0].substring(0,36);
		int count = Integer.parseInt(datas[0].substring(36));

		System.out.println("\tdevice uuid=" + uuid + "\n\tnumber of data=" + count);
		for(int i=0; i<count; i++)
			System.out.println(String.format("\t#%-4d", i+1) + String.format(" - DataID: %-70s", datas[i*3+1]) + String.format("\tDataSize[KB]: %-4s", datas[i*3+2]) + "\tsecurityLevel: " + datas[(i+1)*3]);
		
	}
	public static String sha(String fileName) {
		String filePath = Main.storageFolder+fileName;
		File file = new File(filePath);
        if(!file.exists())
        {
        	return "none";
        }
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			FileInputStream fis = new FileInputStream(filePath);
			byte[] dataBytes = new byte[1024];
			
			int nread = 0; 
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			};
			byte[] mdbytes = md.digest();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			fis.close();
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "none";
    }
	public static String sha(byte[] fileData) {
	    try {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        md.update(fileData);
	        byte[] mdbytes = md.digest();
	        StringBuilder sb = new StringBuilder();
	        for (int i = 0; i < mdbytes.length; i++) {
	            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        return sb.toString();
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "none";
	}
	//----------------------------------------------------------------------------------
	public boolean ipCheck(String address) {

		List<String> edgeList = new ArrayList<>();
		if(Main.mode.equals("master")) {
			String slaves = OSProcess.getEdgeListAsString();
			String[] slaveList = slaves.split(":");
			edgeList = new ArrayList<>(Arrays.asList(slaveList));
		}
		else {
			String slaves = requestEdgeList(PortNum.KETI_PORT);
			String[] slaveList = slaves.split(", ");
			edgeList = new ArrayList<>(Arrays.asList(slaveList));
			edgeList.add(Main.masterIP);
			
		}
		
		if(edgeList.contains(address)) return true;
		else return false;
	}
	private String cpuShellCmd(){
		String command = "cat /proc/cpuinfo"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		InputStream is;
		InputStreamReader isr;
		BufferedReader br;
		String line="none";
		try {
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			int cnt=0;
			while(cnt < 4)
			{
				line = br.readLine();
				cnt ++;
			}
			line = br.readLine();
			if(line == null)
				line = "none";
			String[] array = line.split(":");
			line = array[1].replaceAll(" ","");

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return line;
	}
	public String metadataToString(String dataid) {
		FileManagementDto dto = (FileManagementDto) database.select("file_management", dataid);
		return metaDataToString(dto);
	}
	public String metaDataToString(FileManagementDto dto) {
		String metaData = "none";
		if(dto != null) {
			metaData = dto.getDataId()+"#"+dto.getTimestamp()+"#"+dto.getFileType()+"#"+dto.getDataType()
			+"#"+dto.getSecurityLevel()+"#"+dto.getDataPriority()+"#"+dto.getAvailabilityPolicy()
			+"#"+dto.getDataSign()+"#"+dto.getCert()+"#"+dto.getDirectory()
			+"#"+dto.getLinkedEdge()+"#"+dto.getDataSize();
		}
		return metaData;
	}
	public static FileManagementDto metaDataToDto(String metadata) {
		String[] datas = metadata.split("#");
		FileManagementDto dto = new FileManagementDto();
		dto.setDataId(datas[0]);
		dto.setTimestamp(Timestamp.valueOf(datas[1]));
		dto.setFileType(datas[2]);
		dto.setDataType(Integer.parseInt(datas[3]));
		dto.setSecurityLevel(Integer.parseInt(datas[4]));
		dto.setDataPriority(Integer.parseInt(datas[5]));
		dto.setAvailabilityPolicy(Integer.parseInt(datas[6]));
		dto.setDataSign(datas[7]);
		dto.setCert(datas[8]);
		dto.setDirectory(datas[9]);
		dto.setLinkedEdge(datas[10]);
		dto.setDataSize(Long.parseLong(datas[11]));
		return dto;
	}
	private int netloadShellCmd(){
//		cat /proc/net/dev | grep enp3s0 | awk '{print $2}'
//		cat /proc/net/dev | head -n 3 | tail -n 1
		String command = "cat /proc/net/dev"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		InputStream is;
		InputStreamReader isr;
		BufferedReader br;
		String line="none";
		String[] array;
		double rx=0, tx=0;
		try {
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			line = br.readLine();
			line = br.readLine();
			line = br.readLine();
			if(line != null)
			{
				array = line.split(":");
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
				array = array[1].split(" ");
				rx = Double.parseDouble(array[1]); //55206812351
				tx = Double.parseDouble(array[9]);
			}
			
			br.close();
			isr.close();

			Thread.sleep(100);
			
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			line = br.readLine();
			line = br.readLine();
			line = br.readLine();
			if(line != null)
			{
				array = line.split(":");
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
				array = array[1].split(" ");
				rx =  (Math.abs(rx-Double.parseDouble(array[1])) * 8.0 / 1024.0 / 1024.0 / 0.1); //Mbps
				tx =  (Math.abs(tx-Double.parseDouble(array[9])) * 8.0  / 1024.0 / 1024.0 / 0.1); //Mbps
			}
			
			br.close();
			isr.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int load = (int) ((rx + tx) / 2);
		if(load==0 && rx!=0 || tx!=0)
			load = 1;
		else if(load>99)
			load = 99;
		
		return load;
	}

	public static byte[] messageCreate(byte[] data, String... args){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write("{[{".getBytes("UTF-8"));
			baos.write(args[0].getBytes("UTF-8"));
			for(int i=1; i<args.length;i++) {
				baos.write("::".getBytes("UTF-8"));
				baos.write(args[i].getBytes("UTF-8"));
			}
			baos.write("::".getBytes("UTF-8"));
			baos.write(data);
			baos.write("}]}".getBytes("UTF-8"));
			return baos.toByteArray();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static byte[] messageCreate(String... args) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write("{[{".getBytes("UTF-8"));
			baos.write(args[0].getBytes("UTF-8"));
			for(int i=1; i<args.length;i++) {
				baos.write("::".getBytes("UTF-8"));
				baos.write(args[i].getBytes("UTF-8"));
			}
			baos.write("}]}".getBytes("UTF-8"));
			return baos.toByteArray();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static String parseHex(byte[] data) {
		StringBuilder returns = new StringBuilder(); // Use StringBuilder for efficiency
        for (byte b : data) {
            String hex = String.format("%02X", b & 0xFF); // Apply bitwise AND with 0xFF to handle negative bytes correctly
            returns.append(hex);
        }
        return returns.toString();
	}
}
