package kr.re.keti;

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
import java.util.ArrayList;
import java.util.Scanner;

public class MasterWorker implements Runnable // extends Thread // implements Runnable
{

	MasterWorker(ArrayList<String> ip_list, String fname)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.fileList = new ArrayList<String>();
		this.slaveList = (ArrayList<String>) ip_list.clone();
		this.foldername = fname;
	}
	MasterWorker(String ip, String fname)
	{
		this.stop = false;
		this.slaveList = new ArrayList<String>();
		this.fileList = new ArrayList<String>();
		this.master_ip = ip;
		this.foldername = fname;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Scanner sc = new Scanner(System.in);
		DataProcess dataprocess = new  DataProcess();
		int check=-1, i;
		
//		System.out.println(manage.file_list[0]); // ex. [1.txt, 2.txt]
//		System.out.println(manage.file_list[1]);
		while(!stop)
		{
			if(Thread.interrupted())
				break;
			
			System.out.print("filename(declare an end = end )\t(ex) 2.txt ?\t");
			String filename = sc.nextLine();
			if(filename.equals("end"))
				break;
			System.out.println("function : 1. fileExist     5. fileRead");
//			System.out.println("function : 1. fileExist     2. fileCreate     3. fileRemove     4. fileWrite     5. fileRead     6. fileWhere     7. fileLength");
//			System.out.println("function : 8. fileOpen\t 9. fileClose");
			System.out.print("function number\t(ex) 1 ?\t");
			int func = sc.nextInt();
			sc.nextLine();
			while(func<1 || func>9)
			{
				System.out.print("function is wrong.\nfunction number\t(ex) 1 ?\t");
				func = sc.nextInt();
				sc.nextLine();
			}
			
			System.out.println("slaves ip list : " + slaveList);
			
			fileList.clear();
//			System.out.println("master ip list : " + master_ip);
			for(i=0; i<slaveList.size(); i++)
			{
				check = dataprocess.fileExist(filename, slaveList.get(i));
				if(check == 1)
					fileList.add(slaveList.get(i));
			}
			check = dataprocess.fileExist(filename, "localhost");
			if(check == 1)
				fileList.add("localhost"); //localhost == master_ip
/*
			if(func == 1)
			{
				fileList.clear();
				for(i=0; i<slaveList.size(); i++)
				{
					check = dataprocess.fileExist(filename, slaveList.get(i));
					if(check == 1)
						fileList.add(slaveList.get(i));
				}
				check = dataprocess.fileExist(filename, "localhost");
//				check = dataprocess.fileExist(filename, master_ip);
				if(check == 1)
					fileList.add("localhost");
//					fileList.add(master_ip);
				if(fileList.size() == 0)
					System.out.println("\tfile don't exist.");
			}
*/
			if(func == 1) // fileExsit always execute.
			{
				if(fileList.size() == 0)
					System.out.println("Anyone doesn't had DATA.");
				else
					System.out.println("[" + fileList + "] : had DATA.");
			}
			else if(func == 2)
			{
//				check = transmission.fileCreate(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile exist already.");
//				else if(check == -2)
//					System.out.println("\tfile cannot create.");
				else
					System.out.println("\tfile creation is success.");
			}
			else if(func == 3)
			{
//				check = transmission.fileRemove(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile don't exist already.");
//				else if(check == -2)
//					System.out.println("\tfile list correct.");
				else
					System.out.println("\tfile removing is success.");
			}
			else if(func == 4)
			{
				check = dataprocess.fileWrite(foldername+filename);
				if(check == -1)
				{
					check = dataprocess.fileWrite(filename, slaveList); 
					if(check == -1)
					{
						System.out.println("\tfile don't exist and write in local.");
						dataprocess.fileWrite(foldername+filename, 1);
					}
					else
//						System.out.printf("\tfile writing is success in slave #%d.\n", check+1);									
						System.out.println("\tfile writing is success");// in local.");				
				}
				else
					System.out.println("\tfile writing is success");// in local.");				
			}
			else if(func == 5)
			{
				// FileExist() above
				if (fileList.size() == 0)
					System.out.println("Anyone doesn't had and couldn't read DATA.");
					
				check  = dataprocess.fileRead(filename, fileList);
				if(check == 1)
					System.out.println("[" + fileList + "] : had and could read DATA.");
				else
					System.out.println("[" + fileList + "] : had and couldn't read DATA.");
			}
/* version #2
			else if(func == 5)
			{
				check = dataprocess.fileRead(foldername+filename);
				if(check == -1)
				{
					check = dataprocess.fileRead(filename, slaveList);
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile exist. [false]");
					else
//						System.out.printf("\tfile reading is success in slave #%d.\n", check+1);
						System.out.println("\tfile reading is success.");
				}
//				else if(check == -2)
//				System.out.println("\tfile cannot read.");
				else
					System.out.println("\tfile reading is success.");// in local.");				
			}
*/
			else if(func == 6)
			{
				check = dataprocess.fileWhere(foldername+filename); 
//				System.out.println(check);
				if(check == -1)
					check = dataprocess.fileWhere(filename, slaveList) + 1;
				
				if(check == -1)
					System.out.println("\tfile don't exist.");
//				else if(check == -2)
//					System.out.println("\tfile cannot search.");
				else
					System.out.printf("\tfile is in %dth place. (0 is local, 1~ are connected nodes)\n", check);
				
			}
			else if(func == 7)
			{
				check = dataprocess.fileLength(foldername+filename);
				if(check == -1)
				{
					check = dataprocess.fileLength(filename, slaveList); 
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile length measure is false.");
					else
						System.out.println("\tfile length measure is success.\n\tfile Size : " + check);
				}
				else
					System.out.println("\tfile length measure is success.\n\tfile Size : " + check);
					
			}
			else if(func == 8)
			{ 
				// vi, cat, less = impossible , gedit = possible
//				check = transmission.fileOpen(filename, master_ip);
				if(check == -1)
					System.out.println("\tfile don't exist.");
//				else if(check == -2)
//					System.out.println("\tfile openning is false.");
				else
					System.out.println("\tfile openning is success.");
			}
			else if(func == 9)
			{
				// how to ??
			}
		}
		
	}

	public void threadStop(boolean stop)
	{
		this.stop = stop;
	}
	public void slaveSetting(ArrayList<String> slist)
	{
		slaveList = (ArrayList<String>) slist.clone();
	}
	public void masterSetting(String ip)
	{
		master_ip = ip;
	}

	private boolean stop = false;
	public static ArrayList<String> slaveList=null;
	public static ArrayList<String> fileList=null;
	public static String master_ip=null;
	public static String foldername = "/home/eunae/keti/";
}

