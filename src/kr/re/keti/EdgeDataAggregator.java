/*
 * EdgeDataAggregator = main 
 * DataProcess = file handle
 * EdgeFinder = master existence and nonexistence check
 * EdgeReceptor = when node is master, slave wait.
 * ReceiveWorker = node wait to request and confirm what kind of request
 * TransmitWorker = confirm user request and transmit to others 
 * ShellCommander = X
 *  
 */
package kr.re.keti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
// basic
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class EdgeDataAggregator {
	public static void EdgeInformation()
	{
		System.out.println("==================================================================");
		FileReader file = null;
		BufferedReader br = null;
		try {
			file = new FileReader("info_device.txt");
			br = new BufferedReader(file); // path - 실제 데이터가 있는 기본 폴더 위치 //
//			String line = null;
//			while (true) { line = br.readLine(); if (line == null) break; }
			dev_uuid = br.readLine();
			if (dev_uuid == null)
			{
				System.out.println(" * Input the UUID of Edge Device.");
				System.exit(0);
			}
			else
				System.out.println(" * UUID of Edge Device. : " + dev_uuid);
			
			data_folder = br.readLine();
			if (data_folder == null)
			{
				System.out.println(" * Input the Name of Main Path with data.");
				System.exit(0);
			}
			else
			{
				System.out.println(" * Name of Main Path with data : " + data_folder);
				File folder = new File (data_folder);
				if(!folder.exists())
				{
//					System.out.println("!! receive 007 cert mkdir");
					folder.mkdir();
				}
				folder = new File (data_folder+"chunk");
				if(!folder.exists())
				{
//					System.out.println("!! receive 007 cert mkdir");
					folder.mkdir();
				}
				folder = new File (data_folder+"time");
				if(!folder.exists())
				{
//					System.out.println("!! receive 007 cert mkdir");
					folder.mkdir();
				}
			}
			cert_folder = br.readLine();
			if (cert_folder == null)
			{
				System.out.println(" * Input the Name of Main Path with cert.");
				System.exit(0);
			}
			else
			{
				System.out.println(" * Name of Main Path with cert : " + cert_folder);
				File folder = new File (cert_folder);
				if(!folder.exists())
				{
//					System.out.println("!! receive 007 cert mkdir");
					folder.mkdir();
				}
			}
//			br.close();
//			file.close();

			// 사용하고 있는 DB종류
//			file = new FileReader("info_db.txt");
//			br = new BufferedReader(file); // 사용중인 DB 종류 
			whatDB = br.readLine();
			System.out.println(" * DBMS used by EdgeNode : " + whatDB);
			if(whatDB.equals("MySQL"))
			{
				String line = br.readLine();
				String[] db_info = line.split(",");
				if (db_info.length != 4)
				{
					System.out.println(" * Input DB Infomation in info_device.txt(ex:DB name,table name,user ID,user PW).");
					System.exit(0);
				}
				db_name = db_info[0];
				table_name = db_info[1];
				user_id = db_info[2];
				user_pw = db_info[3];
//				System.out.println("!! egdeinfo : " + db_name);
				
				dataprocess = Database.getInstance(Database.DB_MySQL);
//				dataprocess.connectDB(foldername, DBpath, db_name);
				dataprocess.connectDB(data_folder, db_path, db_name, table_name, user_id, user_pw);
			}
			else if(whatDB.equals("SQLite"))
			{
				String line = br.readLine();
				String[] db_info = line.split(",");
				if (db_info.length != 5)
				{
					System.out.println(" * Input DB Infomation in info_device.txt(ex:DB name,table name,user ID,user PW,DB path).");
					System.exit(0);
				}
				db_name = db_info[0];
				table_name = db_info[1];
				user_id = db_info[2];
				user_pw = db_info[3];
				db_path = db_info[4];

				dataprocess = Database.getInstance(Database.DB_SQLITE);
//				dataprocess.connectDB(foldername, DBpath, db_name);
				dataprocess.connectDB(data_folder, db_path, db_name, table_name, user_id, user_pw);
			}
			else
			{
				System.out.println(" * Input DBMS used by EdgeNode(ex:MySQL, SQLite).");
				System.exit(0);
			}
//			br.close();
//			file.close();
			
//			file = new FileReader("info_upnp.txt");
//			br = new BufferedReader(file); //upnp or manual mode 
			upnp_mode = br.readLine();
			if(upnp_mode.equals("slave"))
			{
				master_ip = br.readLine();
			}
			else if(!upnp_mode.equals("master") && !upnp_mode.equals("upnp"))
			{
				System.out.println(" * Input UPnP mode(ex:upnp, master, slave&master_ip).");
				System.exit(0);
			}
			
			deviceIP = br.readLine();
//			System.out.println("!! EdgeInformation - " + currentIPAddrStr);
			if(deviceIP.equals("keties.iptime.org") || deviceIP.equals("keti.d-sharing.kr"))
				currentIPAddrStr = InetAddress.getByName(deviceIP).getHostAddress(); // ex=\10.0.7.11
			else if(deviceIP.equals("auto"))
			{
				if (currentIPAddr == null)  // discover ip address
					currentIPAddrStr = InetAddress.getLocalHost().getHostAddress();
				else 
					currentIPAddrStr = currentIPAddr.getHostAddress();
			}
			else
				currentIPAddrStr = deviceIP;
//			System.out.println("!! EdgeInformation - " + currentIPAddr);
			System.out.println(" * IP Address of Edge Device : " + currentIPAddrStr + "\n");
			
			br.close();
			file.close();
		} catch (FileNotFoundException e) {
//			System.out.println(e);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void UPnPmode(EdgeFinder finder)
	{
		if (finder.discoverMaster()) 
		{
			// I'm a slave.
			master_ip = finder.getMasterAddress().getHostAddress();
			FileWriter fw = null;
			try {
				fw = new FileWriter("edge_ipList.txt", false);
				fw.write("slave\n");
				fw.flush();
				fw.write(master_ip);
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			Runnable receiver = new ReceiveWorker(foldername, whatDB); // v1,2			// implements Runnable
//			System.out.println("!! egdeinfo : " + db_name);

//			Runnable receiver = new ReceiveWorker(foldername, dataprocess, dev_uuid, db_name, currentIPAddrStr);
//			Thread receiver_th = new Thread(receiver);

			if(deviceIP.equals("auto"))
				receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw);
			else
				receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw, deviceIP);
			receiver_th = new Thread(receiver);
			receiver_th.start();
			// System.out.println(Thread.currentThread().getName());
			// implements Runnable
//			slave_to_master_transmission();

			System.out.println("* Master found: " + master_ip + "\n");
//			SlaveWorker slave = new SlaveWorker(master_ip, foldername, whatDB); // v1,2
			SlaveWorker slave = new SlaveWorker(master_ip, data_folder, cert_folder, dataprocess, currentIPAddrStr, table_name, dev_uuid);//			; // new TransmitWorker(slaveList, foldername);
			Thread slave_th = new Thread(slave);//			; // new Thread(master);
			slave_th.start();
		} 
		else 
		{
			master_ip = currentIPAddrStr;
			// I'm the master!
			FileWriter fw = null;
			try {
				fw = new FileWriter("edge_ipList.txt", false);
				fw.write("master\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			receptor = new EdgeReceptor(null);
			receptor = new EdgeReceptor((currentIPAddr == null) ? (null) : (currentIPAddrStr));
//			receiver = new ReceiveWorker(foldername, whatDB);
//			receiver = new ReceiveWorker(foldername, dataprocess, dev_uuid, db_name, currentIPAddrStr);
			if(deviceIP.equals("auto"))
				receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw);
			else
				receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw, deviceIP);
			receiver_th = new Thread(receiver);

			System.out.println("Waiting for connections from slaves...");
			if (master == null) // test need
			{
//				System.out.println("test2");
//				master = new MasterWorker(master_ip, foldername, whatDB); // v1,2
				master = new MasterWorker(master_ip, data_folder, cert_folder, dataprocess, table_name, dev_uuid);
				master_th = new Thread(master);
				master_th.start();
//				master.start();
			}

			EdgeReceptor.ReceptionEvent rEvent = new EdgeReceptor.ReceptionEvent() 
			{
				private String msg = "";

				@Override
				public void handler(InetAddress addr) // when slave contact
				{
					String slaveAddr = addr.getHostAddress(); // now slave address
					if (!slaveList.contains(slaveAddr)) // first contact
					{
						FileWriter fw = null;
						try {
							fw = new FileWriter("edge_ipList.txt", false);
							fw.write("master\n");
							fw.flush();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						/*
						 * if(slaveList.size() > 0) { System.out.println("input the end ");
						 * master.threadStop(true); master.interrupt(); }
						 */
						slaveList.add(slaveAddr);
						/*
						 * try { final String os_version = System.getProperty("os.name");
						 * 
						 * if (os_version.contains("Windows")) { Runtime.getRuntime().exec("cls"); }
						 * else { Runtime.getRuntime().exec("clear"); } } catch (final Exception e) { //
						 * Handle any exceptions. }
						 */
						SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nowtime = new Date();
						String logtime = timeformat.format(nowtime);

//						String message = "* slave list *\n";
						for (int i = 0; i < slaveList.size(); ++i) 
						{
//			              message += slaveList.get(i) + "\n";
							// System.out.print("\t" + logtime + ":" + slaveList.get(i)+"\n");
							try {
								fw.write(slaveList.get(i) + "\n");
								fw.flush();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						try {
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						System.out.println("\n* Slave List");
//						if (!msg.equals(""))
//							System.out.println(msg);
//						System.out.println("\t" + logtime + " : " + slaveList.get(slaveList.size() - 1) + " : new");
						msg += "\t" + logtime + " : " + slaveList.get(slaveList.size() - 1);
						System.out.printf(msg + " : new");
						msg += "\n";
						// JOptionPane.showMessageDialog(null, message); //

//						if(master == null) // test need
//						{
//							master = new MasterWorker(master_ip, foldername);
//							master_th = new Thread(master); 
//							master_th.start();
////							master.start();
//						}
//						master_to_slave_transmission(slaveList);
//						else
						master.slaveSetting(slaveList);
						receiver.slaveSetting(slaveList);
						edgeList = (ArrayList<String>)slaveList.clone();
						edgeList.add(0, master_ip);
						receiver.edgeListadd(edgeList);


					}
//					else
//					{
//						System.out.println("\n* Slave List");
//						System.out.print(msg);
//						System.out.println("\tSlave(" + slaveAddr + ") reconnect in Edge Network.");
//					}

				}

				private ArrayList<String> slaveList = new ArrayList<String>();
				private ArrayList<String> edgeList = new ArrayList<String>();
			};

			receptor.setEventHandler(rEvent);
			receptor.start();
			receiver_th.start();

			try {
				receptor.join();
				receiver_th.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void MasterMode(EdgeFinder finder)
	{
//        BufferedReader read = new BufferedReader(new InputStreamReader(is));
//        String message = read.readLine();

		master_ip = currentIPAddrStr;
		// I'm the master!
		FileWriter fw = null;
		try {
			fw = new FileWriter("edge_ipList.txt", false);
			fw.write("master\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(deviceIP.equals("auto"))
			receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw);
		else
			receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw, deviceIP);
		receiver_th = new Thread(receiver);
		receiver_th.start();

		if (master == null) // test need
		{
			master = new MasterWorker(master_ip, data_folder, cert_folder, dataprocess, table_name, dev_uuid);
			master_th = new Thread(master); 
			master_th.start();
		}
//		receiver = new ReceiveWorker(foldername, dataprocess, dev_uuid, db_name, currentIPAddrStr);

		System.out.println("Waiting for connections from slaves...");
		
		ServerSocket ss = null;
		Socket cs = null;
		InputStream is = null;
		final int defaultBackLog = 3000; // eunae 


		ArrayList<String> slaveList = new ArrayList<String>();
		ArrayList<String> edgeList = new ArrayList<String>();
		byte[] data = new byte[100];
		int n=0;
		String msg = "";
		while(true)
		{
//			System.out.println("!! master socket open");	
			try {
				ss = new ServerSocket(defaultManualPort, defaultBackLog);
				cs = ss.accept();
				is = cs.getInputStream();
				n = is.read(data);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(n > 0)
			{
				String message = new String(data, 0, n);
				String[] array = message.split("::");
//				System.out.println("test");
				if (array[0].equals("{[{REQ") && array[2].equals("001") && !array[3].equals("DEV_STATUS") && !array[3].equals("SLAVE_LIST"))
				{
//					receptionEvent.
					String[] slave_ip = array[3].split("}]}");
					if (!slaveList.contains(slave_ip[0]))
					{
						slaveList.add(slave_ip[0]);

						SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nowtime = new Date();
						String logtime = timeformat.format(nowtime);

						System.out.println("\n* Slave List");
//						if (!msg.equals(""))
//							System.out.println(msg);
//						System.out.println("\t" + logtime + " : " + slaveList.get(slaveList.size() - 1) + " : new");
						msg += "\t" + logtime + " : " + slaveList.get(slaveList.size() - 1);
						System.out.printf(msg + " : new");
						msg += "\n";
						
//						String msg = "New EdgeNode : " + logtime + " : " + slaveList.get(slaveList.size() - 1);
//						System.out.println("\n" + msg);
					}
//					else
//					{
//						System.out.println("\n* Slave List");
//						System.out.print(msg);
//						System.out.println("\tSlave(" + slave_ip[0] + ") reconnect in Edge Network.");
//					}	
					//
//					System.out.println("* Slave list");
					try {
						fw = new FileWriter("edge_ipList.txt", false);
						fw.write("master\n");
						fw.flush();
						for (int i = 0; i < slaveList.size(); ++i)
						{
							fw.write(slaveList.get(i) + "\n");
							fw.flush();
//							System.out.print("\t" + slaveList.get(i));
						}
						fw.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					master.slaveSetting(slaveList);
					receiver.slaveSetting(slaveList);
					edgeList = (ArrayList<String>)slaveList.clone();
					edgeList.add(0, master_ip);
					receiver.edgeListadd(edgeList);
				}
			}
			try {
//				System.out.println(n);
				is.close();
				cs.close();
				ss.close();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
//		receiver_th.join();
	}
	public static void SlaveMode(EdgeFinder finder)
	{
		Socket socket;
		OutputStream os;
		try {
			socket = new Socket(master_ip, defaultManualPort);
			os = socket.getOutputStream();
			String remote_cmd = "{[{REQ::" + master_ip + "::001::" + currentIPAddrStr + "}]}"; // slave add request
			os.write(remote_cmd.getBytes());
			os.flush();
			os.close();
			socket.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		FileWriter fw = null;
		try {
			fw = new FileWriter("edge_ipList.txt", false);
			fw.write("slave\n");
			fw.flush();
			fw.write(master_ip);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// implements Runnable
//		Runnable receiver = new ReceiveWorker(foldername, whatDB); // v1,2
		//(String ip, String fname, Database dp, String dev_uuid, String dbname, String tablename, String userid, String userpw)
//		Runnable receiver = new ReceiveWorker(currentIPAddrStr, foldername, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw);
//		Thread receiver_th = new Thread(receiver);
		if(deviceIP.equals("auto"))
			receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw);
		else
			receiver = new ReceiveWorker(currentIPAddrStr, data_folder, cert_folder, dataprocess, dev_uuid, db_name, table_name, user_id, user_pw, deviceIP);
		receiver_th = new Thread(receiver);
		receiver_th.start();
		// System.out.println(Thread.currentThread().getName());
		// implements Runnable
//		slave_to_master_transmission();

		System.out.println("* Master found: " + master_ip + "\n");
//		SlaveWorker slave = new SlaveWorker(master_ip, foldername, whatDB); // v1,2
		SlaveWorker slave = new SlaveWorker(master_ip, data_folder, cert_folder, dataprocess, currentIPAddrStr, table_name, dev_uuid);
//		; // new TransmitWorker(slaveList, foldername);
		Thread slave_th = new Thread(slave);
//		; // new Thread(master);
		slave_th.start();
	}
	
	
	public static void main(String[] args) throws UnknownHostException
	{
		if(args.length > 0)
		{
			currentIPAddr = InetAddress.getByName(args[args.length - 1]); // ex=\10.0.7.11
//			currentIPAddrStr = args[args.length - 1]; //InetAddress.getByName(args[args.length - 1]);
		}
		
		EdgeInformation();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println("!! main - " + currentIPAddrStr);
		
// Search the master edge in same network mash  
		EdgeFinder finder = new EdgeFinder((currentIPAddr == null) ? (null) : (currentIPAddrStr));
//		System.out.println("!! main - " + currentIPAddrStr);
		if(upnp_mode.equals("upnp"))
		{
			currentIPAddr = null; // 내 컴퓨터 이름을 자동으로 찾아 알리기 위해 - ip 주소 수동 입력시 master 알림이 정상동작하지 않음
			UPnPmode(finder);
		}
		else if(upnp_mode.equals("master"))
			MasterMode(finder);
		else if(upnp_mode.equals("slave"))
			SlaveMode(finder);
	}

	static EdgeReceptor receptor = null;
	static ReceiveWorker receiver = null;
	static Thread receiver_th = null;
//	static PentaCommunity penta = null;
//	static Thread penta_th = null;
	static MasterWorker master = null; // new TransmitWorker(slaveList, foldername);
	static Thread master_th = null; // new Thread(master);
	static Database dataprocess = null;

	static String currentIPAddrStr = null;
	static String deviceIP = null;
	static InetAddress currentIPAddr = null;
	static int defaultManualPort = 5678; // KETI 내부통신
	
	static int receive_check = 0;
	static String dev_uuid=null, data_folder=null, cert_folder=null, upnp_mode=null, master_ip=null;
	static String whatDB=null, db_name=null, db_path=null, table_name=null, user_id=null, user_pw=null;
}
