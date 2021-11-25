package kr.re.keti;


import java.net.SocketException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.imageio.stream.FileImageInputStream;

import kr.re.keti.DataProcess.UnitEdge;
import kr.re.keti.EdgeReceptor.ReceptionEvent;
import java.sql.ResultSet;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean; //java.lang.management.OperatingSystemMXBean와 다름
import java.lang.management.MemoryMXBean;

/*
import java.lang.management.OperatingSystemMXBean;
import javax.management.MBeanServerConnection;

			MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();

			OperatingSystemMXBean osBean;
			try {
				osBean = ManagementFactory.newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
				System.out.println("Process Load : " + String.format("%.2f", osBean.getSystemLoadAverage()) + "%");			
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

*/
public class ReceiveWorker implements Runnable 
{
	/*
	 * public static void main(String[] args) { while (true) {
	 * System.out.println("waiting"); // extend Thread RemoteNode tsm1 = new
	 * RemoteNode(); tsm1.start(); //System.out.println(getName()); // extend Thread
	 * // implements Runnable Runnable r = new RemoteNode(); Thread tsm2 = new
	 * Thread(r); tsm2.start();
	 * //System.out.println(Thread.currentThread().getName()); // implements
	 * Runnable try { tsm1.join(); } catch (InterruptedException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } } }
	 */
	ReceiveWorker() // 
	{
		//Byte.toUnsignedInt()
	}
	ReceiveWorker(String ip, String dfname, String cfname, Database dp, String dev_uuid, String dbname, String tablename, String userid, String userpw) // v2 - 왜 여기서 instance를 이용한 후 master나 slave에서 이용하면 에러나는지 모르겠음. 
	{
//		receptionEvent = new ReceiveWorker();
		slaveList = new ArrayList<String>();
		edgeList = new ArrayList<String>();
		data_folder = dfname; 
		cert_folder = cfname;
//		origin_data_folder = data_folder;
		database = dp;
		device_uuid = dev_uuid;
		db_name = dbname;
		table_name = tablename;
		user_id = userid;
		user_pw = userpw;
		currentIPAddrStr = ip;
		
		url = "jdbc:mysql://localhost:3306/' + db_name + '?serverTimezone=UTC";


		metadata_list = (ResultSet) database.query(select_sql + table_name);
		try {
			int cnt = 1;
			while (metadata_list.next()) {
				// 레코드의 칼럼은 배열과 달리 0부터 시작하지 않고 1부터 시작한다.
				// 데이터베이스에서 가져오는 데이터의 타입에 맞게 getString 또는 getInt 등을 호출한다.
//				String dataID, fileType, dataSignature, cert, directory, linked_edge;	
//				Timestamp timestamp;
//				int dataType, securityLevel, dataPriority, availabilityPolicy, dataSize;
				timestamp = metadata_list.getTimestamp("timestamp");
//(dataid, availability_policy, cert, data_priority, data_signature, data_size, data_type, directory, file_type, linked_edge, security_level, timestamp)

				dataID = metadata_list.getString("dataid");
				fileType = metadata_list.getString("file_type");
				dataSignature = metadata_list.getString("data_signature");
				cert = metadata_list.getString("cert");
				directory = metadata_list.getString("directory");
				linked_edge = metadata_list.getString("linked_edge");
				
				dataType = metadata_list.getInt("data_type");
				securityLevel = metadata_list.getInt("security_level"); 
				dataPriority = metadata_list.getInt("data_priority");
				availabilityPolicy = metadata_list.getInt("availability_policy"); 
				dataSize = metadata_list.getLong("data_size");

				System.out.println("\t#" + cnt++ 
						+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
						+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
						+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize: " + dataSize);
				System.out.println("------------------------------------------------------------------");
			}
			metadata_list.close();
			System.out.println("==================================================================\n");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	ReceiveWorker(String ip, String dfname, String cfname,  Database dp, String dev_uuid, String dbname, String tablename, String userid, String userpw, String deviceIP) // v2 - 왜 여기서 instance를 이용한 후 master나 slave에서 이용하면 에러나는지 모르겠음. 
	{
//		receptionEvent = new ReceiveWorker();
		slaveList = new ArrayList<String>();
		edgeList = new ArrayList<String>();
//		origin_data_folder = data_folder;
		data_folder = dfname; 
		cert_folder = cfname;
		database = dp;
		device_uuid = dev_uuid;
		db_name = dbname;
		table_name = tablename;
		user_id = userid;
		user_pw = userpw;
		currentIPAddrStr = ip;
		device_ip = deviceIP;
		
		url = "jdbc:mysql://localhost:3306/' + db_name + '?serverTimezone=UTC";


		metadata_list = (ResultSet) database.query(select_sql + table_name);
		try {
			int cnt = 1;
			while (metadata_list.next()) {
				// 레코드의 칼럼은 배열과 달리 0부터 시작하지 않고 1부터 시작한다.
				// 데이터베이스에서 가져오는 데이터의 타입에 맞게 getString 또는 getInt 등을 호출한다.
//				String dataID, fileType, dataSignature, cert, directory, linked_edge;	
//				Timestamp timestamp;
//				int dataType, securityLevel, dataPriority, availabilityPolicy, dataSize;
				timestamp = metadata_list.getTimestamp("timestamp");
//(dataid, availability_policy, cert, data_priority, data_signature, data_size, data_type, directory, file_type, linked_edge, security_level, timestamp)

				dataID = metadata_list.getString("dataid");
				fileType = metadata_list.getString("file_type");
				dataSignature = metadata_list.getString("data_signature");
				cert = metadata_list.getString("cert");
				directory = metadata_list.getString("directory");
				linked_edge = metadata_list.getString("linked_edge");
				
				dataType = metadata_list.getInt("data_type");
				securityLevel = metadata_list.getInt("security_level"); 
				dataPriority = metadata_list.getInt("data_priority");
				availabilityPolicy = metadata_list.getInt("availability_policy"); 
				dataSize = metadata_list.getLong("data_size");

				System.out.println("\t#" + cnt++ 
						+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
						+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
						+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize: " + dataSize);
				System.out.println("------------------------------------------------------------------");
			}
			metadata_list.close();
			System.out.println("==================================================================\n");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*
	public void run() {
		int check = 0;
		try {
			//netstat -nap | grep LISTEN | grep 16300

			SocketAgent socketAgent_penta = new TCPSocketAgent(pentaCommPort);
			PacketProcessor packetProcessor_penta = new PacketProcessorImpl("penta");
			EdgeDevInfoAgent agent_penta = new EdgeDevInfoAgent(socketAgent_penta, packetProcessor_penta);

			Thread.sleep(100);
//			socketAgent = new UDPSocketAgent();
			SocketAgent socketAgent = new TCPSocketAgent(ketiCommPort);
			PacketProcessor packetProcessor = new PacketProcessorImpl("keti");
			EdgeDevInfoAgent agent = new EdgeDevInfoAgent(socketAgent, packetProcessor);
			

			agent.startAgent();
			agent_penta.startAgent();

			agent.joinAgent();
			agent_penta.joinAgent();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		while(true)
//		{
//		}
//		System.out.println("-> finish"); //v1102
//		run();
	}
*/
	public void run() {
		int check = 0;
		//netstat -nap | grep LISTEN | grep 16300

		SocketAgent socketAgent_penta = new TCPSocketAgent(pentaCommPort);
		PacketProcessor packetProcessor_penta = new PacketProcessorImpl("penta");
		EdgeDevInfoAgent agent_penta = new EdgeDevInfoAgent(socketAgent_penta, packetProcessor_penta);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		socketAgent = new UDPSocketAgent();
		SocketAgent socketAgent = new TCPSocketAgent(ketiCommPort);
		PacketProcessor packetProcessor = new PacketProcessorImpl("keti");
		EdgeDevInfoAgent agent = new EdgeDevInfoAgent(socketAgent, packetProcessor);
		

		agent.startAgent();
		agent_penta.startAgent();

		agent.joinAgent();
		agent_penta.joinAgent();
	}


	
	public static String cpu_shellCmd()
	{
		String command = "cat /proc/cpuinfo"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
//		System.out.println(command);
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
//				System.out.println(line);
			}
			line = br.readLine();
			if(line == null)
				line = "none";
			String[] array = line.split(":");
			line = array[1].replaceAll(" ","");

			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return line;
	}
	public static int netload_shellCmd()
	{
//		cat /proc/net/dev | grep enp3s0 | awk '{print $2}'
//		cat /proc/net/dev | head -n 3 | tail -n 1
		String command = "cat /proc/net/dev"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
//		System.out.println(command);
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
//			System.out.println(line);
			if(line != null)
			{
				array = line.split(":");
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
//				System.out.println(array[1]);
				array = array[1].split(" ");

				rx = Double.parseDouble(array[1]); //55206812351
				tx = Double.parseDouble(array[9]);
//				System.out.println(rx);
//				System.out.println(tx);
//				System.out.println(array[1]);
//				System.out.println(array[9]);
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
//				System.out.println(array[1]);
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
//				System.out.println(array[1]);
				array = array[1].split(" ");
//				System.out.println(Math.abs(rx-Double.parseDouble(array[1])));
				rx =  (Math.abs(rx-Double.parseDouble(array[1])) * 8.0 / 1024.0 / 1024.0 / 0.1); //Mbps
				tx =  (Math.abs(tx-Double.parseDouble(array[9])) * 8.0  / 1024.0 / 1024.0 / 0.1); //Mbps
//				System.out.println(rx);
//				System.out.println(tx);
			}
			
			br.close();
			isr.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int load = (int) ((rx + tx) / 2);
		if(load==0 && rx!=0 || tx!=0)
			load = 1;
		else if(load>99)
			load = 99;
		
		return load;
	}
	
	protected final static class PacketProcessorImpl extends PacketProcessor
	{
		private final int chunk_buffer_size = 4000;

		PacketProcessorImpl(String com)
		{
			company = com;
		}

		void keti_community(PacketType pkt, String originalData)
		{
			String result = "none";
			String answer = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::";	

			String[] array = originalData.substring(8, originalData.indexOf("}]}")).split("::");
			// [0] = my_ip, [1]=003, [2]=metadata or none
			
//			System.out.println("!! receive work : " + originalData);
			if (array[1].equals("001"))
			{
				if(array[2].equals("DEV_STATUS"))
					result = answer + array[1] + "::" + DeviceStatusInfo(array[2]) + "}]}";
				else if(array[2].equals("EDGE_LIST"))
					result = answer + array[1] + "::" + EdgeListUpdate(array[3]) + "}]}";
				else if(array[2].equals("SLAVE_LIST"))
					result = answer + array[1] + "::" + SlaveListInfo() + "}]}";
				
			}
			else if (array[1].equals("002"))
				result = answer + array[1] + "::" + WholeDataInfo(array[2]) + "}]}";
			else if (array[1].equals("003"))
				result = answer + array[1] + "::" + MetaDataInfo(array[2]) + "}]}";
			// 응답프로토콜에도 1=read 2=write 3=remove 표기할 것인지... 
			else if (array[1].equals("004")) // read
			{
//				System.out.println(ReceiveWorke1 : " + array[2].substring(0, array[2].indexOf(".")));
//				System.out.println("!! ReceiveWorker : " + array[2]);
//				result = MetaDataInfo(array[2]); // 필요없음 - DataProcess에서 선 검증
//				System.out.println("!! ReceiveWorker - work : " + result);

//				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
//				{
//				System.out.println("!! ReceiveWorker - read : " + dataSize);
//				System.out.println("!! ReceiveWorker - read : " + securityLevel);
					if(dataSize>4 && securityLevel==5)
					{
						int cnt = 0;
						int current = 0;
//						System.out.println(Math.ceil(dataSize/4.0));
						for(cnt=0; cnt<Math.ceil(dataSize/4.0)-1; cnt++)
						{
							result = answer + array[1] + "::1::" + Integer.toString((int)Math.ceil(dataSize/4.0)) + "::" + Integer.toString(cnt+1)  + "::" + String.format("%04d", partSize) + "::" + IndividualDataRead(array[2], current) + "}]}";
							current += partSize;
//							System.out.println("!! receive work : " + cnt);
							reply(pkt.getAddress(), result.getBytes());
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						result = answer + array[1] + "::" + Integer.toString((int)Math.ceil(dataSize/4.0)) + "::" + Integer.toString(cnt+1)  + "::" + String.format("%04d", partSize) + "::" + IndividualDataRead(array[2], current) + "}]}";
//						System.out.println("!! receive work : " + result);
					}
					else
					{
						if(dataSize > 9999)
							dataSize = (long) 9999;
					
						result = answer + array[1] + "::1::1::" + String.format("%04d", dataSize) + "::" + IndividualDataRead(array[2]) + "}]}";
//						System.out.println("!! receive work : " + result);						
					}
//				}
			}
			else if (array[1].equals("005")) // write
			{
				result = MetaDataInfo(array[2]);
				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
					result = answer + array[1] + "::" + IndividualDataWrite(array[2], array[3]) + "}]}";
				}
			}
			else if (array[1].equals("006")) // remove
			{
				result = MetaDataInfo(array[2]);
//				System.out.println("!! receive work : " + result);
				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
//					uuid = result.substring(0,36);
					result = answer + array[1] + "::" + IndividualDataRemove(array[2]) + "}]}";
//					System.out.println("!! receive work : " + result);
				}
			}
			else if (array[1].equals("400")) // data split
			{
//				System.out.println("!! ReceiveWorker - " + array[2]);
//				System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));
				if (array[2].indexOf(".") != -1)
					result = DataSplit(array[2]);
				result = answer + array[1] + "::" + result + "}]}";
//				System.out.println("!! ReceiveWorker - datasplit : " + result);
			}			
			else if (array[1].equals("401")) // data sha verify
			{
//				System.out.println("!! ReceiveWorker - " + array[2]);
//				System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));
				if (array[2].indexOf(".") != -1)
					try {
						result = sha(array[2]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				result = answer + array[1] + "::" + result + "}]}";
			}			
			else if (array[1].equals("404")) // chunk read
			{
//					System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));

//				System.out.println("!! ReceiveWorker - " + originalData); //
				File file = new File(data_folder + "chunk/" + array[2]);
				int FileLength = (int)file.length();
//				result = answer + array[1] + "::" + String.format("%04d", FileLength) + "::" + ChunkDataReadString(array[2]) + "}]}";
				String[] chunk_array = array[2].split("_");

//				result = answer + array[1] + "::" + chunk_array[1] + "::" + String.format("%04d", FileLength) + "::" + ChunkDataReadByte(array[2]) + "}]}"; // 전송프로토콜 양식에 맞춰 // chunk 프로토콜 규약 
				result = answer + array[1] + "::" + chunk_array[1] + "::" + ChunkDataReadByte(array[2]) + "}]}"; // file 길이 함수에서 같이 받아오는 경우 // chunk 프로토콜 규약
//				String.format("%04d", FileLength) == readBytes
//				result = ChunkDataReadByte(array[2]); // 전송프로토콜 양식에 맞춰
//				System.out.println("!! ReceiveWorker 404 - " + array[2]);
//				System.out.println("!! ReceiveWorker 404 result - " + result);
//				System.out.println("!! ReceiveWorker 404 chunk - " + ChunkDataReadByte(array[2]));
//				if(array[2].equals("395240aa-92e4-40fd-8014-12033767f351.csv_1"))
//				try { // for 공인인증
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} // for 공인인증
			}			
			else if (array[1].equals("405") && originalData.indexOf("REQ") > 0) // chunk구간요청(split포함)
			{
				// chunk request #3

//					System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
				long start_time=0;// = System.currentTimeMillis(); // + 32400000;
				long end_time=0;// = System.currentTimeMillis(); // + 32400000;

//				System.out.println("!! receive 405 req : " + originalData);
				int i, start = Integer.parseInt(array[3]), finish=Integer.parseInt(array[4]);

				result = DataSplit(array[2], start, finish); // 데이터가 무조건 있음.
								
				ChunkTransfer[] chunk_th = new ChunkTransfer[finish-start];
				for(i=start; i<finish; i++) // chunk 마다 thread 열어서 개별 다중 전송
				{
					chunk_th[i-start] = new ChunkTransfer(pkt, "406", array[2]+"_"+Integer.toString(i)); //
					start_time = System.currentTimeMillis(); // + 32400000;
					try {
						File file = new File(data_folder+"time/answer_"+array[2].replace(fileType, "txt")+"_"+Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
						FileWriter fw = new FileWriter(file);
						String str = format.format(new Date(start_time));
//							fw.write(req_ip+"\n");
//							fw.flush();
						fw.write(str+"\n");
						fw.flush();
						fw.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					chunk_th[i-start].start(); // chunk send - // chunk request #4
				}
				
				boolean[] working = new boolean[finish-start+1];
				Arrays.fill(working,true); // working[number_chunk] = true;
				int work_cnt=0;
				
				long period_time = System.currentTimeMillis();
				while(working[finish-start])
				{
					for(i=start; i<finish; i++) // thread가 끝났는지 검사 // chunk request #5
					{
						if(new File(data_folder+"chunk/"+array[2]+"_"+Integer.toString(i)).exists() && working[i-start])
						{
//							System.out.println("!! chunk read finish : " + Integer.toString(i));
							end_time = System.currentTimeMillis(); // + 32400000;
							try {
								File file = new File(data_folder+"time/answer_"+array[2].replace(fileType, "txt")+"_"+Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
								FileWriter fw = new FileWriter(file, true);
								String str = format.format(new Date(end_time));
//									fw.write(req_ip+"\n");
//									fw.flush();
								fw.write(str+"\n");
								fw.flush();
//								str = Long.toString(end_time - start_time);
//								fw.write("\n"+str+"ms\n");
//								fw.flush();
								fw.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							work_cnt ++;
							working[i-start] = false;
						}
					}
					if(work_cnt == finish-start)
						working[finish-start] = false;
//					else if(System.currentTimeMillis()-period_time > 5000) // re-request after 2sec
//					{
//						for(i=start; i<finish; i++) // thread가 끝났는지 검사
//						{
//							if(working[i-start])
//							{
////								System.out.println("!! chunk read finish : " + chunkList.get(i));
//								chunk_th[i-start].interrupt();
//								
//								chunk_th[i-start] = new ChunkTransfer(pkt, "405", array[2]+"_"+Integer.toString(i)); //
//								chunk_th[i-start].start();
//							}
//						}
//						period_time = System.currentTimeMillis();
//					}
				}
				
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				
				// chunk request #6 - sha 보내기
				result = answer + array[1] + "::sha::";
				for(i=start; i<finish; i++)
				{
					try {
//						System.out.println("!! receive 405 chunk sha : " + Integer.toString(i));
						String str = sha("chunk/"+array[2]+"_"+Integer.toString(i));
						try {
							File file = new File(data_folder+"time/answer_"+array[2].replace(fileType, "txt")+"_"+Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
							FileWriter fw = new FileWriter(file, true);
							fw.write(str+"\n");
							fw.flush();
							fw.close();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						result += str + "::";
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				result += "}]}";
			}			
			else if (array[1].equals("406") && originalData.indexOf("REQ") > 0) // chunk별 thread 전송내역 수신
			{
				// chunk request #5
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
//				System.out.println("!! receive 405 ans : " + originalData);
				File file = new File(data_folder+"chunk/"+array[2]); // 실제 chunk 수신 부분
				
				try { 
					
					if(!array[3].equals("none"))
					{
						FileOutputStream fos = new FileOutputStream(file);

						//			            array[3] = array[3] + "";
			            fos.write(array[4].getBytes("UTF-8"), 0, Integer.parseInt(array[3])); //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
//			            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
			            fos.flush();
						result = answer + array[1] + "::success::" + array[2] + "}]}"; 

//						// 수신 완료 시간 작성
//						long end_time = System.currentTimeMillis(); // + 32400000;
//
//						file = new File(data_folder+"time/request_"+array[2].replace(fileType, "txt")); // 1. check if the file exists or not boolean isExists = file.exists();
//						FileWriter fw = new FileWriter(file, true);
//						String str = format.format(new Date(end_time));
////							fw.write(req_ip+"\n");
////							fw.flush();
//						fw.write(str+"\n");
//						fw.flush();
////						str = Long.toString(end_time - start_time);
////						fw.write("\n"+str+"ms\n");
////						fw.flush();
//						fw.close();

			            fos.close();					
					}
					else
					{
						result = answer + array[1] + "::false::" + array[2] + "}]}"; 
					}
					
				} catch (IOException | NumberFormatException e) {
					// TODO Auto-generated catch block
					result = answer + array[1] + "::false::" + array[2] + "}]}"; 
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
			else if (array[1].equals("406") && originalData.indexOf("ANS") > 0) // chunk별 thread 전송내역 수신 결과
			{
//				System.out.println("!! receivework - keti : " + result);
				if(!array[2].equals("success")) //sha 검사하기
				{
					ChunkTransfer chunk_th = new ChunkTransfer(pkt, "406", array[3]);
					chunk_th.start(); // chunk send - // chunk request #4		
				}
				return ; // reply할 게 없으므로
			}
			else
			{
//				System.out.println("REQ_CODE is wrong.");
				return ; // reply할 게 없으므로
			}
//			System.out.println("!! receivework - keti : " + result);
//			System.out.println("!! receive-work : " + pkt.getAddress());
			
			reply(pkt.getAddress(), result.getBytes(), array[2]);
//			try { // for 공인인증
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} // for 공인인증
		}
		void penta_community(PacketType pkt, String originalData)
		{
			String result = "none";
			String answer = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::";	

//			System.out.println("!! penta_commnunity :" + originalData.substring(0,10));
			String[] array = originalData.substring(8, originalData.indexOf("}]}")).split("::");
			// [0] = my_ip, [1]=003, [2]=metadata or none
			if(!array[0].equals(currentIPAddrStr) && !array[0].equals(device_ip) && !array[1].equals("007")) //except : "007"
			{
				System.out.println("\tRequest the different edge node.");
				result = answer + array[1] + "::" + "none" + "}]}";
				reply(pkt.getAddress(), result.getBytes());
				return ;
			}
//			System.out.println("!! receive work : " + array[2]);
			if (array[1].equals("001"))
			{
				if(array[2].equals("DEV_STATUS"))
					result = answer + array[1] + "::" + DeviceStatusInfo(array[2]) + "}]}";
				else if(array[2].equals("SLAVE_LIST"))
					result = answer + array[1] + "::" + SlaveListInfo() + "}]}";
				else if(array[2].equals("EDGE_LIST"))
					result = answer + array[1] + "::" + EdgeListInfo() + "}]}";
				
			}
			else if (array[1].equals("002"))
				result = answer + array[1] + "::" + WholeDataInfo(array[2]) + "}]}";
			else if (array[1].equals("003"))
			{
//				System.out.println("!! ReceiveWorker - " + array[2]);
//				System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));
				if (array[2].indexOf(".") == -1)
					result = answer + array[1] + "::" + MetaDataInfo(array[2]) + "}]}";
				else
				{
//					System.out.println("!! ReceiveWorker - " + array[2].substring(array[2].indexOf(".")+1, array[2].length()));
					result = answer + array[1] + "::" + MetaDataInfo(array[2].substring(0, array[2].indexOf("."))) + "}]}";
					if(!fileType.equals(array[2].substring(array[2].indexOf(".")+1, array[2].length())))
						result = answer + array[1] + "::" + "none" + "}]}";
				}
			}
			// 응답프로토콜에도 1=read 2=write 3=remove 표기할 것인지... 
			else if (array[1].equals("004")) // read
			{
//				System.out.println("!! ReceiveWorke1 : " + array[2].substring(0, array[2].indexOf(".")));
//				System.out.println("!! ReceiveWorker : " + array[2]);
				if (array[2].indexOf(".") == -1)
				{
					result = MetaDataInfo(array[2]);
					array[2] = dataID + "." + fileType;
				}
				else
				{
//					result = MetaDataInfo(array[2].substring(0, array[2].indexOf("."))); // 필요없음 - DataProcess에서 선 검증
//					System.out.println("!! ReceiveWorker - " + array[2].substring(array[2].indexOf(".")+1, array[2].length()));
					result = MetaDataInfo(array[2].substring(0, array[2].indexOf(".")));
					if(!fileType.equals(array[2].substring(array[2].indexOf(".")+1, array[2].length())))
						result = "none";
				}
//				System.out.println("!! ReceiveWorker : " + result);
//				System.out.println("!! ReceiveWorker - work : " + result);

				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
//				System.out.println("!! ReceiveWorker - read : " + dataSize);
//				System.out.println("!! ReceiveWorker - read : " + securityLevel);
					if(securityLevel==5)
					{
						if(dataSize>4)
						{
							int cnt = 0;
							int current = 0;
//							System.out.println(Math.ceil(dataSize/4.0));
							for(cnt=0; cnt<Math.ceil(dataSize/4.0)-1; cnt++)
							{
								result = answer + array[1] + "::1::" + Integer.toString((int)Math.ceil(dataSize/4.0)) + "::" + Integer.toString(cnt+1)  + "::" + String.format("%04d", partSize) + "::" + IndividualDataRead(array[2], current) + "}]}";
								current += partSize;
//								System.out.println("!! receive work : " + cnt);
								reply(pkt.getAddress(), result.getBytes());
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							result = answer + array[1] + "::" + Integer.toString((int)Math.ceil(dataSize/4.0)) + "::" + Integer.toString(cnt+1)  + "::" + String.format("%04d", partSize) + "::" + IndividualDataRead(array[2], current) + "}]}";
//							System.out.println("!! receive work : " + result);
						}
						else
							result = answer + array[1] + "::1::1::" + String.format("%04d", dataSize) + "::" + IndividualDataRead(array[2]) + "}]}";
					}
					else
					{
						if(dataSize > 9999)
							dataSize = 9999;
					
//						result = answer + array[1] + "::1::1::" + String.format("%04d", dataSize) + "::" + IndividualDataRead(array[2]) + "}]}";
						result = answer + array[1] + "::" + IndividualDataRead(array[2]) + "}]}";
//						System.out.println("!! receive work : " + result);						
					}
				}
				else
				{
					result = answer + array[1] + "::" + "none" + "}]}";
				}
			}
			else if (array[1].equals("005")) // write
			{
				result = MetaDataInfo(array[2]);
				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
					result = answer + array[1] + "::" + IndividualDataWrite(array[2], array[3]) + "}]}";
				}
			}
			else if (array[1].equals("006")) // remove
			{
//				System.out.println("!! ReceiveWorker - " + array[2]);
//				System.out.println("!! ReceiveWorker - " + array[2].substring(0, array[2].indexOf(".")));
				if (array[2].indexOf(".") == -1)
				{
					result = MetaDataInfo(array[2]);
					array[2] = dataID + "." + fileType;
				}
				else
				{
//					result = MetaDataInfo(array[2].substring(0, array[2].indexOf("."))); // 필요없음 - DataProcess에서 선 검증
//					System.out.println("!! ReceiveWorker - " + array[2].substring(array[2].indexOf(".")+1, array[2].length()));
					result = MetaDataInfo(array[2].substring(0, array[2].indexOf(".")));
					if(!fileType.equals(array[2].substring(array[2].indexOf(".")+1, array[2].length())))
						result = "none";
				}
//				System.out.println("!! receive work : " + result);
				if(!result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
//					uuid = result.substring(0,36);
					result = answer + array[1] + "::" + IndividualDataRemove(array[2]) + "}]}";
//					System.out.println("!! receive work : " + result);
				}
				else
					result = answer + array[1] + "::" + "none" + "}]}";
			}
			else if (array[1].equals("007") && originalData.indexOf("REQ") > 0) // send
			{
				String meta_result = MetaDataInfo(array[2]);
				if(!meta_result.equals("none")) //메타데이터가 없으면, 읽기 작업 안함
				{
					String data_file = dataID + fileType;
					String cert_file = cert;
					
					result = answer + array[1] + "::" + IndividualDataSend(data_file, cert_file, meta_result, array[0]) + "}]}";
				}
				else // data information X
					result = answer + array[1] + "::false}]}";
					
			}
			else
			{
//				System.out.println("REQ_CODE is wrong.");
				return ;
			}
//			System.out.println("!! receivework - penta : " + result);
//			System.out.println("!! receive-work : " + pkt.getAddress());
			
			reply(pkt.getAddress(), result.getBytes());
			
		}
		

		@Override
		protected void work()
		{
			PacketType pkt = getRequestPacket();
			String originalData = new String(pkt.getData());
//			System.out.println("!! receive work : " + pkt.getAddress().getHostAddress() + "==" +  currentIPAddrStr);
//			System.out.println("!! receiveWorker - work : " + company);
//			System.out.println("!! receiveWorker -  work : " + originalData);
			

//			if(originalData.indexOf("{[{REQ")==0 && originalData.indexOf("}]}")!=-1) // 기본 양식 맞음
			if(originalData.indexOf("{[{")==0 && originalData.indexOf("}]}")!=-1) // 기본 양식 맞음
			{
				if(company.equals("keti"))
					keti_community(pkt, originalData);
				else if(company.equals("penta"))
					penta_community(pkt, originalData);
			}
//			else
//				System.out.println("REQ_MESSAGE is wrong."); // 발생하는 경우 있음 error
		}
		
		@SuppressWarnings("deprecation")
		String DeviceStatusInfo(String message)
		{
			String result = "none";	
			double mb = 1024.0 * 1024.0;
			double gb = 1024.0 * 1024.0 * 1024.0;
			
			// 프로세서 부하량
			OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
			/* getSystemLoadAverage ()
			// 1보다 낮은 값을 유지하면 서버의 상태 원활하고, 1보다 높으면 서버가 부하를 받기 시작한다고 판단할 수 있다. 
			// 평균적으로 5 이상을 시스템에 과부하가 있다고 판단하지만 절대적인 판단의 기준은 절대 아니다.*/
			int process_load=0;
			if (osBean.getSystemLoadAverage() != -1) // window 에선 -1 
				process_load = (int) Math.ceil(osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100); // cpu사용량 - osBean.getSystemCpuLoad() * 100;
			
			// 물리적 실제 메모리 
			// memory 사용량 : free -m	
			double mem_free = (double)osBean.getFreePhysicalMemorySize();
			double mem_total = (double)osBean.getTotalPhysicalMemorySize();
			double mem_usage = Math.ceil((mem_total-mem_free) / mem_total * 100);
//			System.out.println("!! DeviceStatusInfo - Total Memory : " + mem_total/gb + "[GB]"); 			
//			System.out.println("!! DeviceStatusInfo - free Memory : " + mem_free/gb + "[GB]"); 			
//			System.out.println("!! DeviceStatusInfo - Memory Usage : " + String.format("%.2f", (mem_total-mem_free)/mem_total*100.0) + "%"); 
			mem_total /= gb;
			
			
			// memory 사용량 : free -m				
			// 스토리지 여유량
			int storage_total=0, storage_free=0;
			try
			{
				File root = new File( data_folder );
				storage_total = (int)Math.round(root.getTotalSpace() / gb); // 우분투 = round
				storage_free = (int)Math.round(root.getUsableSpace() / gb);
//				System.out.println( "!! DeviceStatusInfo -Total  Space: " + storage_total + "MB" );
//				System.out.println( "!! DeviceStatusInfo - Usable Storage: " + storage_free + "MB" );
			}
			catch ( Exception e )
			{
				e.printStackTrace( );
			}
			
			String cpu_id=cpu_shellCmd();
			if(cpu_id.length() < 8)
				cpu_id = cpu_id + "    ";
			else if(cpu_id.length() > 8)
				cpu_id = cpu_id.substring(0, 8);
			
			int net_load = netload_shellCmd();
			
			if(mem_total > 99)
				mem_total = 99;
			if(storage_total > 9999)
				storage_total = 9999;
			if(storage_free > 9999)
				storage_free = 9999;
			if(process_load > 99)
				process_load = 99;
			if(mem_usage > 99)
				mem_usage = 99;
			
			
			result = device_uuid + cpu_id + String.format("%02.0f", mem_total) + String.format("%04d", storage_total) + "        " + String.format("%02d", process_load) + String.format("%02.0f", mem_usage) + String.format("%02d", net_load) + String.format("%04d", storage_free) + "  ";
//			result = device_uuid + cpu_id + String.format("%02d", mem_total) + String.format("%04d", storage_total) + "        " +  Base64.encodeBase64URLSafeString(pl) + Integer.toString(mem_usage) + String.format("%02d", net_load) + String.format("%04d", storage_free) + "  ";
//			System.out.println("!! DeviceStatusInfo : " + result);
			return result;
		}

		String SlaveListInfo()
		{
			String result = "none";	
			for (String slave : slaveList)
			{
				if(result.equals("none"))
					result = slave;
				else
					result += "::" + slave;
			}
			return result;
		}
		String EdgeListInfo()
		{
			String result = "none";	
			for (String edge : edgeList)
			{
				if(result.equals("none"))
					result = edge;
				else
					result += "#" + edge;
			}
			if(result.equals("none"))
				result = currentIPAddrStr;
			return result;
		}
		String EdgeListUpdate(String slist)
		{
			String[] array = slist.split(":");
			// [0] = my_ip, [1]=003, [2]=metadata or none
			
//			System.out.println("!! slaveList : " + array[2]);

			edgeList.clear();
			for(int i=0; i<array.length; i++)
			{
				edgeList.add(array[i]);
//				System.out.println("!! slaveList : " + array[i]);
			}
//			System.out.println("!! slaveList : " + edgeList);
			return "updated";
		}
		
		String WholeDataInfo(String message)
		{
			String result = "none";	
			return result;
		}
		
		String MetaDataInfo(String message)
		{
			String result = "none";	
//			String sql = select_sql + db_name + " where uuid = '" + message + "'";
			String sql = select_sql + table_name + " where dataid = '" + message + "'";
			ResultSet metadata = (ResultSet) database.query(sql);
        	try {
				if(metadata.next()) // metadata가 있으면 true, 없으면 false
				{
					dataID = metadata.getString("dataid");
					fileType = metadata.getString("file_type");
					// 기존 메타데이터
//        		uuid = rs.getString("uuid");
//        		security = rs.getInt("security");
//        		sharing = rs.getInt("sharing");
//        		location = rs.getString("location");
//        		System.out.print("Name: "+ file_name + "\nUUID: " + uuid + "\nSecurity: " + security); 
//        		System.out.println("\nSharing: "+ sharing + "\nLocation: " + location + "\n--------------------------\n");
					// 기존 메타데이터
					if(message.equals(dataID))
					{
						// 기존 메타데이터
//						File file = new File(data_folder+message+"."+fileType);
//						dataSize = 0;
//						if (file.length() > 0)
//						{
//							dataSize = (int)Math.ceil(file.length() / 1024.0); // kilo = bytes/1024
////							System.out.println("!!MetaDataInfo : " + (file.length() / 1024.0) + ":" + dataSize);
////							if (dataSize == 0) // 데이터 크기가 1KB 이하이면
////								dataSize = 1;
//						}
						// 기존 메타데이터

						timestamp = metadata.getTimestamp("timestamp");

//						dataID = metadata_list.getString("dataID");
//						fileType = metadata_list.getString("fileType");
						dataSignature = metadata.getString("data_signature");
						cert = metadata.getString("cert");
						directory = metadata.getString("directory");
						linked_edge = metadata.getString("linked_edge");
//						System.out.println("!! ReceiveWorker : " + directory);
//						System.out.println("!! ReceiveWorker : " + linked_edge);
						
						dataType = metadata.getInt("data_type");
						securityLevel = metadata.getInt("security_level"); 
						dataPriority = metadata.getInt("data_priority");
						availabilityPolicy = metadata.getInt("availability_policy"); 
						dataSize = metadata.getLong("data_size");

						// 기존 메타데이터
//						uuid = rs.getString("uuid");
//						security = rs.getInt("security");
//						datatype = rs.getInt("sharing");
//						location = rs.getString("location");
//        			data_folder = rs.getString("location");
//			        	result = uuid + String.format("%03d", location.length()) + location +  String.format("%04d", dataSize) + security + datatype + "}]}"; //0813 - dataSize를 포함
						// 기존 메타데이터
						//Timestamp.valueOf(timestamp);
						result = dataID + "#" + timestamp + "#" + fileType + "#" + dataType + "#" + securityLevel + "#" + dataPriority + "#" + availabilityPolicy + "#" + dataSignature + "#" + cert + "#" + directory + "#" + linked_edge + "#" + dataSize;
//						System.out.println("!! ReceiveWorker - metainfo : " + result);
						
//			String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
//						break;
					}
				}
        		//		String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//        	System.out.println("!! MetaDataInfo : " + result);
			return result;
		}
		
		String IndividualDataRead(String message)
		{
			String result = "none";
//			if(!MetaDataInfo(message).equals("none")) //link 데이터여서, 실제 데이터가 없으면 아무것도 안함
//			{
				if(dataType != 1)
				{	
					File file = new File(data_folder + message);
					if(file.exists()) {

						String lines = "";
	
						try {
	//						System.out.println("!! ReceiveWorker - read : " + data_folder + message);
							BufferedReader br = new BufferedReader(new FileReader(file));
	//						System.out.println(security);
	//						System.out.println("	["); //v1102
							while (true) {
								String line = br.readLine();
								if (line == null)
									break;
	
								lines += line + "\n";
							}
							
							br.close();
	
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						result = lines;
					}
				}
				
//			}
			return result;
		}
		String IndividualDataRead(String foldername, String message)
		{
			String result = "none";
//			if(!MetaDataInfo(message).equals("none")) //link 데이터여서, 실제 데이터가 없으면 아무것도 안함
//			{
				if(dataType != 1)
				{	
					File file = new File(foldername + message);
					if(file.exists()) {

						String lines = "";
	
						try {
	//						System.out.println("!! ReceiveWorker - read : " + data_folder + message);
							BufferedReader br = new BufferedReader(new FileReader(file));
	//						System.out.println(security);
	//						System.out.println("	["); //v1102
							while (true) {
								String line = br.readLine();
								if (line == null)
									break;
	
								lines += line + "\n";
							}
							
							br.close();
	
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						result = lines;
					}
				}
				
//			}
			return result;
		}		
		String IndividualDataRead(String filename, int current)
		{
			String result = "none";
//			if(!MetaDataInfo(filename).equals("none"))
//			{
				if(dataType != 1) //link 데이터여서, 실제 데이터가 없으면 아무것도 안함
				{	
					
					File f = new File(data_folder + filename);
					if(f.exists()) {
						String lines = "";
						int bytecnt=0;
						partSize = 0;
						
						try {
//							System.out.println(security);
//							System.out.println("	["); //v1102
							BufferedReader br = new BufferedReader(new FileReader(data_folder + filename));
							while(true)
							{
								String line = br.readLine();
								bytecnt += (line + "\n").getBytes().length;
								if (line == null)
									break;

								if (bytecnt >= current+4096)
								{
//									System.out.println("!! IndividualDataRead2 : " + line );
									break;
								}
								else if(bytecnt>=current && bytecnt<current+4096)
								{
//									System.out.println("!! IndividualDataRead1 : " + line );
									lines += line + "\n";
									if(partSize == 0)
										partSize = bytecnt;
//									System.out.println("!! IndividualDataRead : " + bytecnt + " _ " + current);
								}
							}
							br.close();
							

						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						result = lines;
						partSize = bytecnt - partSize;
//						System.out.println("!! IndividualDataRead : " + bytecnt + " _ "  + partSize + " _ " + current);
					} 				
				}
//			}
			return result;
		}		

		String ChunkDataReadString(String message)
		{
			String result = "none";
			String lines = "";
	
			try {
//						System.out.println("!! ReceiveWorker - read : " + data_folder + message);
				BufferedReader br = new BufferedReader(new FileReader(data_folder + "chunk/" + message));
//						System.out.println(security);
//						System.out.println("	["); //v1102
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;

					lines += line + "\n";
				}
				
				br.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			result = lines;
			return result;
		}
		String ChunkDataReadByte(String message)
		{
			String result = "none";
			String lines = "";
	        byte[] buffer = new byte[chunk_buffer_size];
	
			try {
//						System.out.println("!! ReceiveWorker - read : " + data_folder + message);
				File file = new File(data_folder + "chunk/" + message);
				if(!file.exists())
					return result;
				
//						System.out.println(security);
//						System.out.println("	["); //v1102
		        long fileSize = file.length();
		        long totalReadBytes = 0;
		        int readByte, readBytes=0;
		        double startTime = 0;
		         
	            FileInputStream fis = new FileInputStream(file);
	            
	            while ((readByte = fis.read(buffer)) > 0)
	            {
//	            if((readBytes = fis.read(buffer)) > 0)
	            	lines += new String(buffer, "UTF-8");
	            	readBytes += readByte;
//	            	result = Base64.getEncoder().encodeToString(buffer); //new String(buffer);
//	            	if(readBytes > chunk_buffer_size)
//	            		System.out.println("!! ReceiveWorker - chunk : " + readBytes);
	            }
	            if(readBytes > 0)
//	            	result = String.format("%04d", readBytes) + "::" + lines; //파일 길이 포함해서 보내기.
	            	result = String.format("%04d", readBytes) + "::" + lines; //파일 길이 포함해서 보내기.
	            else
	            	result = "0000::"; //파일 길이 포함해서 보내기.
//	            while((readBytes = fis.read(buffer)) > 0) // 조각 파일 최대 4kb
//	            	result += new String(buffer);
	            
	            fis.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			return buffer.toString();
			return result; //buffer.toString();
		}
		
		String IndividualDataWrite(String filename, String message)
		{
			String result = "none";	
			
//			if(!MetaDataInfo(filename).equals("none"))
//			{
				if(securityLevel > 1)
				{
					result = "permission";
					System.out.println("-> Have Permission to Write to Data[" + message + "] in " + filename);
					
					if(dataType != 1) //datatype == 1 == link data
					{
						
					}
				}
				else
				{
					System.out.println("-> Do not Have Permission to Write to Data[" + message + "] in " + filename);
					result = "failure";
				}
					
//			}
			return result;
		}
		
		String IndividualDataRemove(String filename)
		{
			String result = "none";	
//			if(!MetaDataInfo(filename).equals("none"))
//			{
				if(securityLevel > 2)
				{					
					result = "permission";
//					System.out.println("!! IndividualDataRemove " + datatype);
					if(dataType != 1) //datatype == 1 == link data
					{
						File file = new File(data_folder+filename);
						boolean check1 = false; 
						if( file.exists() )
						{ 
							check1 = file.delete();
						}
						boolean check2 = database.delete(dataID); 
//						if(check1 && check2)
//						{ 
//							result = "success";
//							System.out.println("-> Have Permission(both) to Remove a " + filename);
//						}
//						else if(check1)
//						{ 
//							result = "permission::data";
//							System.out.println("-> Have Permission(data) to Remove a " + filename);
//						} 
//						else if(check2)
//						{ 
//							result = "permission::db";
//							System.out.println("-> Have Permission(db) to Remove a " + filename);
//						} 
					}
				}
				else
				{
					System.out.println("-> Do not Have Permission to Remove a " + filename);
					result = "authority";
				}
					
//			}			
			
			return result;
		}

		String IndividualDataSend(String data_file, String cert_file, String meta_info, String ip)
		{
			String result = "false", check="";
			String remote_cmd = "{[{REQ::" + ip + "::007::";
			
			EdgeDeviceInfoClient client = new EdgeDeviceInfoClient(ip, ketiCommPort);
			client.startWaitingResponse();
			
			check = IndividualDataRead(data_file);
			if(!check.equals("none"))
			{
				remote_cmd += check;
				check = IndividualDataRead("", cert_file);
				if(!check.equals("none"))
				{
					remote_cmd += "::" + check + "::" + meta_info;
					client.sendPacket(remote_cmd.getBytes(), remote_cmd.length()); // send to keti
					client.stopWaitingResponse(); //
					result = " success"; // to penta
				}
				else
					result = "false";
			}
			
			return result;
		}
		
		public static String DataSplit(String filename)
		{
			String result="none";
			try {
				File file = new File(data_folder+filename);
				String path = file.getParent();
				if(!file.exists())
				{
					result = "false";
					return result;
				}
				
				long originTotalFileLength = file.length();
				int chunk_size = 1000; //1KB
				long jobProcess = 0;
				long chunkCnt = (long)Math.ceil((double)originTotalFileLength / (double)chunk_size);
				
//				for (int i = 1; i <= chunkCnt; i++) {
//					FileOutputStream fout = new FileOutputStream("./chunk/"+filename + "_" + i);
//					
//					int len = 0;
//					byte[] buf = new byte[1024];
//
//					while ((len = in.read(buf, 0, 1024)) != -1) {
//						fout.write(buf, 0, len);
//						jobProcess = jobProcess + len;
//						
//						if (fileSplitSize * (i + 1) == jobProcess) break;
//					}
//					fout.flush();
//					fout.close();
//				}
				
				FileInputStream in = new FileInputStream(file);
				for (int i = 0; i < chunkCnt; i++) {
					FileOutputStream fout = new FileOutputStream(data_folder+"chunk/"+filename + "_" + (i+1)); //.format("%d", (i+1))
					int len = 0;
					byte[] buf = new byte[chunk_size];
					while ((len = in.read(buf, 0, chunk_size)) != -1) {
						fout.write(buf, 0, len);
						fout.flush();
						jobProcess = jobProcess + len;
						
						if (chunk_size * (i + 1) == jobProcess) break;
					}
					fout.close();
//					Thread.sleep(1);
				}
				in.close();
				result = "success";
			} catch (Exception e) {
				result = "false";
				e.printStackTrace();
			}
//			System.out.println("!! data split : " + result);
			return result;
		}
		
		public static String DataSplit(String filename, int start, int finish)
		{
			// chunk request #3-2
			String result="none";
			try {
				File file = new File(data_folder+filename);
				String path = file.getParent();
				if(!file.exists())
				{
					result = "false";
					return result;
				}
				
				long originTotalFileLength = file.length();
				int chunk_size = 1000; //1KB
				long jobProcess = 0;
				long chunkCnt = (long)Math.ceil((double)originTotalFileLength / (double)chunk_size);
				
//				for (int i = 1; i <= chunkCnt; i++) {
//					FileOutputStream fout = new FileOutputStream("./chunk/"+filename + "_" + i);
//					
//					int len = 0;
//					byte[] buf = new byte[1024];
//
//					while ((len = in.read(buf, 0, 1024)) != -1) {
//						fout.write(buf, 0, len);
//						jobProcess = jobProcess + len;
//						
//						if (fileSplitSize * (i + 1) == jobProcess) break;
//					}
//					fout.flush();
//					fout.close();
//				}
				
				FileInputStream in = new FileInputStream(file);
				for (int i = 0; i < chunkCnt; i++) {
					FileOutputStream fout = null;
					if(i+1>=start && i+1<finish)
						fout = new FileOutputStream(data_folder+"chunk/"+filename + "_" + (i+1)); //.format("%d", (i+1))					int len = 0;

					int len = 0;
					byte[] buf = new byte[chunk_size];
					while ((len = in.read(buf, 0, chunk_size)) != -1) {
						if(i+1>=start && i+1<finish)
						{
							fout.write(buf, 0, len);
							fout.flush();
						}
						jobProcess = jobProcess + len;
						
						if (chunk_size * (i + 1) == jobProcess) break;
					}
					if(i+1>=start && i+1<finish)
						fout.close();
//					Thread.sleep(1);
				}
				in.close();
				result = "success";
			} catch (Exception e) {
				result = "false";
				e.printStackTrace();
			}
//			System.out.println("!! data split : " + result);
			return result;
		}
		public static String sha(String filepath) throws Exception{
	        File file = new File(data_folder+filepath);
	        if(!file.exists())
	        {
	        	Thread.sleep(100);
	        }
	        if(!file.exists())
	        {
	        	return "";
	        }

	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        FileInputStream fis = new FileInputStream(data_folder+filepath);
	        
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
	 
	        //System.out.println("SHA-256 : " + sb.toString());
	        fis.close();
	        return sb.toString();
	    }
		
		void backup_read_file_split()
		{
/*			
			else if (array[0].equals("read")) {
				check = 0;
				try {
					BufferedReader br = new BufferedReader(new FileReader(data_folder + array[1]));
					String lines = "";
//					System.out.println(security);
					if(security == 1)
					{
						n = is.read(data);
						String hash_code = new String(data, 0, n);
						String[] hash = hash_code.split("#");
//						System.out.println(hash[0] + " : " + hash[1]); 

						StringBuffer sb = new StringBuffer();
						try {

							MessageDigest md = MessageDigest.getInstance("SHA-256");
							FileInputStream fis = new FileInputStream("hash_info.txt");
					        
							byte[] dataBytes = new byte[1024];
					     
							int nread = 0; 
							while ((nread = fis.read(dataBytes)) != -1) {
								md.update(dataBytes, 0, nread);
					  		};
//					  		System.out.println(hash[0]);
							md.update(hash[0].getBytes(), 0, hash[0].length());
					  		byte[] mdbytes = md.digest();
					     
							for (int i = 0; i < mdbytes.length; i++) {
								sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
							}
							fis.close();										
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if(sb.toString().equals(hash[1]))
						{
							System.out.println("\t-> authentication hash code (" + sb.toString() + ") is correct");
							os.write("yes".getBytes());
							os.flush();
						}
						else
						{
							System.out.println("\t-> authentication hash code (" + sb.toString() + ") is not correct");
							os.write("no".getBytes());
							os.flush();
							break;
						}
						
					}
					System.out.println("	["); //v1102
					while (true) {
						String line = br.readLine();
						if (line == null)
							break;

						if (lines.length() + line.length() > 100) {
							os.write(lines.getBytes());
							os.flush();
//							System.out.println("\t\t" + lines); //v1102
							
							lines = "";
							Thread.sleep(5);
						}
//                        line = line;
						check = 1;
						lines += line + "\n";
						int len = line.length();
						int start=0;
						while(start+100 < len)
						{
							System.out.println("\t\t" + line.substring(start, start+100)); //v1102
							start += 100;
						}
						System.out.println("\t\t" + line.substring(start, len)); //v1102
					}
					os.write(lines.getBytes());
					os.flush();
					System.out.println("	]"); //v1102
					if (check == 1)
						System.out.println("\t-> finish to transmit the DATA : " + array[1]); //v1102

					br.close();
					message = "finish";

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} 
*/			
		}

//		private String select_sql = "SELECT * FROM file_management"; //(file_name, uuid, security, sharing, location)
//		private ResultSet rs = null;
		private String company = null;

		class ChunkTransfer extends Thread // chunk별 thread 전송
		{
//			public static String data_folder="/data/";
			public String req_content, req_code, req_ip;
			PacketType req_pkt;
			
			ChunkTransfer(PacketType pkt, String code, String content) {
				req_content = content;
				req_code = code;
				req_pkt = pkt;
				req_ip = req_pkt.getAddress().getHostAddress();
			}
			ChunkTransfer(String ip, String code, String content) {
				req_content = content;
				req_code = code;
				req_ip = ip;
			}
			public void run() // 동기화 synchronized - 소용없음
			{
				// chunk request #4
				EdgeDeviceInfoClient client =  new EdgeDeviceInfoClient(req_ip, EdgeDeviceInfoClient.socketTCP);
				client.answerData = null;
				client.startWaitingResponse();
				
				String[] chunk_array = req_content.split("_");
				String remote_cmd = "{[{REQ::" + req_pkt.getAddress().getHostAddress() + "::" + req_code + "::" + req_content + "::" + ChunkDataReadByte(req_content) + "}]}"; // file 길이 함수에서 같이 받아오는 경우 // chunk 프로토콜 규약
//				System.out.println("!! ChunkTransfer send : " + req_content);
				
				client.sendPacket(remote_cmd.getBytes(), remote_cmd.length()); //실제 chunk 보내는 부분
//				while(true) // chunk send result 응답 대기 - 수신이 receive로 되서 종료가 안됨
//				{
//					client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
//					
//					while(client.answerData == null)
//					{
//						try {
//							Thread.sleep(10);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} // test 필요
//					}
//					
//					if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
//					{
////						System.out.println("!! RequestMessageRead : " + client.answerData); //
//						String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
//						client.answerData = null;
//						if(array[2].equals("success")) //sha 검사하기
//						{
//							break;
//						}
//						else
//							System.out.println("!! ChunkTransfer false : " + array[3]);
//					}
//					
//				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // test 필요
				
				
				client.stopWaitingResponse();
			}
		}
	}

	public void setEventHandler(ReceiveWorker ev)
	{
		receptionEvent = ev;
	}
	public static interface ReceiveWorkerEvent // like to callback function
	{
		public void handler(String ip);
	}
	
	
	public ArrayList<String> slaveGetting()
	{
		return slaveList;
	}
	public void slaveSetting(ArrayList<String> slist)
	{
		slaveList = (ArrayList<String>) slist.clone();
//		System.out.println("!! ReceiveWorker - slave : " + slaveList);
	}
	public void edgeListadd(ArrayList<String> slist)
	{
		edgeList = (ArrayList<String>) slist.clone();
//		System.out.println("!! ReceiveWorker - edge : " + edgeList);
	}
	
	

	private ReceiveWorker receptionEvent;
	private static ArrayList<String> slaveList;
	private static ArrayList<String> edgeList;

//	public static String filename;
	private static String device_uuid = "", device_ip=null;
//	private static String origin_data_folder = "/home/eunae/keti/";
	private static String data_folder = "/home/keti/data/";
	private static String cert_folder = "/home/keti/cert/";
	private String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
	static String select_sql = "SELECT * FROM "; //file_management"; //(file_name, uuid, security, sharing, location) //
	static String whatDB=null, db_name=null, db_path=null, table_name=null, user_id=null, user_pw=null;
	static ResultSet metadata_list = null;
//	static String uuid="", location="", file_name="";
//	static int security=-1, datatype=-1, datasize=-1, d_length=-1, partsize=-1;
	private static Database database = null;
	static String dataID, fileType, dataSignature, cert, directory, linked_edge;	
	static Timestamp timestamp;
	static int dataType, securityLevel, dataPriority, availabilityPolicy, partSize;
	static long dataSize;
	static int ketiCommPort = 5679; // KETI 내부통신
	static int pentaCommPort = 16300; // PENTA 외부통신
	static String currentIPAddrStr = null;

}
