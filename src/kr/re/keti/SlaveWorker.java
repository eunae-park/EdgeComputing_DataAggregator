package kr.re.keti;

import kr.re.keti.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Scanner;

public class SlaveWorker implements Runnable // extends Thread // implements Runnable
{

	SlaveWorker(ArrayList<String> ip_list, String fname)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.dataList = new ArrayList<String>();
		this.metaList = new ArrayList<String>();
		this.slaveList = (ArrayList<String>) ip_list.clone();
//		this.origin_foldername = fname;
		this.data_folder = fname;
	}
	SlaveWorker(String ip, String fname, String whatDB)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.dataList = new ArrayList<String>();
		this.metaList = new ArrayList<String>();
		this.master_ip = ip;
//		this.origin_foldername = fname;
		this.data_folder = fname;
		this.whatDB = whatDB;
	}
	SlaveWorker(String ip, String fname, Database dp)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.dataList = new ArrayList<String>();
		this.metaList = new ArrayList<String>();
		this.master_ip = ip;
//		this.origin_foldername = fname;
		this.data_folder = fname;
		this.database = dp;
	}
	SlaveWorker(String master_ip, String dfname, String cfname, Database dp, String slave_ip, String tablename, String uuid)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.dataList = new ArrayList<String>();
		this.metaList = new ArrayList<String>();
		this.master_ip = master_ip;
//		this.origin_foldername = fname;
		this.data_folder = dfname;
		this.cert_folder = cfname;
		this.database = dp;
		this.my_ip = slave_ip;
		this.table_name = tablename;
		this.device_uuid = uuid;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		Scanner sc = new Scanner(System.in);
		
		DataProcess dataprocess = new  DataProcess(data_folder, cert_folder, database, my_ip, table_name, device_uuid); // v0803
//		DataProcess dataprocess = new  DataProcess(foldername, whatDB); // v1
/*//v2		
		Database dataprocess = null;
		if(whatDB.equals("MySQL"))
			dataprocess = Database.getInstance(Database.DB_MySQL);
		else if(whatDB.equals("SQLite"))
			dataprocess = Database.getInstance(Database.DB_SQLITE);
		dataprocess.connectDB(foldername);
*/		
		int check=-1, ip_number=0, i;
		
//		System.out.println(manage.file_list[0]); // ex. [1.txt, 2.txt]
//		System.out.println(manage.file_list[1]);
		while(!stop)
		{
			if(Thread.interrupted())
				break;
			
			System.out.println("function : 1. Device Information     2. Whole Data Information     3. Individual MetaData Information     4. Individual Data Read     5. Individual Data Write     6. Individual Data Remove");
			System.out.println("function : 0. Declare EXIT");
			System.out.print("function number\t(ex) 1 ?\t");
			String input_func="none";
			while(!sc.hasNextLine()) // && input_func.equals("")) // NoSuchElementException : No line found
			{
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			input_func = sc.nextLine();
			if(!input_func.matches("[+-]?\\d*(\\.\\d+)?") || input_func.equals("none") || input_func.equals(""))
			{
				System.out.println("\tInput String is Wrong.(Input only Number)");
//				System.out.print("Input is wrong.\nfunction number\t(ex) 1 ?\t");
				continue ;
			}
//			sc.nextLine();
			int func = Integer.parseInt(input_func);
			if(func<0 || func>7)
			{
				System.out.println("\tInput Number is Wrong.(Input only Range 0~7)");
//				System.out.print("Input is wrong.\nfunction number\t(ex) 1 ?\t");
				continue ;
			}
			if(func == 0)
			{
				stop = true;
//				break;
				System.exit(0);
			}

			dataList.clear();
			metaList.clear();
			dataprocess.SettingPort();

			slaveList = dataprocess.RequestSlaveList(master_ip);
			System.out.println("Edge List : " + master_ip + "(master)\t" +  slaveList);

//			if(func == 0)
//				threadStop(true);
			if(func == 1)
			{
				System.out.print("Which Edge Device Do you Want to Know\t(ex)127.0.1.1 ?\t");
				String device_ip = sc.nextLine();
				
				if(device_ip.equals(my_ip))
				{
					System.out.println("request to mine");
					dataprocess.DeviceInfomation(); // 210428 add int func
//					if(check == 1)
//						System.out.println("* Bring the Information of Edge Device(" + device_ip + ").");
//					if(check == -1)
//						System.out.println("* Bring the Information of Edge Device(" + device_ip + ") : Failure.");
				}
				else if(slaveList.contains(device_ip) || master_ip.equals(device_ip))
				{
					System.out.println("request to " + device_ip);
					check = dataprocess.DeviceInfomation(device_ip); // 210428 add int func
//					if(check == 1)
//						System.out.println("* Bring the Information of Edge Device(" + device_ip + ").");
					if(check == -1)
						System.out.println("* Bring the Information of Edge Device(" + device_ip + ") : Failure.");
				}
				else
				{
					System.out.println("* Edge Device(" + device_ip + ") don't consist in Edge Network.");
				}
			}
			else if(func == 2)
			{
				System.out.print("Which Edge Device Do you Want to Know\t(ex)127.0.1.1 ?\t");
				String device_ip = sc.nextLine();
				if(device_ip.equals(my_ip))
				{
					System.out.println("request to mine");
					dataprocess.WholeDataInfo(); // 210428 add int func
//					if(check == -1)
//						System.out.println("* Bring the Information of Edge Device(" + device_ip + ") : Failure.");
				}
				else if(slaveList.contains(device_ip) || master_ip.equals(device_ip))
				{
					System.out.println("request to " + device_ip);
					check = dataprocess.WholeDataInfo(device_ip); // 210428 add int func
//					if(check == 1)
//						System.out.println("* Bring the Information of Edge Device(" + device_ip + ").");
					if(check == -1)
						System.out.println("* Bring the Information of Edge Device(" + device_ip + ") : Failure.");
				}
				else
				{
					System.out.println("* Edge Device(" + device_ip + ") don't consist in Edge Network.");
				}
			}
			else
			{
//				System.out.print("Do you Want to Know the Contents of DATA)\t(ex) DataID ?\t");
				System.out.print("What Data Do you Want to Know(Input DataID)?\t");
				String filename = sc.nextLine();
//				System.out.println("slaves request dataID : " + filename);
//				System.out.println("slaves ip list : " + slaveList);

				if(func == 3)
				{
//					slaveList = dataprocess.RequestSlaveList(master_ip);
//					System.out.println("slaves ip list : " + slaveList);
					System.out.print("request to - mine, ");
					String result="none";
					String result_bakcup="none";
					String edge="mine";
					result = dataprocess.MetaDataInfomation(filename); // 210428 add int func
					if(!result.equals("none"))
					{
						dataList.add(my_ip);
						result_bakcup = result;
					}

					System.out.print(master_ip + "(master), ");
					if(result_bakcup.equals("none"))
						result = dataprocess.MetaDataInfomation(filename, master_ip, 0); // metadata를 저장하고 싶으면=1, 저장 안하면=0
					else
						result = dataprocess.MetaDataInfomation(filename, master_ip, 0); // metadata 로컬에 있음
					if(!result.equals("none"))
					{
						dataList.add(master_ip);
						if(result_bakcup.equals("none"))
							result_bakcup = result;
						edge = master_ip;
					}

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(my_ip))
							continue;
						System.out.print(slaveList.get(i) + ", ");
						if(result_bakcup.equals("none"))
							result = dataprocess.MetaDataInfomation(filename, slaveList.get(i), 0); // metadata를 저장하고 싶으면=1, 저장 안하면=0
						else
							result = dataprocess.MetaDataInfomation(filename, slaveList.get(i), 0); // metadata 로컬에 있음
						if(!result.equals("none"))
						{
							dataList.add(slaveList.get(i));
							if(result_bakcup.equals("none"))
								result_bakcup = result;
							edge = slaveList.get(i);
						}
					}
					System.out.println("");
					
					if(dataList.size() == 0)
						System.out.println("* Anyone doesn't have MetaData.");
					else
					{
						String[] array = result_bakcup.split("#");
						System.out.println("* [" + dataList + "] : have MetaData.");
						System.out.println("* Metadata information in " + edge + " :" 
								+ "\n\tDataID: " + array[0] + "\n\tTimeStamp: " + Timestamp.valueOf(array[1]) + "\n\tFileType: " + array[2] + "\n\tDataType: " + Integer.parseInt(array[3])
								+ "\n\tsecurityLevel: " + Integer.parseInt(array[4]) + "\n\tDataPriority: " + Integer.parseInt(array[5]) + "\n\tAvailabilityPolicy: " + Integer.parseInt(array[6])
								+ "\n\tDataSignature: " + array[7] + "\n\tCert: " + array[8] + "\n\tDirectory: " + array[9] + "\n\tLinked_edge: " + array[10] + "\n\tDataSize: " + Long.parseLong(array[11]));
					}
				}
				
// 공인인증시험 준비
				else if(func == 4) // fileExsit always execute.
				{
//					ArrayList<String> edgeList=new ArrayList<String>();
//					edgeList = slaveList.clone();
//					edgeList.add(0, master_ip);

					check = dataprocess.IndividualDataRead(filename); // 211101 - 직접 검색
//					check = dataprocess.IndividualDataRead(filename, slave_ip); // 210428 - 소켓통신으로 나 자신에게 문의
					if(check == 2)
						dataList.add(my_ip); //localhost == master_ip
					else
					{
						if(check == 1)
							metaList.add(my_ip);
						ArrayList<String> edgeList = dataprocess.RequestSlaveList(master_ip);
						edgeList.add(0, master_ip);
						edgeList.remove(my_ip);
						dataList = dataprocess.IndividualDataRead(filename, edgeList); // 210428 add int func
					}
					
					if(dataList.size() != 0)
						System.out.println("* [" + dataList + "] : have Data.");
					else if(metaList.size() != 0)
						System.out.println("* [" + metaList + "] : have only MetaData.");
					else
						System.out.println("* Anyone doesn't have Data.");
				}				
				else if(func == 5) // write
				{
//					slaveList = dataprocess.RequestSlaveList(master_ip);
//					System.out.println("slaves ip list : " + slaveList);
					System.out.println("request to master");
					check = dataprocess.IndividualDataWrite(filename, master_ip); // 210428 add int func
					if(check == 2)
						dataList.add(master_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + master_ip + "] : has only MetaData.");
						metaList.add(master_ip);

					System.out.println("request to mine");
					check = dataprocess.IndividualDataWrite(filename, my_ip); // 210428 add int func
					if(check == 2)
						dataList.add(my_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + slave_ip + "] : has only MetaData.");
						metaList.add(my_ip);

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(my_ip))
							continue;
						System.out.println("request to " + slaveList.get(i));
						check = dataprocess.IndividualDataWrite(filename, slaveList.get(i)); // 210428 add int func
						if(check == 2)
							dataList.add(slaveList.get(i));
						else if(check == 1)
//							System.out.println("* [" + slaveList.get(i) + "] : has only MetaData.");
							metaList.add(slaveList.get(i));
//						System.out.println(check);
					}
					
					if(metaList.size() != 0)
						System.out.println("* [" + metaList + "] : have only MetaData.");

					if(dataList.size() == 0)
						System.out.println("* Anyone doesn't have Data.");
					else
						System.out.println("* [" + dataList + "] : have Data.");
				}				
				else if(func == 6) // remove
				{
					System.out.print("Which Edge Do you Want to Erase the data\t(ex) 127.0.0.1 ?\t");
					String ip = sc.nextLine();
					boolean ip_check=false;
					if(ip.equals("all"))// whole edge remove request
					{
						System.out.println("request to master");
						check = dataprocess.IndividualDataRemove(filename, master_ip); // 210428 add int func
						if(check == 2)
							dataList.add(master_ip); //localhost == master_ip
						else if(check == 1)
//								System.out.println("* [" + master_ip + "] : has only MetaData.");
							metaList.add(master_ip);
						for(i=0; i<slaveList.size(); i++)
						{
							if(slaveList.get(i).equals(my_ip))
								System.out.println("request to mine");
							else
								System.out.println("request to " + slaveList.get(i));
							check = dataprocess.IndividualDataRemove(filename, my_ip); // 210428 add int func
							if(check == 2)
								dataList.add(my_ip); //localhost == master_ip
							else if(check == 1)
								metaList.add(my_ip);
//							System.out.println(check);
						}

//						if(metaList.size() != 0)
//							System.out.println("* [" + metaList + "] : have only MetaData.");
//						if(dataList.size() == 0)
//							System.out.println("* Anyone doesn't have Data.");
//						else
//							System.out.println("* [" + dataList + "] : have Data.");
						if(dataList.size() != 0 || metaList.size() != 0)
						{
							if(dataList.size() != 0)
								System.out.println("* " + dataList + " : have Data and Remove.");
							if(metaList.size() != 0)
//								System.out.println("* [" + ip + "] : have only MetaData, but cannot Remove.");
								System.out.println("* " + metaList + "] : have Data but don't have an Authority.");
						}
						else
							System.out.println("* Anyone doesn't have Data.");

					}
					else
					{
						if(ip.equals(master_ip))
						{
							ip_check = true;
							System.out.println("request to master");
							check = dataprocess.IndividualDataRemove(filename, master_ip); // 210428 add int func
						}
						for(i=0; i<slaveList.size(); i++)
						{
							if(slaveList.get(i).equals(my_ip) && ip.equals(my_ip))
							{
								ip_check = true;
								System.out.println("request to mine");
								check = dataprocess.IndividualDataRemove(filename); // 210428 add int func
								
							}
							else if(ip.equals(slaveList.get(i)))
							{
								ip_check = true;
								System.out.println("request to " + slaveList.get(i));
								check = dataprocess.IndividualDataRemove(filename, slaveList.get(i)); // 210428 add int func 
							}
//							System.out.println(check);
						}
						if(!ip_check)
							System.out.println("* [" + ip + "] : isn't cosist of Edge Network.");
						else if(check == 2)
							System.out.println("* [" + ip + "] : have Data and Remove.");
						else if(check == 1)
							System.out.println("* [" + ip + "] : have Data but don't have an Authority.");
						else
							System.out.println("* [" + ip + "] doesn't have Data.");
					}
/* 					
*/	// whole edge remove request 						
				}					
			}
		}
		
	}
/*
	public int IPList(String ipAddress)
	{
		int i=0;
		try {
			Socket socket = new Socket(ipAddress, TCPport);
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			String message = "list";
			os.write( message.getBytes() ); // file list
			os.flush();
			
			if(slaveList.size() > 0)
				slaveList.clear();
			
//			slaveList.add(master_ip);
			String resultFromServer = "";
			byte[] data = new byte[100];
			while (true)
			{
				int len = is.read(data);
				if(len < 0)
					break;
				resultFromServer = new String(data,0,len);
				String[] read_text = resultFromServer.split("\n");
				for(i=0; i<read_text.length; i++)
				{
//					System.out.println("\t" + read_text[i]);
					slaveList.add(read_text[i]);
				}
			}
			socket.close();				
			while(!socket.isClosed()) {			}// socket close wait - success
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return i;	
	}
*/	
	public void threadStop(boolean stop)
	{
		this.stop = stop;
	}
	public void slaveAdd(ArrayList<String> slist)
	{
		slaveList = (ArrayList<String>) slist.clone();
	}

	private boolean stop = false;
	public static ArrayList<String> slaveList=null;
	public static ArrayList<String> hashList=null;
	public static ArrayList<String> dataList=null;
	public static ArrayList<String> metaList=null;
	public static String master_ip=null;
	public String my_hashcode=null;
//	public static String origin_foldername = "/home/eunae/keti/";
	public static String data_folder = "/home/keti/data";
	public static String cert_folder = "/home/keti/data";
	static String whatDB = "MySQL";
	static String table_name = "file_management"; //(file_name, uuid, security, sharing, location)
	static String device_uuid = "f1d6fc0c-1c51-11ec-a6c1-b75b198d62ab"; //(file_name, uuid, security, sharing, location)
	public static Database database = null;
	static String my_ip = null;
}
