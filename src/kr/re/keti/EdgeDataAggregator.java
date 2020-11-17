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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
// core
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

public class EdgeDataAggregator
{
	public static void main(String[] args) throws UnknownHostException
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader("folder_name.txt"));
			while (true) {
				String line = br.readLine();
				if (line == null)
					break;
				
				foldername = line;
			}
			System.out.println("folder name : " + foldername);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		InetAddress currentIPAddr = null;
		String currentIPAddrStr = null;
/*
		Enumeration enu = null;
		try {
			enu = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while(enu.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) enu.nextElement();
		    Enumeration enu2 = n.getInetAddresses();
		    while (enu2.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) enu2.nextElement();
		        System.out.println(i.getHostAddress());
		    }
		}
*/
		
		if(args.length > 0)
		{
			currentIPAddr = InetAddress.getByName(args[args.length - 1]);
		}

		if(currentIPAddr == null)
		{
			currentIPAddrStr = InetAddress.getLocalHost().getHostAddress();
		}
		else
		{
			currentIPAddrStr = currentIPAddr.getHostAddress();
		}
		
		System.out.println("\nIP Address: " + currentIPAddrStr + "\n");
		
		EdgeFinder finder = new EdgeFinder((currentIPAddr == null)?(null):(currentIPAddrStr));
		
		if(finder.discoverMaster())
		{
			// I'm a slave.
			master_ip = finder.getMasterAddress().getHostAddress();
			
			 // implements Runnable
			Runnable receiver = new ReceiveWorker(foldername);
			Thread receiver_th = new Thread(receiver); 
			receiver_th.start();
			//System.out.println(Thread.currentThread().getName());
			// implements Runnable
//			slave_to_master_transmission();

			System.out.println("* Master found: " + master_ip + "\n");
			SlaveWorker slave = new SlaveWorker(master_ip, foldername);; //new TransmitWorker(slaveList, foldername);
			Thread slave_th = new Thread(slave);; //new Thread(master); 
			slave_th.start();
		}
		else
		{
			master_ip = currentIPAddrStr;
			// I'm the master!
			
			EdgeReceptor receptor = new EdgeReceptor((currentIPAddr == null)?(null):(currentIPAddrStr));
			ReceiveWorker receiver = new ReceiveWorker(foldername);
			Thread receiver_th = new Thread(receiver);
			
			System.out.println("Waiting for connections from slaves...");
			EdgeReceptor.ReceptionEvent rEvent = new EdgeReceptor.ReceptionEvent()
			{
				@Override
				public void handler(InetAddress addr) // when slave contact 
				{
					String slaveAddr = addr.getHostAddress(); // now slave address
					if(!slaveList.contains(slaveAddr)) // first contact
					{
						FileWriter fw = null;
						try {
							fw = new FileWriter("slave_ipList.txt", false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
/*
						if(slaveList.size() > 0)
						{
							System.out.println("input the end ");
							master.threadStop(true);
							master.interrupt();			
						}
*/
						slaveList.add(slaveAddr);
					
						System.out.println("* Slave list");
						for(int i = 0; i < slaveList.size(); ++i)
						{
							System.out.print("\t" + slaveList.get(i));
							try {
								fw.write(slaveList.get(i)+"\n");
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						System.out.print("\n");


						if(slaveList.size() == 1)
						{
							master = new MasterWorker(slaveList, foldername);
							master_th = new Thread(master); 
							master_th.start();
							master.masterSetting(master_ip);
//							master.start();
						}
//						master_to_slave_transmission(slaveList);
						else
							master.slaveSetting(slaveList);
						
						try {
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				}
				private ArrayList<String> slaveList = new ArrayList<String>();
				MasterWorker master = null; //new TransmitWorker(slaveList, foldername);
				Thread master_th = null; //new Thread(master); 
			};
			
			receptor.setEventHandler(rEvent);			
			receptor.start();
			receiver_th.start();
			
			try
			{
				receptor.join();
				receiver_th.join();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
	
		}
	}

	
	public static void master_to_slave_transmission(ArrayList<String> slaveList)
	{
		Scanner sc = new Scanner(System.in);
		DataProcess transmission = new  DataProcess();
		int check;
		
//		System.out.println(manage.file_list[0]); // ex. [1.txt, 2.txt]
//		System.out.println(manage.file_list[1]);
		while(true)
		{
			System.out.print("filename(declare an end = end )\t(ex) 2.txt ?\t");
			String filename = sc.nextLine();
			if(filename.equals("end"))
				break;
			System.out.println("function : 1. fileExist     2. fileCreate     3. fileRemove     4. fileWrite     5. fileRead     6. fileWhere     7. fileLength");
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
			if(func == 1)
			{
				check = transmission.fileExist(foldername+filename);
				if(check == -1)
				{
					check = transmission.fileExist(filename, slaveList);
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile exist. [false]");
					else
						System.out.printf("\tfile exist in slave #%d.\n", check+1);
				}
				else
					System.out.println("\tfile exist in local.");
			}
			else if(func == 2)
			{
				check = transmission.fileCreate(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile exist already.");
//				else if(check == -2)
//					System.out.println("\tfile cannot create.");
				else
					System.out.println("\tfile creation is success.");
			}
			else if(func == 3)
			{
				check = transmission.fileRemove(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile don't exist already.");
//				else if(check == -2)
//					System.out.println("\tfile list correct.");
				else
					System.out.println("\tfile removing is success.");
			}
			else if(func == 4)
			{
				check = transmission.fileWrite(foldername+filename);
				if(check == -1)
				{
					check = transmission.fileWrite(filename, slaveList); 
					if(check == -1)
					{
						System.out.println("\tfile don't exist and write in local.");
						transmission.fileWrite(foldername+filename, 1);
					}
					else
						System.out.printf("\tfile writing is success in slave #%d.", check+1);									
				}
				else
					System.out.println("\tfile writing is success in local.");				
			}
			else if(func == 5)
			{
				check = transmission.fileRead(foldername+filename);
				if(check == -1)
				{
					check = transmission.fileRead(filename, slaveList);
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile exist. [false]");
					else
						System.out.printf("\tfile reading is success in slave #%d.\n", check+1);
				}
//				else if(check == -2)
//				System.out.println("\tfile cannot read.");
				else
					System.out.println("\tfile reading is success in local.");				
			}
			else if(func == 6)
			{
				check = transmission.fileWhere(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile don't exist.");
//				else if(check == -2)
//					System.out.println("\tfile cannot search.");
				else
					System.out.printf("\tfile is in %dth place. (0 is local, 1~ are remote node)\n", check);
				
			}
			else if(func == 7)
			{
				check = transmission.fileLength(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile don't exist.");
//				else if(check == -2)
//					System.out.println("\tfile length measure is false.");
				else
					System.out.println("\tfile length measure is success.\n\tfile Size : " + check);
			}
			else if(func == 8)
			{ 
				// vi, cat, less = impossible , gedit = possible
				check = transmission.fileOpen(filename, master_ip);
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

	public static void slave_to_master_transmission()
	{
		Scanner sc = new Scanner(System.in);
		DataProcess dataprocess = new  DataProcess();
		int check, i;
		ArrayList<String> ipList = new ArrayList<String>();
		
//		System.out.println(manage.file_list[0]); // ex. [1.txt, 2.txt]
//		System.out.println(manage.file_list[1]);
		while(true)
		{
			System.out.print("filename(declare an end = end )\t(ex) 2.txt ?\t");
			String filename = sc.nextLine();
			if(filename.equals("end"))
				break;
			System.out.println("function : 1. fileExist     2. fileCreate     3. fileRemove     4. fileWrite     5. fileRead     6. fileWhere     7. fileLength");
//			System.out.println("function : 8. fileOpen\t 9. fileClose");
			System.out.print("function number\t(ex) 1 ?\t");
			int func = sc.nextInt();
			sc.nextLine();
			
			ipList.add(master_ip);
			// list request
			
			while(func<1 || func>9)
			{
				System.out.print("function is wrong.\nfunction number\t(ex) 1 ?\t");
				func = sc.nextInt();
				sc.nextLine();
			}
			if(func == 1)
			{
				check = dataprocess.fileExist(foldername+filename);
				if(check == -1)
				{
					check = dataprocess.fileExist(filename, master_ip);
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile exist. [false]");
					else
//						System.out.println("\tfile exist in master.");
						System.out.println("\tfile exist.");
				}
				else
					System.out.println("\tfile exist.");// in local.");
			}
			else if(func == 2)
			{
				check = dataprocess.fileCreate(filename, master_ip); 
				if(check == -1)
					System.out.println("\tfile exist already.");
//				else if(check == -2)
//					System.out.println("\tfile cannot create.");
				else
					System.out.println("\tfile creation is success.");
			}
			else if(func == 3)
			{
				check = dataprocess.fileRemove(filename, master_ip); 
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
					check = dataprocess.fileWrite(filename, master_ip); 
					if(check == -1)
					{
						System.out.println("\tfile don't exist and write in local.");
						dataprocess.fileWrite(foldername+filename, 1);
					}
					else
//						System.out.println("\tfile writing is success in master");									
						System.out.println("\tfile writing is success");// in local.");				
				}
				else
					System.out.println("\tfile writing is success");// in local.");				
			}
			else if(func == 5)
			{
				check = dataprocess.fileRead(foldername+filename);
				if(check == -1)
				{
					check = dataprocess.fileRead(filename, master_ip);
					if(check == -1)
						System.out.println("\tfile don't exist.");
//					else if(check == -2)
//						System.out.println("\tfile exist. [false]");
					else
//						System.out.println("\tfile reading is success in master.");
						System.out.println("\tfile reading is success.");
				}
//				else if(check == -2)
//				System.out.println("\tfile cannot read.");
				else
					System.out.println("\tfile reading is success.");// in local.");				
			}
			else if(func == 6)
			{
				check = dataprocess.fileWhere(foldername+filename); 
//				System.out.println(check);
				if(check == -1)
					check = dataprocess.fileWhere(filename, master_ip) + 1;
				
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
					check = dataprocess.fileLength(filename, master_ip); 
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
				check = dataprocess.fileOpen(filename, master_ip);
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

	static String master_ip=null;
	static int receive_check=0;
	static String foldername = "/home/eunae/keti/";
}
