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
// core
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
		
//		System.out.println("\nIP Address: " + currentIPAddrStr + "\n");
		
		EdgeFinder finder = new EdgeFinder((currentIPAddr == null)?(null):(currentIPAddrStr));
		
		if(finder.discoverMaster())
		{
			// I'm a slave.
			master_ip = finder.getMasterAddress().getHostAddress();
			FileWriter fw = null;
			try {
				fw = new FileWriter("edge_ipList.txt", false);
				fw.write("slave\n");
				fw.write(master_ip);
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
			FileWriter fw = null;
			try {
				fw = new FileWriter("edge_ipList.txt", false);
				fw.write("master\n");
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			EdgeReceptor receptor = new EdgeReceptor((currentIPAddr == null)?(null):(currentIPAddrStr));
			ReceiveWorker receiver = new ReceiveWorker(foldername);
			Thread receiver_th = new Thread(receiver);
			
			System.out.println("Waiting for connections from slaves...");
			EdgeReceptor.ReceptionEvent rEvent = new EdgeReceptor.ReceptionEvent()
			{
				private String msg = "";
				@Override
				public void handler(InetAddress addr) // when slave contact 
				{
					String slaveAddr = addr.getHostAddress(); // now slave address
					if(!slaveList.contains(slaveAddr)) // first contact
					{
						FileWriter fw = null;
						try {
							fw = new FileWriter("edge_ipList.txt", false);
							fw.write("master\n");
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
/*					
						try
						{
							final String os_version = System.getProperty("os.name");

					       if (os_version.contains("Windows"))
					        {
					    	   Runtime.getRuntime().exec("cls");
					        }
					       else
					        {
					    	   Runtime.getRuntime().exec("clear");
					        }
						}  catch (final Exception e)
						{
					        //  Handle any exceptions.
						}
*/
						SimpleDateFormat timeformat = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss");	
						Date nowtime = new Date();		
						String logtime = timeformat.format(nowtime);
						
						String message = "* slave list *\n";
			            System.out.println("* Slave list");
			            msg += logtime + ": " + slaveList.get(slaveList.size()-1) + "\n";
			            for(int i = 0; i < slaveList.size(); ++i)
			            {
//			              message += slaveList.get(i) + "\n";
			              //System.out.print("\t" + logtime + ":" + slaveList.get(i)+"\n");
			              try {
			                fw.write(slaveList.get(i)+"\n");
			              } catch (IOException e) {
			                // TODO Auto-generated catch block
			                e.printStackTrace();
			              }
			            }
			            System.out.println(msg);
			            //JOptionPane.showMessageDialog(null, message);

						if(master == null) // test need
						{
							master = new MasterWorker(master_ip, foldername);
							master_th = new Thread(master); 
							master_th.start();
//							master.start();
						}
//						master_to_slave_transmission(slaveList);
//						else
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

	static String master_ip=null;
	static int receive_check=0;
	static String origin_foldername = "/home/eunae/keti/";
	static String foldername = "/home/eunae/keti/";
}
