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
	SlaveWorker(String master_ip, String dfname, String cfname, Database dp, String slave_ip, String tablename)
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
		this.slave_ip = slave_ip;
		this.table_name = tablename;
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
		
		DataProcess dataprocess = new  DataProcess(data_folder, cert_folder, database, slave_ip, table_name); // v0803
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
			int func = sc.nextInt();
			sc.nextLine();
			while(func<0 || func>9)
			{
				System.out.print("function is wrong.\nfunction number\t(ex) 1 ?\t");
				func = sc.nextInt();
				sc.nextLine();
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
				System.out.print("What do you Want to Know the Information of Device\t(ex)127.0.1.1 ?\t");
				String device_ip = sc.nextLine();
				
				if(slaveList.contains(device_ip) || master_ip.equals(device_ip))
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
			}
			else
			{
				System.out.print("Do you Want to Know the Contents of DATA)\t(ex) DataID ?\t");
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
						dataList.add(slave_ip);
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
						if(slaveList.get(i).equals(slave_ip))
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
				
/*
// v211028 이전 방식		
				if(func == 3)
				{
//					slaveList = dataprocess.RequestSlaveList(master_ip);
//					System.out.println("slaves ip list : " + slaveList);
					System.out.println("request to master");
					check = dataprocess.MetaDataInfomation(filename, master_ip); // 210428 add int func
					if(check == 1)
						dataList.add(master_ip); //localhost == master_ip

					System.out.println("request to mine");
					check = dataprocess.MetaDataInfomation(filename, slave_ip); // 210428 add int func
					if(check == 1)
						dataList.add(slave_ip); 

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(slave_ip))
							continue;
						System.out.println("request to " + slaveList.get(i));
						check = dataprocess.MetaDataInfomation(filename, slaveList.get(i)); // 210428 add int func
						if(check == 1)
							dataList.add(slaveList.get(i));
//						System.out.println(check);
					
					}
					
					if(dataList.size() == 0)
						System.out.println("* Anyone doesn't have MetaData.");
					else
						System.out.println("* [" + dataList + "] : have MetaData.");
				}
				else if(func == 4) // fileExsit always execute.
				{
//					slaveList = dataprocess.RequestSlaveList(master_ip);
//					System.out.println("slaves ip list : " + slaveList);
					System.out.println("request to master");
					check = dataprocess.IndividualDataRead(filename, master_ip); // 210428 add int func
					if(check == 2)
						dataList.add(master_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + master_ip + "] : has only MetaData.");
						metaList.add(master_ip);

					System.out.println("request to mine");
					check = dataprocess.IndividualDataRead(filename, slave_ip); // 210428 add int func
					if(check == 2)
						dataList.add(slave_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + slave_ip + "] : has only MetaData.");
						metaList.add(slave_ip);

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(slave_ip))
							continue;
						System.out.println("request to " + slaveList.get(i));
						check = dataprocess.IndividualDataRead(filename, slaveList.get(i)); // 210428 add int func
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
*/

// 공인인증시험 준비
				else if(func == 4) // fileExsit always execute.
				{
//					ArrayList<String> edgeList=new ArrayList<String>();
//					edgeList = slaveList.clone();
//					edgeList.add(0, master_ip);
					File folder = new File (data_folder+"chunk");
					if(!folder.exists())
					{
//						System.out.println("!! receive 007 cert mkdir");
						folder.mkdir();
					}
					folder = new File (data_folder+"time");
					if(!folder.exists())
					{
//						System.out.println("!! receive 007 cert mkdir");
						folder.mkdir();
					}

					check = dataprocess.IndividualDataRead(filename); // 211101 - 직접 검색
//					check = dataprocess.IndividualDataRead(filename, slave_ip); // 210428 - 소켓통신으로 나 자신에게 문의
					if(check == 2)
						dataList.add(slave_ip); //localhost == master_ip
					else
					{
						if(check == 1)
							metaList.add(slave_ip);
						ArrayList<String> edgeList = dataprocess.RequestSlaveList(master_ip);
						edgeList.add(0, master_ip);
						edgeList.remove(slave_ip);
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
					check = dataprocess.IndividualDataWrite(filename, slave_ip); // 210428 add int func
					if(check == 2)
						dataList.add(slave_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + slave_ip + "] : has only MetaData.");
						metaList.add(slave_ip);

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(slave_ip))
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
					System.out.println("request to master");
					check = dataprocess.IndividualDataRemove(filename, master_ip); // 210428 add int func
					if(check == 2)
						dataList.add(master_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + master_ip + "] : has only MetaData.");
						metaList.add(master_ip);

					System.out.println("request to mine");
					check = dataprocess.IndividualDataRemove(filename, slave_ip); // 210428 add int func
					if(check == 2)
						dataList.add(slave_ip); //localhost == master_ip
					else if(check == 1)
//						System.out.println("* [" + slave_ip + "] : has only MetaData.");
						metaList.add(slave_ip);

					for(i=0; i<slaveList.size(); i++)
					{
						if(slaveList.get(i).equals(slave_ip))
							continue;
						System.out.println("request to " + slaveList.get(i));
						check = dataprocess.IndividualDataRemove(filename, slaveList.get(i)); // 210428 add int func 
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
	public static Database database = null;
	static String slave_ip = null;
}
