package kr.re.keti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import kr.re.keti.ReceiveWorker.PacketProcessorImpl.ChunkTransfer;

public class DataProcess {

	public DataProcess(String dfname, String cfname, Database db, String ip, String tablename)
	{
		database = db;
//		origin_foldername = fname;
		data_folder = dfname;
		cert_folder = cfname;
		local_ip = ip;
		table_name = tablename;
		TCPSocketAgent.defaultPort = ketiCommPort;
	}
	
	public ArrayList<String> RequestSlaveList(String ip)
	{
		EdgeDeviceInfoClient client;
		ArrayList<String> slavelist = new ArrayList<String>();
		String remote_cmd="";

		try {
			remote_cmd = "{[{REQ::" + ip + "::001::SLAVE_LIST}]}";
//			System.out.println("!! RequestSlaveList : " + remote_cmd);
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());

			while(client.answerData == null)
			{			
				Thread.sleep(100);
			}
			
//			System.out.println("!! RequestSlaveList : " + client.answerData);
			if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
			{
				String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::"); // [0] = my_ip, [1]=003, [2]=metadata or none
				client.answerData = null;
				
//				System.out.println("!! slaveList : " + array[2]);
				if (array[2].equals("none"))
					return slavelist;
				for(int i=2; i<array.length; i++)
				{
//					System.out.println("!! slaveList : " + array[i]);
					slavelist.add(array[i]);
				}
			}
			client.stopWaitingResponse();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return slavelist;		
	}
	public void SendEdgeList(ArrayList<String> iplist, String result)
	{
		EdgeDeviceInfoClient client;

		try {
			for (String ip : iplist)
			{
//				System.out.println("!! dataprocess - " + result);
//				System.out.println("!! dataprocess - " + ip);
				client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
				client.startWaitingResponse();
				String remote_cmd = "{[{REQ::" + ip + "::" + "001" + "::EDGE_LIST::" + result + "}]}";

				client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
				client.stopWaitingResponse();
				
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public String RequestMessage(String req_content, String ip, String req_code) // real exists in node
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			if(req_code.equals("001"))
					remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::DEV_STATUS}]}";
			else if(req_code.equals("002"))
					remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::DATA_INFO}]}";
			else
				remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "}]}";
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());

//				System.out.println("!! RequestMessage start: " + client.answerData); // Send the answer 로 충분
			while(client.answerData == null)
			{			
				Thread.sleep(100);
			}
			
			if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
			{
//				System.out.println("!! RequestMessage : " + client.answerData); // Send the answer 로 충분
				String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
				client.answerData = null;
				// [0] = my_ip, [1]=003, [2]=metadata or none
//					System.out.println("!! RequestMessage : " + req_code + array[1] + array[2]); // Send the answer 로 충분
				if (array[1].equals(req_code))
				{
					result = array[2];
				}
			}
			client.stopWaitingResponse();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String RequestMessage(String req_content, String ip, String req_code, int start, int finish) // real exists in node
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "::" + start + "::" + finish + "}]}";
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());

//				System.out.println("!! RequestMessage start: " + client.answerData); // Send the answer 로 충분
			while(client.answerData == null)
			{			
				Thread.sleep(100);
			}
			
			if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
			{
//				System.out.println("!! RequestMessage : " + client.answerData); // Send the answer 로 충분
				String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
				client.answerData = null;
				// [0] = my_ip, [1]=003, [2]=metadata or none
//					System.out.println("!! RequestMessage : " + req_code + array[1] + array[2]); // Send the answer 로 충분
				if (array[1].equals(req_code))
				{
					result = array[2];
				}
			}
			client.stopWaitingResponse();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String RequestMessage(String req_content, String ip, int data_code) // real exists in node 
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			client.startWaitingResponse();
//			System.out.println("!! RequestMessage2 : " + remote_cmd);
			if(data_code == 1) // read 
			{
				remote_cmd = "{[{REQ::" + ip + "::004::" + req_content + "}]}";
//				System.out.println("!! RequestMessage2 : " + remote_cmd);
				client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());

				if(securityLevel==5 && dataSize>=4)
				{ // == data가 분할되서 들어오는 경우
					boolean isWaiting = true;

					while(isWaiting)
					{
						while(client.answerData == null)
						{			
							Thread.sleep(100);
						}
						
						String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
//						System.out.println("!! RequestMessage : " + array[2] + array[3]);
						if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
						{
								client.answerData = null;			// [0] = my_ip, [1]=003, [2]=metadata or none
//								System.out.println("!! RequestMessage : " +  array[3].equals("1"));
								if (array[1].equals("004") && array[3].equals("1"))
									result = array[5];
								else
									result += array[5];
						}
						if(array[1].equals("004") && array[3].equals(array[2]))
							isWaiting = false;
					}
//					System.out.println("!! RequestMessage : " + result);
					client.stopWaitingResponse();
				}
				else
				{
					while(client.answerData == null)
					{			
						Thread.sleep(100);
					}
					
//					System.out.println("!! Dataprocess - " + client.answerData);
					if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
					{
//							System.out.println("!! RequestMessage2 : " + client.answerData); // Send the answer 로 충분
							String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
							client.answerData = null;
							// [0] = my_ip, [1]=003, [2]=metadata or none
							if (array[1].equals("004"))
							{
//								System.out.println(array[2]);
//								return array[2];
								result = array[5];
							}
					}
					
				}
				
			}
			else if(data_code == 2) // write
			{
				Scanner sc = new Scanner(System.in);
				System.out.print("Want Do You Want to Write ?\t");
				String contents = sc.nextLine();
				
				remote_cmd = "{[{REQ::" + ip + "::005::" + req_content + "::" + contents + "}]}";
				client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
				
				while(client.answerData == null)
				{			
					Thread.sleep(100);
				}
				
//				System.out.println(client.answerData);
//				System.out.println(client.answerData.indexOf("}]}"));
//				System.out.println(client.answerData.length()); // 왜 512가 나오는지 모르겠음.
//				System.out.println(client.answerData.indexOf("{[{ANS"));
				if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
//						System.out.println("!! RequestMessage2 : " + client.answerData); // Send the answer 로 충분
						String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
						client.answerData = null;
						// [0] = my_ip, [1]=003, [2]=metadata or none
						if (array[1].equals("005"))
						{
//							System.out.println(array[2]);
//							return array[2];
							result = array[2];
						}
				}
				
			}
			else if(data_code == 3) // remove
			{
				remote_cmd = "{[{REQ::" + ip + "::006::" + req_content + "}]}";
				client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
				
				while(client.answerData == null)
				{			
					Thread.sleep(100);
				}
				
//				System.out.println(client.answerData);
//				System.out.println(client.answerData.indexOf("}]}"));
//				System.out.println(client.answerData.length()); // 왜 512가 나오는지 모르겠음.
//				System.out.println(client.answerData.indexOf("{[{ANS"));
				if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
//						System.out.println("!! RequestMessage2 : " + client.answerData); // Send the answer 로 충분
						String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
						client.answerData = null;
						// [0] = my_ip, [1]=003, [2]=metadata or none
						if (array[1].equals("006"))
						{
//							System.out.println(array[2]);
//							return array[2];
							result = array[2];
						}
				}
				
			}
			client.stopWaitingResponse();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String RequestMessageKETIRead(String req_content, String ip, String req_code)
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			client.startWaitingResponse();
			
			remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "}]}";
			client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
//			System.out.println("!! RequestMessageRead : " + remote_cmd); //datasize
//			System.out.println("!! RequestMessageRead : " + client.answerData); //datasize
			while(client.answerData == null)
			{			
				Thread.sleep(100);
			}
//			System.out.println("!! RequestMessageRead : " + client.answerData); //datasize
			
			String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");

			if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1 && array[1].equals(req_code))
			{
/*			 //String
				if(array[1].equals("404")) // 기본 양식 맞음
				{
//					System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//					System.out.println("!! RequestMessageRead : " + array[3]); //datasize
					FileWriter fw = null; // 원본 공유 허용 데이터인 경우, 무조건 저장
					fw = new FileWriter(data_folder+"chunk/"+req_content, false);
					fw.write(array[3]);
					fw.close();
					result = "success";
				}
*/
				//Byte
				if(array[1].equals("404")) // 기본 양식 맞음
				{
//					System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//					System.out.println("!! RequestMessageRead : " + array[3].length()); // 4kb보다 높다
//					System.out.println("!! RequestMessageRead 404 : " + array[3].getBytes()); //datasize
		            FileOutputStream fos = new FileOutputStream(data_folder+"chunk/"+req_content);
	                fos.write(array[3].getBytes(), 0, Integer.parseInt(array[2]));
	                fos.flush();
//		            while (array[3].getBytes().length > 0) {
//		                fos.write(array[3].getBytes(), 0, array[3].getBytes().length); 
//		            }      
//	                fos.write(array[3].getBytes(), 0, array[3].length()); //==while
		            fos.close();

					result = "success";
				}
				else if(array[1].equals("444")) // 기본 양식 맞음
				{
//					System.out.println("!! RequestMessageRead : " + array[2]); //datasize

					if(array[2].equals(sha(req_content)))
					{
						System.out.println("* Recieved Data SHA code is the same as original.");
						result = "success";
					}
					else
					{
						System.out.println("* Recieved Data SHA code isn't the same as original.");
						result = "false";
					}
					System.out.println("\tOriginal Data SHA code :\t" + array[2]);
					System.out.println("\tRecieved Data SHA code :\t" + sha(req_content));
				}
			}
			client.answerData = null;
//			System.out.println("!! RequestMessage : " + result);
			client.stopWaitingResponse();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		}
		
		return result;		
	}
	
	public int DeviceInfomation(String ipAddress) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(null, ipAddress, "001");
		
		if(msgs.equals("none"))
			return devcnt;
		
		devcnt = 1;
		
		String device_uuid, device_cpu;
		int device_mem, device_storage, device_net, device_free_storage, device_load, device_mem_usage;
		
		device_uuid = msgs.substring(0,36);
		device_cpu =  msgs.substring(36,36+8);
		if(device_cpu.equals("none    "))
			device_cpu = "none";
		device_mem =  Integer.parseInt(msgs.substring(44,44+2));
		device_storage =  Integer.parseInt(msgs.substring(46,46+4));
//		reserved = msgs.substring(50,50+8);
		device_load =  Integer.parseInt(msgs.substring(58,58+2)); // .getBytes()
		device_mem_usage =  Integer.parseInt(msgs.substring(60,60+2)); // msgs.substring(60,60+2).getBytes(); //Integer.parseInt(msgs.substring(60,60+2));
		device_net =  Integer.parseInt(msgs.substring(62,62+2));
		device_free_storage =  Integer.parseInt(msgs.substring(64,64+4));

		System.out.print("\tdevice information : \n\t\tuuid=" + device_uuid + ",     cpu=" + device_cpu + ",     memory=" + device_mem + "[GB],     storage=" + device_storage);
		System.out.println("[GB],     process load=" + device_load + "[%],     memory usage=" + device_mem_usage + "[%],     network load=" + device_net + "[Mbps],     free storage=" + device_free_storage + "[GB]");
//		System.out.println("\tprocess load=" + new String(device_load) + "[%],     memory usage=" + new String(device_mem_usage) + "[%],     network load=" + device_net + "[Mbps],     free storage=" + device_free_storage + "[MB]");
		
		return devcnt;
	}
	public String MetaDataInfomation(String req_content) // 210428 add int func
	{
		String result = "none";	
//		String sql = select_sql + db_name + " where uuid = '" + message + "'";
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				dataID = metadata.getString("dataid");
				fileType = metadata.getString("file_type");
				if(req_content.equals(dataID))
				{
					timestamp = metadata.getTimestamp("timestamp");
					dataSignature = metadata.getString("data_signature");
					cert = metadata.getString("cert");
					directory = metadata.getString("directory");
					linked_edge = metadata.getString("linked_edge");
					dataType = metadata.getInt("data_type");
					securityLevel = metadata.getInt("security_level"); 
					dataPriority = metadata.getInt("data_priority");
					availabilityPolicy = metadata.getInt("availability_policy"); 
					dataSize = metadata.getLong("data_size");
					result = dataID + "#" + timestamp + "#" + fileType + "#" + dataType + "#" + securityLevel + "#" + dataPriority + "#" + availabilityPolicy + "#" + dataSignature + "#" + cert + "#" + directory + "#" + linked_edge + "#" + dataSize;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return result;
	}
	public int MetaDataInfomation(String req_content, String ipAddress) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! MetaDataInfomation : " + msgs); // Send the answer 로 충분

		if(!msgs.equals("none"))
		{
			devcnt = 1;
			System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//			Thread.sleep(5);
//			System.out.println("!! DataProcess : " + msgs);
			
			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			System.out.println("\tmetadata information :" 
					+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
					+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
					+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize: " + dataSize);
			
		}
		try {
			ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataid='" + dataID + "'");
			int check = 0;
			
			if(dataType == 0)
			{
				dataType = 1;
				linked_edge = ipAddress + ":" + directory;
				directory = null;
			}

			if(!metadata_list.next())
			{
//					check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
				check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
				if(check == 0)
					System.out.println("\tMetaData upload into DataBase : Failure");
				else
					System.out.println("\tMetaData upload into DataBase : Success");
			}
			metadata_list.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return devcnt;	
	}
	public String MetaDataInfomation(String req_content, String ipAddress, int check) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! MetaDataInfomation : " + msgs); // Send the answer 로 충분

		if(check == 1 && !msgs.equals("none"))
		{
			devcnt = 1;
			//System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//				Thread.sleep(5);
//				System.out.println("!! DataProcess : " + msgs);
			
			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			try {
				ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataid='" + dataID + "'");
				
				if(dataType == 0)
				{
					dataType = 1;
					linked_edge = ipAddress + ":" + directory;
					directory = null;
				}

				if(!metadata_list.next())
				{
//						check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
					check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
					if(check == 0)
						System.out.println("\tMetaData upload into DataBase : Failure");
					else
						System.out.println("\tMetaData upload into DataBase : Success");
				}
				metadata_list.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return msgs;	
	}
	
	public int IndividualDataRead(String req_content) // local 검사
	{
		int devcnt = 0;
		String result = "none";	
//		String sql = select_sql + db_name + " where uuid = '" + message + "'";
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				dataID = metadata.getString("dataid");
				fileType = metadata.getString("file_type");
				if(req_content.equals(dataID))
				{
					devcnt = 1;
					timestamp = metadata.getTimestamp("timestamp");
					dataSignature = metadata.getString("data_signature");
					cert = metadata.getString("cert");
					directory = metadata.getString("directory");
					linked_edge = metadata.getString("linked_edge");
//					System.out.println("!! ReceiveWorker : " + directory);
//					System.out.println("!! ReceiveWorker : " + linked_edge);
					
					dataType = metadata.getInt("data_type");
					securityLevel = metadata.getInt("security_level"); 
					dataPriority = metadata.getInt("data_priority");
					availabilityPolicy = metadata.getInt("availability_policy"); 
					dataSize = metadata.getLong("data_size");


					System.out.println("\tmetadata information :" 
							+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
							+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
							+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize: " + dataSize);
				}
			}
			
    		//		String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if(dataType == 0)
    	{
			req_content = dataID + "." + fileType;
    		File f = new File(data_folder + req_content);
			if(f.exists()) {
				devcnt = 2;
				
				System.out.println("\n\tDATA [[\n\t");

				try {
//						System.out.println("!! ReceiveWorker - read : " + data_folder + message);
					BufferedReader br = new BufferedReader(new FileReader(data_folder + req_content));
					while (true) {
						String line = br.readLine();
						if (line == null)
							break;
						System.out.println("\t" + line);
					}
					br.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    		
				System.out.println("\n\t]] DATA\n");
			}
    	}

		return devcnt;
	}
	public int IndividualDataRead(String req_content, String ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(!msgs.equals("none"))
		{
			devcnt = 1;

			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			System.out.println("\tmetadata information :" 
					+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
					+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
					+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize: " + dataSize);
			
		
			if(dataType == 1)
				return devcnt;
			
			req_content = dataID + "." + fileType;
			msgs = RequestMessage(req_content, ipAddress, 1);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3) //
			if(!msgs.equals("none"))
			{
				devcnt = 2;

				if(!ipAddress.equals(local_ip))
				{
					ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataID='" + dataID + "'");
//					System.out.println("!! IndividualDataRead : " + select_sql + db_name + " where uuid='" + uuid + "';");
					if(securityLevel >= 4)
					{
						FileWriter fw = null; // 원본 공유 허용 데이터인 경우, 무조건 저장
						try {
							fw = new FileWriter(data_folder+req_content, false);
							fw.write(msgs);
							fw.flush();
							fw.close();
							dataType = 0;
							directory = data_folder;
							linked_edge = null;

							if(!metadata_list.next())
							{
//								check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
								check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							else
							{
								if(database.delete(dataID)) // 기존 메타데이터가 있으면 삭제하고 새로 업로드
//									check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
									check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							metadata_list.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}						
					}
					else // 링크데이터이면, db에 있는지만 확인하여 없으면 upload
					{
						try {
//							System.out.print("!! " + metadata_list.getString("file_name").equals(req_content));
//							System.out.print("!! " + metadata_list.next()); // 테스트용 - 여기서 한번 하면 if문에서 정상동작 안함
							if(dataType == 0)
							{
								dataType = 1;
								linked_edge = ipAddress + ":" + directory;
								directory = null;
							}

							if(!metadata_list.next())
							{
								check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							metadata_list.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
				}
			}

		}

		return devcnt;
	}

/*
// unit chunk thread
	public ArrayList<String> IndividualDataRead(String req_content, ArrayList<String> ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0, i=0, j=0;
		String msgs ="";
		ArrayList<String> edgeList = new ArrayList<String>();
		ArrayList<String> chunkList = new ArrayList<String>();
		int number_chunk=0;
		int chunk_size = 1; //KB
		double data_split_size = 4; //KB

		
		for(i=0; i<ipAddress.size(); i++)
		{
			msgs=RequestMessage(req_content, ipAddress.get(i), "003");
			if(!msgs.equals("none"))
			{
				String[] array = msgs.split("#");
				dataType = Integer.parseInt(array[3]);
				if(dataType == 0)
				{
					if(edgeList.size() == 0)
					{
						check ++;
						
						dataID = array[0];
						timestamp  = Timestamp.valueOf(array[1]);
						fileType = array[2];
						dataType = Integer.parseInt(array[3]);
						securityLevel = Integer.parseInt(array[4]);
						dataPriority = Integer.parseInt(array[5]);
						availabilityPolicy = Integer.parseInt(array[6]);
						dataSignature = array[7];
						cert = array[8];
						directory = array[9];
						linked_edge = null;
						dataSize = Long.parseLong(array[11]);
						number_chunk = (int)Math.ceil((double)dataSize / (double)chunk_size); // chunk size에 따른 chunk 갯수
						for(j=1; j<=number_chunk; j++)
							chunkList.add(req_content + "." + fileType + "_" + j);
//						System.out.println("!! individual data read : " + chunkList);
					}
					edgeList.add(ipAddress.get(i));
				}
			}
		}
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
		File file = new File(data_folder+"time/"+req_content.replace(fileType, "txt")); // 1. check if the file exists or not boolean isExists = file.exists();
		long start_time;// = System.currentTimeMillis(); // + 32400000;
		long end_time;// = System.currentTimeMillis(); // + 32400000;

		
		String chunk_result="none";
		req_content = dataID + "." + fileType;
		if(check != 0)
		{
			if(securityLevel < 5 || (securityLevel==5 && dataSize<=data_split_size))
			{
//				System.out.println("!! dataprocess - read : " + securityLevel + ", " + dataSize);
				start_time = System.currentTimeMillis(); // + 32400000;
				try {
					FileWriter fw = new FileWriter(file);
					String str = format.format(new Date(start_time));
//						fw.write(req_ip+"\n");
//						fw.flush();
					fw.write(str+"\n");
					fw.flush();
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				for(i=0; i<edgeList.size(); i++)
				{
					msgs = RequestMessage(req_content, edgeList.get(i), 1);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3) //
					if(!msgs.equals("none"))
					{
						if(securityLevel >= 4)
						{
							try {
								FileWriter fw = new FileWriter(data_folder+req_content, false);
								fw.write(msgs);
								fw.flush();
								fw.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}					
							dataType = 0;
							directory = data_folder;
							linked_edge = null;
						}
						else // 링크데이터이면, db에 있는지만 확인하여 없으면 upload
						{
							dataType = 1;
							linked_edge = edgeList.get(i) + ":" + directory;
							directory = null;
						}

						System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
						break;
					}		
				}
				end_time = System.currentTimeMillis(); // + 32400000;
				try {
					FileWriter fw = new FileWriter(file, true);
					String str = format.format(new Date(end_time));
//						fw.write(req_ip+"\n");
//						fw.flush();
					fw.write(str+"\n");
					str = Long.toString(end_time - start_time);
					fw.write("\n"+str+"ms\n");
					fw.flush();
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

//				while(chunk_result.equals("none")) //sha 검증
//				{
////					System.out.print("!! sha test ");
//					chunk_result = RequestMessageKETIRead(req_content, edgeList.get(j), "444"); // chunk read request
//					j++;
//					if((j+1)%edgeList.size() == 0)   //순차적으로 다른 엣지에게  
//						j = 0;
//				}
				chunk_result = RequestMessageKETIRead(req_content, edgeList.get(i), "444"); // chunk read request

//				ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataID='" + dataID + "'");
				
			}
			else // securityLevel==5 && dataSize>4
			{
				///////////////////////////////////////////////////////////////////v201209.3
//				try {
//					PrintWriter fprogress;
//					fprogress = new PrintWriter(data_folder + dataID + ".meta");
//					fprogress.println(securityLevel);  //length
//					//					        long file_length = fileLength(req_content, origin_ip); // 단위?
//					fprogress.println(dataSize);  //length
//					fprogress.println(local_ip);  //length
//					fprogress.close();
//				} catch (FileNotFoundException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
				///////////////////////////////////////////////////////////////////v201209.3
//				System.out.println("!! dataprocess - edgeList : " + edgeList);
				
				Iterator<String> iter = edgeList.iterator();
				//for(String edge : edgeList)
				while(iter.hasNext())
				{
					String edge = iter.next();
					String result = RequestMessage(req_content, edge, "400");  // chunk make request
					if(result.equals("false"))
//						edgeList.remove(edge);
						iter.remove();
//					else
//						System.out.println("dataprocess - chunk split : " + edge);
				}
				System.out.println("\tEdge List with Data Separation Completed : " + edgeList);
				
				UnitChunk[] cd = new UnitChunk[number_chunk]; // thread
				chunk_check = new int[number_chunk]; //thread check
				j=0;
				// for 공인인증 - 나중에는 최초 한번 기록하고, 다시 요청한 경우 skip 하도록 구현하기 

				start_time = System.currentTimeMillis(); // + 32400000;
				try {
					FileWriter fw = new FileWriter(file);
					String str = format.format(new Date(start_time));
//						fw.write(req_ip+"\n");
//						fw.flush();
					fw.write(str+"\n");
					fw.flush();
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(i=0; i<number_chunk; i++, j++)
				{
					chunk_check[i] = 0;
					cd[i] = new UnitChunk(data_folder, chunkList.get(i), edgeList.get(j), "404", fileType, i);
					cd[i].start();
					
					if((j+1)%edgeList.size() == 0)
						j = -1;	
					
//					try { // 1:1 순차 전송
//						cd[i].join();
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}

				// 1:1 순차 전송 X 
				boolean[] working = new boolean[number_chunk+1];
				Arrays.fill(working,true); // working[number_chunk] = true;
				int finish = 0, work_cnt=0;
				j = 0;  //순차적으로 다른 엣지에게 
				while(working[number_chunk])
				{
					//j=0; //요청했던 edge에게 다시 요청
					for(i=0; i<number_chunk; i++) // thread가 끝났는지 검사
					{
						if(chunk_check[i]==2 && working[i])
						{
//							System.out.println("!! chunk read finish : " + chunkList.get(i));
							finish ++;
							working[i] = false;
							cd[i].interrupt();
							try {
								cd[i].join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else if(new File(data_folder+"chunk/"+chunkList.get(i)).exists() && working[i])
						{
//							System.out.println("!! chunk read finish : " + chunkList.get(i));
							finish ++;
							working[i] = false;
						}
					}
					if(finish == number_chunk)
						working[number_chunk] = false;
				}
				
				end_time = System.currentTimeMillis(); // + 32400000;
				try {
					FileWriter fw = new FileWriter(file, true);
					String str = format.format(new Date(end_time));
//						fw.write(req_ip+"\n");
//						fw.flush();
					fw.write(str+"\n");
					str = Long.toString(end_time - start_time);
					fw.write("\n"+str+"ms\n");
					fw.flush();
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				DataMerge(req_content, chunkList, chunk_size*1000);
				
				j=0;
				while(chunk_result.equals("none"))
				{
//					System.out.print("!! sha test ");
					chunk_result = RequestMessageKETIRead(req_content, edgeList.get(j), "444"); // chunk read request
					j++;
					if((j+1)%edgeList.size() == 0)   //순차적으로 다른 엣지에게  
						j = 0;
				}

				dataType = 0;
				directory = data_folder;
				linked_edge = null;
			}
			
			database.delete(dataID);
			check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
			if(check == 0)
				System.out.println("\tMetaData upload into DataBase : Failure");
			else
				System.out.println("\tMetaData upload into DataBase : Success");
			
		}

		return edgeList;
	}
// unit chunk thread
*/


// unit edge thread
	public ArrayList<String> IndividualDataRead(String req_content, ArrayList<String> ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0, i=0, j=0;
		String msgs ="";
		ArrayList<String> edgeList = new ArrayList<String>();
		ArrayList<String> chunkList = new ArrayList<String>();
		int number_chunk=0, edge_size;
		int[] number_request;
		int chunk_size = 1; //KB
		double data_split_size = 4; //KB

		
		for(i=0; i<ipAddress.size(); i++)
		{
			msgs=RequestMessage(req_content, ipAddress.get(i), "003");
			if(!msgs.equals("none"))
			{
				String[] array = msgs.split("#");
				dataType = Integer.parseInt(array[3]);
				if(dataType == 0)
				{
					if(edgeList.size() == 0)
					{
						check ++;
						
						dataID = array[0];
						timestamp  = Timestamp.valueOf(array[1]);
						fileType = array[2];
						dataType = Integer.parseInt(array[3]);
						securityLevel = Integer.parseInt(array[4]);
						dataPriority = Integer.parseInt(array[5]);
						availabilityPolicy = Integer.parseInt(array[6]);
						dataSignature = array[7];
						cert = array[8];
						directory = array[9];
						linked_edge = null;
						dataSize = Long.parseLong(array[11]);
						number_chunk = (int)Math.ceil((double)dataSize / (double)chunk_size); // chunk size에 따른 chunk 갯수
						for(j=1; j<=number_chunk; j++)
							chunkList.add(req_content + "." + fileType + "_" + j);
//							System.out.println("!! individual data read : " + chunkList);
					}
					edgeList.add(ipAddress.get(i));
				}
			}
		}
		
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
		req_content = dataID + "." + fileType;
		if(check != 0)
		{
			if(securityLevel < 5 || (securityLevel==5 && dataSize<=data_split_size))
			{
//					System.out.println("!! dataprocess - read : " + securityLevel + ", " + dataSize);
				for(i=0; i<edgeList.size(); i++)
				{
					msgs = RequestMessage(req_content, edgeList.get(i), 1);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3) //
					if(!msgs.equals("none"))
					{
						File file = new File(data_folder+"time/"+req_content.replace(fileType, "txt")); // 1. check if the file exists or not boolean isExists = file.exists();
						long start_time = System.currentTimeMillis(); // + 32400000;
						try {
							FileWriter fw = new FileWriter(file);
							String str = format.format(new Date(start_time));
//								fw.write(req_ip+"\n");
//								fw.flush();
							fw.write(str+"\n");
							fw.flush();
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if(securityLevel >= 4)
						{
							try {
								FileWriter fw = new FileWriter(data_folder+req_content, false);
								fw.write(msgs);
								fw.flush();
								fw.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}					
							dataType = 0;
							directory = data_folder;
							linked_edge = null;
						}
						else // 링크데이터이면, db에 있는지만 확인하여 없으면 upload
						{
							dataType = 1;
							linked_edge = edgeList.get(i) + ":" + directory;
							directory = null;
						}

						long end_time = System.currentTimeMillis(); // + 32400000;
						try {
							FileWriter fw = new FileWriter(file);
							String str = format.format(new Date(end_time));
//								fw.write(req_ip+"\n");
//								fw.flush();
							fw.write(str+"\n");
							fw.flush();
							str = Long.toString(end_time-start_time);
							fw.write(str+"ms\n");
							fw.flush();
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
						break;
					}		
				}
//					ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataID='" + dataID + "'");
				
				database.delete(dataID);
				check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
				if(check == 0)
					System.out.println("\tMetaData upload into DataBase : Failure");
				else
					System.out.println("\tMetaData upload into DataBase : Success");
			}
			else // securityLevel==5 && dataSize>4
			{
				///////////////////////////////////////////////////////////////////v201209.3
				try {
					PrintWriter fprogress;
					fprogress = new PrintWriter(data_folder + dataID + ".meta");
					fprogress.println(securityLevel);  //length
					//					        long file_length = fileLength(req_content, origin_ip); // 단위?
					fprogress.println(dataSize);  //length
					fprogress.println(local_ip);  //length
					fprogress.close();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				///////////////////////////////////////////////////////////////////v201209.3
//					System.out.println("!! dataprocess - edgeList : " + edgeList);
				
//				Iterator<String> iter = edgeList.iterator();
//				//for(String edge : edgeList)
//				while(iter.hasNext())
//				{
//					String edge = iter.next();
//					String result = RequestMessage(req_content, edge, "400");  // chunk make request
//					if(result.equals("false"))
////							edgeList.remove(edge);
//						iter.remove();
////						else
////							System.out.println("dataprocess - chunk split : " + edge);
//				}
//				System.out.println("\tEdge List with Data Separation Completed : " + edgeList);
				edge_size = edgeList.size();
				// chunk request #0
				do
				{
					number_request = new int[edge_size];
					Arrays.fill(number_request, (int)(number_chunk/edge_size)); // working[number_chunk] = true;
					
					if(number_chunk % edge_size != 0)
					{
	//					number_request[i] += number_chunk % edge_size ; // 엣지 갯수로 데이터 조각이 나누어 떨어지지 않으면, 잔여조각만큼 마스터에 추가 요청
						for(i=0; i<number_chunk%edge_size; i++)
							number_request[i] ++; // 잔여 조각 갯수만큼 엣지들에게 추가 요청
					}
	
					Iterator<String> iter = edgeList.iterator();
					i = 0;
					j = 1;
					while(iter.hasNext())
					{
						String edge = iter.next();
						String result = RequestMessage(req_content, edge, "401", j, j+number_request[i] );  // chunk make request
						j += number_request[i];
						i++;
						if(result.equals("false"))
							iter.remove();
					}
					
					if(edgeList.size() == 0)
						return edgeList;
				}while(edge_size != edgeList.size());
				System.out.println(" * Edge List with Data Separation Completed : " + edgeList);
				
				// chunk request #1
				UnitEdge[] edge_th = new UnitEdge[edge_size]; // thread
				chunk_check = new int[edge_size]; //thread check
				for(i=0, j=1; i<edge_size; i++)
				{
//						System.out.println("!! chunk read : " + i + ", " + j);
//						System.out.println("!! chunk read : " + chunkList.get(i));
		
// thread			
					chunk_check[i] = 0;
					// chunk request #2
					edge_th[i] = new UnitEdge(data_folder, req_content, edgeList.get(i), "405", fileType, i, j, j+number_request[i]); //
					edge_th[i].start();
					j += number_request[i];
				}
				
//				try { // for 공인인증
//					Thread.sleep(number_chunk);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} // for 공인인증
				
				boolean[] working = new boolean[number_chunk+1];
				Arrays.fill(working,true); // working[number_chunk] = true;
				int finisher = 0, edge_number=0;
				i = 0;  //순차적으로 다른 엣지에게 
				while(working[number_chunk])
				{
					//j=0; //요청했던 edge에게 다시 요청
					for(j=0; j<number_chunk; j++) // thread가 끝났는지 검사
					{
						if(new File(data_folder+"chunk/"+chunkList.get(j)).exists() && working[j])
						{
							finisher ++;
							working[j] = false;
						}
					}
					if(finisher == number_chunk)
						working[number_chunk] = false;
				}
				
//				try {
//					Thread.sleep(number_chunk);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				DataMerge(req_content, chunkList, chunk_size*1000);
				
				j=0;
				String chunk_result="none";
				while(chunk_result.equals("none"))
				{
//					System.out.print("!! sha test ");
					chunk_result = RequestMessageKETIRead(req_content, edgeList.get(j), "444"); // chunk read request
					j++;
					if((j+1)%edgeList.size() == 0)   //순차적으로 다른 엣지에게  
						j = 0;
				}

				dataType = 0;
				directory = data_folder;
				linked_edge = null;
				
				if(chunk_result.equals("success"))
				{
					database.delete(dataID);
					check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
					if(check == 0)
						System.out.println(" * MetaData upload into DataBase : Failure");
					else
						System.out.println(" * MetaData upload into DataBase : Success");
				}
			}

			
		}
		

		return edgeList;
	}
// unit edge thread

	
	public int IndividualDataWrite(String req_content, String ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(!msgs.equals("none"))
		{
			devcnt = 1;
//			System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//			Thread.sleep(5);

			// 기존 메타데이
//			uuid_file = req_content;
//			uuid = msgs.substring(0,36);
//			d_length = Integer.parseInt(msgs.substring(36,39));//.getBytes()[0] & 0xff;
//			location = descriptor = msgs.substring(39,39+d_length);
//			datasize = Integer.parseInt(msgs.substring(39+d_length,43+d_length));//.getBytes()[0] & 0xff;
//			security =  Integer.parseInt(msgs.substring(43+d_length, 44+d_length));
//			datatype =  Integer.parseInt(msgs.substring(44+d_length, 45+d_length));
//			System.out.println("\tmetadata information : uuid=" + uuid + ",     location=" + location + ",     data_size=" + datasize + ",     security_level=" + security + ",     data_type=" + datatype);
//			
//			if(datatype == 1)
//				return devcnt;
			// 기존 메타데이터
			
			
			msgs = RequestMessage(req_content, ipAddress, 2);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3)
//			System.out.println(msgs);
			if(msgs.equals("permission"))
			{
				devcnt = 2;
				System.out.println("\tIt is Possible to Write the DATA\n");
			}
			else if(msgs.equals("failure"))
			{
				System.out.println("\tIt is ImPossible to Write the DATA\n");
				
			}


//			if(!msgs.equals("none"))
//				System.out.println("\tAnswer : " + ipAddress + " has data.");
//			else
//				System.out.println("\tAnswer : doesn't had DATA."); // Send the answer 로 충분
		}
//		else
//		{
//			System.out.println("\tAnswer : doesn't had DATA."); // Send the answer 로 충분
//		}
		return devcnt;
	}

	public int IndividualDataRemove(String req_content, String ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(!msgs.equals("none"))
		{
			devcnt = 1;
//			System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//			Thread.sleep(5);

			// 기존 메타데이터
//			uuid_file = req_content;
//			uuid = msgs.substring(0,36);
//			d_length = Integer.parseInt(msgs.substring(36,39));//.getBytes()[0] & 0xff;
//			location = descriptor = msgs.substring(39,39+d_length);
//			datasize = Integer.parseInt(msgs.substring(39+d_length,43+d_length));//.getBytes()[0] & 0xff;
//			security =  Integer.parseInt(msgs.substring(43+d_length, 44+d_length));
//			datatype =  Integer.parseInt(msgs.substring(44+d_length, 45+d_length));
//			System.out.println("\tmetadata information : uuid=" + uuid + ",     location=" + location + ",     data_size=" + datasize + ",     security_level=" + security + ",     data_type=" + datatype);
//			
//			if(datatype == 1)
//				return devcnt;
			// 기존 메타데이터
			
			msgs = RequestMessage(req_content, ipAddress, 3);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3)
//			System.out.println("\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
//			if(!msgs.equals(null))
//				devcnt = 2;
			if(msgs.equals("permission"))
			{
				devcnt = 2;
				System.out.println("\tIt is Possible to Remove the DATA\n");
			}
			else if(msgs.equals("failure"))
			{
				System.out.println("\tIt is ImPossible to Remove the DATA\n");
				
			}
		}
		return devcnt;
	}

	public static void DataMerge(String newFileName, ArrayList<String> filenames, int chunk_size){

		FileOutputStream fout = null;
		FileInputStream in = null;
		BufferedInputStream bis = null;
		try {
			File newFile = new File(data_folder + newFileName);
//			System.out.println("!! data mergy : " + data_folder+newFileName);
//			System.out.println("!! data mergy : " + filenames);
			fout = new FileOutputStream(newFile);

			for (String fileName : filenames) {
//				System.out.println(fileName);
				File splittedFile = new File(data_folder + "chunk/" + fileName);
				while(!splittedFile.exists())
				{
					Thread.sleep(10);
				}
				if(splittedFile.exists())
				{
					in = new FileInputStream(splittedFile);
					bis = new BufferedInputStream(in);
					int len = 0;
					byte[] buf = new byte[chunk_size];
					while ((len = bis.read(buf, 0, chunk_size)) != -1) {
//						System.out.println(new String(buf));
						fout.write(buf, 0, len);
						fout.flush();
					}
//					splittedFile.delete();
					bis.close();
					in.close();
				}
			}
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	public static String chunk_sha(String filepath) throws Exception{
        File file = new File(data_folder+filepath);
        for(int i=0; i<5; i++)
        {
            if(!file.exists())
            {
            	Thread.sleep(100);
            }
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
	
	private static String finish_cmd="finish";
	public ResultSet rs = null;
/*	
	public Connection conn = null;
	public Statement state = null; 
	public PreparedStatement pstate = null;
	// rs.absolute(low_index);
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
*/	
//	public String uuid_file="", uuid="", location="", origin_ip="", descriptor="";
//	public int security=-1, datatype=-1, d_length=-1, datasize=-1;
	static String dataID, fileType, dataSignature, cert, directory, linked_edge;	
	static Timestamp timestamp;
	static int dataType, securityLevel, dataPriority, availabilityPolicy, partSize;
	static long dataSize;

	static String select_sql = "SELECT * FROM "; //file_management"; //(file_name, uuid, security, sharing, location)
	static String table_name = "file_management"; //(file_name, uuid, security, sharing, location)
//	public static String origin_foldername = "/home/keti/data/";
	public static String data_folder = "/home/keti/data/";
	public static String cert_folder = "/home/keti/data/";
	public static String local_ip = "localhost";
	public static Database database = null;
	static int ketiCommPort = 5679; // KETI 내부통신
	public int[] chunk_check, chunk_edge;
//	public String[] chunk;
	public static int maxRetry = 10;
	
	class UnitChunk extends Thread
	{
//		public static String foldername="/data/";
		public String req_content, req_ip, req_code, file_type;
		int th_id;
		
		UnitChunk(String fd, String content, String ip, String code, String type, int id)
		{
			data_folder = fd;
			req_content = content;
			req_ip = ip;
			req_code = code;
			file_type = type;
			th_id = id;
		}
		
		public void run() // 동기화 synchronized - 소용없음
		{
//			synchronized (this) // 지역 동기화 - 소용없음
			while(chunk_check[th_id] == 0)
			{ 
		        EdgeDeviceInfoClient client = null;
				String remote_cmd="";
		
				try {
					// for 공인인증 - 데이터 공유 시작/종료 시간 파일 작성
					for(int i=0; i<maxRetry; i++)
					{
						try
						{
							client = new EdgeDeviceInfoClient(req_ip, EdgeDeviceInfoClient.socketTCP);
							break;
						} catch (Exception e)
						{	
							if(i == maxRetry-1)
							{
//								System.out.println("!! error : client create : " + th_id+1);
								client.stopWaitingResponse();
								chunk_check[th_id] = 1;
								return ;								
							}
						}
					}
					client.startWaitingResponse();
					int work_cnt = 0;
					
					remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "}]}";
					client.answerData = null;
//					System.out.println("RequestMessage - chunk : " + remote_cmd); //datasize
					client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
		
					while(client.answerData == null)
					{			
//						System.out.println("!! chunk request thread : " + th_id);
						if(chunk_check[th_id] != 0) // 수신 대기 중, 다른 thread에서 수신완료한 경우
						{
//							System.out.println("!! error : client not need : " + th_id+1);
							client.stopWaitingResponse();
							return ;
						}
//						else if(++work_cnt%(maxRetry*5)==0 && chunk_check[th_id]==0)
//						{
////							System.out.println("!! chunk waiting : " + req_content);
//							client.answerData = null;
//							client.stopWaitingResponse();
//							chunk_check[th_id] = 1;
//							return ;
//						}
						else
							Thread.sleep(10); // test 필요
					}
					if(client.answerData.equals("retry"))
					{
						System.out.println("error : client retry : " + th_id+1);
						client.answerData = null;
						client.stopWaitingResponse();
						chunk_check[th_id] = 1;
						return ;								
					}
					
					if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
					{
//						System.out.println("!! RequestMessageRead : " + client.answerData); //
						String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
						if(!array[1].equals(req_code))
						{
							System.out.println("error : client retry : " + th_id+1);
							client.answerData = null;
							client.stopWaitingResponse();
							chunk_check[th_id] = 1;
							return ;								
						}
						client.answerData = null;
//						System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//						System.out.println("!! RequestMessageRead : " + array[3]); //datasize
						String[] chunk_array = req_content.split("_");
						if(!array[2].equals(chunk_array[1])) // chunk 프로토콜 규약
						{
//							System.out.println("!! chunk request : " + chunk_array[1] + ", but receive : " + array[4]);
							chunk_check[th_id] = 1;
//							chunk_edge[th_id] = chunk_edge[Integer.parseInt(array[4])-1]; //대신 저장한 chunk
							th_id = Integer.parseInt(array[2]) - 1; // chunk 프로토콜 규약
//				            chunk[th_id] = array[3];
							req_content = chunk_array[0] + "_" + array[2]; // 데이터 thread가 엉키는건가? 엉뚱한 thread에서 수신  // chunk 프로토콜 규약
						}
			            
//						if(req_content.equals("195240aa-92e4-40fd-8014-12033767f351.csv_307"))
//						{
//							System.out.println("!! RequestMessageRead : " + client.answerData); //datasize
//							System.exit(0);
//						}
						File file = new File(data_folder+"chunk/"+req_content);
			            FileOutputStream fos = new FileOutputStream(file);
//			            array[3] = array[3] + "";
			            fos.write(array[4].getBytes("UTF-8"), 0, Integer.parseInt(array[3])); //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
//			            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
			            fos.flush();
			            fos.close();
			            
//						try { // for 공인인증 - interrupted error
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						chunk_check} // for 공인인증

					}
					else
						chunk_check[th_id] = 1;
					
			//			System.out.println("!! RequestMessage : " + result);
					client.stopWaitingResponse();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				chunk_check[th_id] = 2;
			}
		}
		// UnitChunk Thread
	}
	
	// UnitEdge Thread
	class UnitEdge extends Thread
	{
		public String req_content, req_ip, req_code, file_type;
		int index_s, index_f, th_id;
		
		UnitEdge(String fd, String content, String ip, String code, String type, int id, int start, int finish)
		{
			data_folder = fd;
			req_content = content;
			req_ip = ip;
			req_code = code;
			file_type = type;
			th_id = id;
			index_s = start;
			index_f = finish;
		}
		
		public void run() // 동기화 synchronized - 소용없음
		{
			// chunk request #2
//			while(chunk_check[th_id] == 0)
//			{ 
		        EdgeDeviceInfoClient client = null;
				String remote_cmd="";
		
				try {
					// for 공인인증 - 데이터 공유 시작/종료 시간 파일 작성
					for(int i=0; i<maxRetry; i++)
					{
						try
						{
							client = new EdgeDeviceInfoClient(req_ip, EdgeDeviceInfoClient.socketTCP);
							break;
						} catch (Exception e)
						{	
							if(i == maxRetry-1)
							{
//								System.out.println("!! error : client create : " + th_id+1);
								client.stopWaitingResponse();
								chunk_check[th_id] = -1;
								return ;								
							}
						}
					}
					client.startWaitingResponse();
					int work_cnt = 0;
					
					remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "::" + index_s + "::" + index_f + "}]}";
					client.answerData = null;
					System.out.println("Request to : " + req_ip + ", chunk #"+ index_s + " to #" + (index_f-1)); //datasize
//					System.out.println("RequestMessage - chunk : " + remote_cmd); //datasize
					client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
					long start_time = System.currentTimeMillis(); // + 32400000;
					for(int i=index_s; i<index_f; i++)
					{
						try { // 요청 시간 작성
							File file = new File(data_folder+"time/request_"+req_content.replace(fileType, "txt")+"_"+Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
//							if(!file.exists()) // 데이터를 2번 요청 할 수도 있음 - sha 결과가 달라서
//							{
//							}
							FileWriter fw = new FileWriter(file);
							String str = format.format(new Date(start_time));
//								fw.write(req_ip+"\n");
//								fw.flush();
							fw.write(str+"\n");
							fw.flush();
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// chunk request #7 - sha 수신
					boolean[] check_file = new boolean[index_f-index_s];
					Arrays.fill(check_file,true); // working[number_chunk] = true;
					do
					{
						while(client.answerData == null)
						{
							Thread.sleep(10); // test 필요
							if(chunk_check[th_id] >= index_f-index_s) // 수신 대기 중, 다른 thread에서 수신완료한 경우
							{
								System.out.println("!! error : client not need : " + chunk_check[th_id]);
								client.stopWaitingResponse();
								return ;
							}
//							else if(++work_cnt%(maxRetry*1)==0 && chunk_check[th_id]==0)
//							{
////								System.out.println("!! chunk waiting : " + req_content);
//								client.answerData = null;
//								client.stopWaitingResponse();
//								chunk_check[th_id] = 1;
//								return ;
//							}
						}
						
						if(client.answerData.indexOf("{[{ANS")==0 && client.answerData.indexOf("}]}")!=-1) // 기본 양식 맞음
						{
//							System.out.println("!! RequestMessageRead : " + client.answerData); //
							String[] array = client.answerData.substring(8, client.answerData.indexOf("}]}")).split("::");
							client.answerData = null;
							if(!array[1].equals(req_code))
							{
								continue;
							}
//							System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//							System.out.println("!! RequestMessageRead : " + array[3]); //datasize
							
							
//							if
							if(array[2].equals("sha")) //sha 검사하기
							{
								Thread.sleep(100);
								for(int i=index_s; i<index_f; i++)
					            {
					            	if(check_file[i-index_s])
					            	{
//										System.out.println("* Original Data SHA code -\t" + array[3+i-index_s]);
										long end_time = System.currentTimeMillis(); // + 32400000;
										String str = chunk_sha("chunk/" + req_content + "_" + Integer.toString(i));
//										System.out.println("* Recieved Data SHA code -\t" + str);
										if(array[3+i-index_s].equals(str))
										{
											File file = new File(data_folder+"time/request_"+req_content.replace(fileType, "txt") + "_" + Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
											// 수신 완료 시간 작성

											try {
												FileWriter fw = new FileWriter(file, true);
//													fw.write(req_ip+"\n");
//													fw.flush();
												fw.write(format.format(new Date(end_time))+"\n");
												fw.flush();
												fw.write(str+"\n"); //SHA 기록
												fw.flush();
												fw.close();
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
								            check_file[i-index_s] = false;
								            chunk_check[th_id] ++;
										}
										else
										{ // SHA가 다르면 재요청 - 
//											System.out.println("\tRecieved Chunk #" + Integer.toString(i) + " SHA code isn't the same as original : retry");
//											File file = new File(data_folder+"chunk/"+req_content + "_" + Integer.toString(i));
//											file.delete();
											client.answerData = null;
											remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "::" + Integer.toString(i) + "::" + Integer.toString(i+1) + "}]}";
											client.sendPacket(remote_cmd.getBytes(), remote_cmd.length());
											Thread.sleep(100); // test 필요

											i --;
											continue;
										}
					            		
					            	}
					            }
					            break;
							}
//							else
//							{
//								String chunk_content = req_content + "_" + array[2]; // chunk 프로토콜 규약
//								System.out.println("!! RequestMessageRead : " + chunk_content); //
//								File file = new File(data_folder+"chunk/"+chunk_content);
//					            FileOutputStream fos = new FileOutputStream(file);
////					            array[3] = array[3] + "";
//					            fos.write(array[4].getBytes("UTF-8"), 0, Integer.parseInt(array[3])); //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
////					            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
//					            fos.flush();
//					            fos.close();
//							}
						}
						
					}while(true);
//					System.out.println("!! RequestMessageRead exit : " + th_id); //datasize
					
					client.stopWaitingResponse();
//					System.out.println("!! RequestMessageRead : " + chunk_check[th_id]); //
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//		}
		
	} // UnitEdge Thread
}




