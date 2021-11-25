package kr.re.keti;
// master case

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

public class EdgeReceptor
{
	public EdgeReceptor()
	{
		initReceptor(null);
	}
	
	public EdgeReceptor(String myAddr)
	{
//		System.out.println("EdgeReceptor" + myAddr);
		initReceptor(myAddr);
	}
	
	private void initReceptor(String myAddr)
	{
//		System.out.println("initReceptor");
		working = false;
		listenerThread = receptionThread = null;
		waitingAddressQueue = new ArrayBlockingQueue<InetAddress>(defaultWaitingQueueCapacity);
		masterAddress = null;
		receptionEvent = null;
		masterAddressObj = null;
		
		try
		{
			// 127.0.0.1 = 자기 자신을 의미하는 localhost
			// 127.0.1.1 =  자신의 컴퓨터 이름
			if(myAddr == null)
			{
				masterAddress = InetAddress.getLocalHost().getHostAddress().getBytes();

//				System.out.println("!!" + InetAddress.getLocalHost().getHostAddress()); // 127.0.1.1 //
			}
			else
			{
				masterAddressObj = InetAddress.getByName(myAddr);
				masterAddress = masterAddressObj.getHostAddress().getBytes();

//				System.out.println("!!" + masterAddressObj.getHostAddress());
			}
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
	}
	
	private void initThreads()
	{
//		System.out.println("initThreads");
		working = true;
		
		listenerThread = new Thread(){
			public void run()
			{
//				System.out.println("listenerThread");
				bsSocket = null;
				
				try
				{
					if(masterAddressObj == null)
					{
//						System.out.println("3");
						bsSocket = new DatagramSocket(EdgeFinder.defaultBroadcastPort);
					}
					else
					{
//						System.out.println("4");
						bsSocket = new DatagramSocket(EdgeFinder.defaultBroadcastPort, masterAddressObj);
					}
					
					bsSocket.setReuseAddress(true);
				}
				catch(SocketException e)
				{
					e.printStackTrace();
					
					return;
				}
				
				while(working)
				{
					buf = new byte[defaultPacketBufLength];
					
					bsPacket = new DatagramPacket(buf, defaultPacketBufLength);
					
					try
					{
						bsSocket.receive(bsPacket);
					}
					catch(IOException e)
					{
						e.printStackTrace();
						
						continue;
					}
					
					addr = bsPacket.getAddress();
					
					if(addr != null)
					{
//						System.out.println("5");
						try
						{
							waitingAddressQueue.put(addr);
						}
						catch(InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			
			private DatagramPacket bsPacket;
			private DatagramSocket bsSocket;
			InetAddress addr;
			private byte[] buf;
		};
		
		receptionThread = new Thread() {
			public void run()
			{
//				System.out.println("receptionThread");
				ackSocket = null;
				
				try
				{
					ackSocket = new DatagramSocket();
					
					ackSocket.setReuseAddress(true);
				}
				catch(SocketException e)
				{
					e.printStackTrace();
					
					return;
				}
				
				while(working)
				{
//					System.out.println("6");
					try
					{
						addr = waitingAddressQueue.take();
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
						
						continue;
					}
					
					if(masterAddress != null && addr != null)
					{
//						System.out.println("7");
						ackPacket = new DatagramPacket(masterAddress, masterAddress.length, addr, EdgeFinder.defaultAckPort);
						
						try
						{
							ackSocket.send(ackPacket);
						}
						catch(IOException e)
						{
							e.printStackTrace();
						}
						
						if(receptionEvent != null)
						{
//							System.out.println("8");
							receptionEvent.handler(addr);
						}
					}
				}
			}
			
			private InetAddress addr;
			
			private DatagramPacket ackPacket;
			private DatagramSocket ackSocket;
		};
	}
	
	public void setEventHandler(ReceptionEvent ev)
	{
		receptionEvent = ev;
	}
	
	public void start()
	{
//		System.out.println("start");
		if(!working)
		{
//			System.out.println("9");
			initThreads();
			
			listenerThread.start();
			receptionThread.start();
		}
	}
	
	public void join() throws InterruptedException
	{
		if(working)
		{
			receptionThread.join();
		}
	}
	
	public void stop()
	{
		working = false;
		receptionThread = null;
		listenerThread = null;
	}
	
	public static interface ReceptionEvent // like to callback function
	{
		public void handler(InetAddress addr);
	}
	
	private ReceptionEvent receptionEvent;

	private boolean working;
	
	private Thread receptionThread, listenerThread;
	private ArrayBlockingQueue<InetAddress> waitingAddressQueue;
	
	private byte[] masterAddress;
	private InetAddress masterAddressObj;
	
	private final int defaultPacketBufLength = 64;
	private final int defaultWaitingQueueCapacity = 256;
}
