package kr.re.keti;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class EdgeFinder
{
	public EdgeFinder() throws UnknownHostException
	{
		masterAddress = null;
		myAddress = null;
	}
	
	public EdgeFinder(String myAddr) throws UnknownHostException
	{
		masterAddress = null;
		myAddress = (myAddr == null)?(null):(InetAddress.getByName(myAddr));
	}
	
	private DatagramPacket createBroadcastPacket() throws UnknownHostException
	{
		DatagramPacket bcPacket = null;
		
		byte[] addr = null;
		
		if(myAddress == null)
		{
			addr = InetAddress.getLocalHost().getHostAddress().getBytes();
		}
		else
		{
			addr = myAddress.getHostAddress().getBytes();
		}
		
		if(addr != null)
		{
			bcPacket = new DatagramPacket(addr, addr.length, InetAddress.getByName(defaultBroadcastAddress), defaultBroadcastPort);
		}

		return bcPacket;
	}
	
	private void broadcaster(DatagramPacket bcPacket) // real broadcasting
	{
		DatagramSocket bcSocket = null;
		
		try
		{
			bcSocket = new DatagramSocket();
			
			bcSocket.setBroadcast(true);
		}
		catch(SocketException e)
		{
			e.printStackTrace();
		}
		
		if(bcSocket != null)
		{
			try
			{
				bcSocket.send(bcPacket);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private boolean ackReceiver()
	{
		boolean masterDiscovered = false;
		boolean ioSuccess = true;
		
		byte[] ackBuf = new byte[defaultAckBufLength];
		
		DatagramSocket ackSocket = null;
		DatagramPacket ackPacket = new DatagramPacket(ackBuf, defaultAckBufLength);
		
		try
		{
			ackSocket = (myAddress == null)?(new DatagramSocket(defaultAckPort)):(new DatagramSocket(defaultAckPort, myAddress));
			
			ackSocket.setSoTimeout(defaultTimeout);

			ackSocket.receive(ackPacket);
		}
		catch(SocketTimeoutException ee)
		{
			ioSuccess = false;
		}
		catch(IOException e)
		{
			ioSuccess = false;
			
			e.printStackTrace();
		}
		
		if(ioSuccess)
		{ //slave
			masterDiscovered = true;
			
			masterAddress = ackPacket.getAddress();
		}
		else
		{ //master
			masterAddress = null;
		}
		
		return masterDiscovered;
	}
	
	public boolean discoverMaster() 
	{
		boolean masterFound = false;
		DatagramPacket bcPacket = null;
				
		try
		{
			bcPacket = createBroadcastPacket();
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
		
		if(bcPacket != null)
		{
			final DatagramPacket pkt = bcPacket;
			
			Thread broadcastThread = new Thread() {
				public void run()
				{
					try
					{
						Thread.sleep(defaultBroadcastDelay);
					}
					catch(InterruptedException e)
					{
						e.printStackTrace();
					}
					
					for(int i = 0; i < numOfTry; ++i) // repeat 3times - packet loss X
					{
						broadcaster(pkt);
					}
				}
			};
			
			broadcastThread.start();
			
			masterFound = ackReceiver(); // 
		}
		
		return masterFound;
	}
	
	public void manualMaster() 
	{
		boolean masterDiscovered = false;
		boolean ioSuccess = true;
		
		byte[] ackBuf = new byte[defaultAckBufLength];
		
		DatagramSocket ackSocket = null;
		DatagramPacket ackPacket = new DatagramPacket(ackBuf, defaultAckBufLength);
		
		try
		{
			ackSocket = (myAddress == null)?(new DatagramSocket(defaultAckPort)):(new DatagramSocket(defaultAckPort, myAddress));
			
			ackSocket.setSoTimeout(defaultTimeout);

			ackSocket.receive(ackPacket);
		}
		catch(SocketTimeoutException ee)
		{
			ioSuccess = false;
		}
		catch(IOException e)
		{
			ioSuccess = false;
			
			e.printStackTrace();
		}
		
		if(ioSuccess)
		{ //slave
			masterDiscovered = true;
			
			masterAddress = ackPacket.getAddress();
		}
		else
		{ //master
			masterAddress = null;
		}
	}
	public void manualSlave(InetAddress ip) 
	{
		masterAddress = ip;
	}

	public InetAddress getMasterAddress()
	{
		return masterAddress;
	}
	
	private InetAddress masterAddress;
	private InetAddress myAddress;
	
	private final int numOfTry = 3;
	private final int defaultTimeout = 1000; //ms -> 1s wait but 
	private final long defaultBroadcastDelay = 300;
	private final int defaultAckBufLength = 64;
	private final String defaultBroadcastAddress = "255.255.255.255";
	public final static int defaultBroadcastPort = 5678;
	public final static int defaultAckPort = 5679;
}
