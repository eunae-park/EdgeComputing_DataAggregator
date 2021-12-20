package kr.re.keti;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.SimpleDateFormat;

public final class EdgeDeviceInfoClient
{
	public EdgeDeviceInfoClient(String addr, int socketType)
	{
		agentSocket = null;
		replySocket = null;
		
		streamSocket = null;
		inputStream = null;
		
		isWaiting = false;
		answerData = null;
		
		currentSocketType = socketType;
		
		// try catch 여기서 각각 하기 
		try {
			targetAddress = InetAddress.getByName(addr);
			if(currentSocketType == socketUDP)
			{
				agentSocket = new DatagramSocket();
				replySocket = new DatagramSocket(UDPSocketAgent.defaultReplyPort);
			}
			else if(currentSocketType == socketTCP)
			{
				int numOfRetry = 0;
//				System.out.println("!! EdgeDeviceInfoClient : " + TCPSocketAgent.defaultPort);
				
				do
				{
//					System.out.println("!! EdgeDeviceInfoClient : " + numOfRetry);
					streamSocket = new Socket(targetAddress, TCPSocketAgent.defaultPort);
					
					++numOfRetry;
				}
				while(streamSocket == null && numOfRetry <= connectionRetryLimit);
				
				if(streamSocket != null && streamSocket.isConnected())
				{
					inputStream = streamSocket.getInputStream();
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println("!! EdgeDeviceInfoClient : ");
		
	}
	public EdgeDeviceInfoClient(String addr, int socketType, int port)
	{
		SimpleDateFormat log_format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
		agentSocket = null;
		replySocket = null;
		
		streamSocket = null;
		inputStream = null;
		
		isWaiting = false;
		
		currentSocketType = socketType;
		
		// try catch 여기서 각각 하기 
		try {
			targetAddress = InetAddress.getByName(addr);
			if(currentSocketType == socketUDP)
			{
				agentSocket = new DatagramSocket();
				replySocket = new DatagramSocket(UDPSocketAgent.defaultReplyPort);
			}
			else if(currentSocketType == socketTCP)
			{
				int numOfRetry = 0;
				
				do
				{
					TCPSocketAgent.defaultPort = port;
//					System.out.println("!! EdgeDeviceInfoClient : " + TCPSocketAgent.defaultPort);
					streamSocket = new Socket(targetAddress, port);
					
					++numOfRetry;
				}while(streamSocket == null && numOfRetry <= connectionRetryLimit);
				
				if(streamSocket != null && streamSocket.isConnected())
				{
					inputStream = streamSocket.getInputStream();
				}
			}
		} catch (UnknownHostException e) {
			streamSocket = null;
			System.out.println(log_format.format(new Date(System.currentTimeMillis())) + " client UnknownHostException");
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (SocketException e) {
			streamSocket = null;
			System.out.println(log_format.format(new Date(System.currentTimeMillis())) + " client SocketException");
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
			streamSocket = null;
			System.out.println(log_format.format(new Date(System.currentTimeMillis())) + " client IOException");
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
	}
	
	public void sendPacket(byte[] data, int len)
	{
//		String str = new String(data);
//		System.out.println("!! EdgeDeviceInfoClient : " + str); //send error
//		if(str.indexOf("{[{")==-1 || str.indexOf("}]}")==-1)
//			return ;
		answerData = null; // 
		
		try {

			if(currentSocketType == socketUDP)
			{
				if(data != null && len >= minimumPacketLength)
				{
					DatagramPacket pkt = new DatagramPacket(data, len, targetAddress, UDPSocketAgent.defaultAgentPort);
					agentSocket.send(pkt);
				}
			}
			else if(currentSocketType == socketTCP)
			{
				if(streamSocket.isConnected() && data != null && len >= minimumPacketLength)
				{
					streamSocket.getOutputStream().write(data, defaultDataStartPosition, len);
					streamSocket.getOutputStream().flush();
				}
			}
			else
			{
				return;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startWaitingResponse()
	{
		Runnable runnable;
		
		if(currentSocketType == socketUDP)
		{
			runnable = new UDP_ResponseWaiter();
		}
		else if(currentSocketType == socketTCP) 
		{
			runnable = new TCP_ResopnseWaiter();
		}
		else
		{
			return;
		}
		
		responseWaiter = new Thread(runnable);
		
		responseWaiter.start();
	}
	
	public void stopWaitingResponse()
	{
		isWaiting = false;
	}

	public void stopRequest()
	{
		isWaiting = false;
		try
		{
			if(inputStream != null)
			{
				inputStream.close();
				inputStream = null;
			}
		}
		catch(IOException e)
		{
			inputStream = null;
		}
		
		try
		{
			if(streamSocket != null)
			{
				streamSocket.close();
				streamSocket = null;
			}
		}
		catch(IOException e)
		{
			streamSocket = null;
		}
	}
	
	public boolean streamSocket_alive()
	{
		if(streamSocket == null)
			return false;
		return true;
	}

	private int currentSocketType;
	private boolean isWaiting;
	private Thread responseWaiter;
	private DatagramSocket agentSocket;
	private DatagramSocket replySocket;
	
	private Socket streamSocket;
	private InputStream inputStream;
	
	private InetAddress targetAddress;
	
	private final int minimumPacketLength = 0;
	private final int defaultDataStartPosition = 0;
	private final int connectionRetryLimit = 10;
	
	public static final int socketUDP = 0x1111;
	public static final int socketTCP = 0x2222;
//	public String answerData = null;
	public byte[] answerData = null;
	public int permission = 0;
	
	private class UDP_ResponseWaiter implements Runnable
	{
		@Override
		public void run()
		{
			answerData = null;
			isWaiting = true;
			
			byte[] data = null;
			byte[] packetData = new byte[UDPSocketAgent.defaultPacketSize];
			DatagramPacket responsePacket = new DatagramPacket(packetData, UDPSocketAgent.defaultPacketSize, targetAddress, UDPSocketAgent.defaultReplyPort);;
			
			while(isWaiting) // joo
			{
				java.util.Arrays.fill(packetData, (byte)0); // joo
				String msg = "";
				int cnt=0, total_len=0;
				byte[] msg_b=null;
				try
				{
					while(msg.indexOf("}]}")==-1)
					{
						replySocket.receive(responsePacket); // joo
						if(total_len == 0)
						{
							total_len += packetData.length;
							data = new byte[total_len];
							System.arraycopy(packetData, 0, data, 0, packetData.length);

							msg_b =  data;
						}
						else
						{
							total_len += packetData.length;
							data = new byte[total_len];
							System.arraycopy(msg_b, 0, data, 0, msg_b.length);
							System.arraycopy(packetData, 0, data, msg_b.length, packetData.length);

							msg_b =  data;
							
						}
						msg += new String(data, "UTF-8");
						cnt ++;
//						System.out.println("!! TCP_ResopnseWaiter : " + msg.indexOf("}]}")); //
					}
				}
				catch(IOException e) // joo
				{
					continue;
				}
				
				
				answerData = data;
				String[] array = msg.substring(8, msg.indexOf("}]}")).split("::");
				if(array[1].equals("004"))
				{
					if(array[3]==array[4] && cnt>=Integer.parseInt(array[3]))
						isWaiting = false;
				}
				else
					isWaiting = false;
			}
		}
	}
	
	private class TCP_ResopnseWaiter implements Runnable
	{
		@Override
		public void run()
		{
//			answerData = null;
			isWaiting = true;
			
			if(inputStream == null)
			{
				return;
			}
			
			byte[] packetData = new byte[TCPSocketAgent.defaultPacketSize];
			byte[] data_b = null;
			byte[] data = null;
			
			int datacnt=0;
//			String[] data = null;
			while(isWaiting) // joo
			{
				java.util.Arrays.fill(packetData, (byte)0); // joo
				String msg = "";
				int cnt = 0, total_len=0;
				try
				{
					int len=0;
					while(msg.indexOf("}]}")==-1)
					{
//						System.out.println("!! TCP_ResopnseWaiter : " + isWaiting);
						if(!isWaiting) // false면
						{
							msg = "retry";
							break;
						}
						if((len = inputStream.read(packetData)) != -1)
						{
							if(total_len == 0)
							{
								total_len += len;
								data = new byte[total_len];
								
								System.arraycopy(packetData, 0, data, 0, len);
								data_b =  data;
							}
							else
							{
								total_len += len;
								data = new byte[total_len];
								
								System.arraycopy(data_b, 0, data, 0, data_b.length);
								System.arraycopy(packetData, 0, data, data_b.length, len);
								data_b =  data;
							}
//							System.out.println("!! answer while : " + new String(packetData));
//							System.out.println("!! answer while : " + new String(data_b));
//							System.out.println("!! answer while : " + new String(data));
						}
						
//						System.out.println("!! len : " + total_len);
						if(data != null)
							msg = new String(data, "UTF-8"); //"EUC-KR"
//						System.out.println("!! answer : " + msg);
						cnt ++;
						Thread.sleep(10);
					}
//					Thread.sleep(100);
				}
				catch(IOException e) // joo
				{
					continue;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
//				System.out.println("!! TCP_ResopnseWaiter : " + new String(packetData));
//				System.out.println("!! TCP_ResopnseWaiter2 : " + msg);
				if(msg.equals(""))
					continue ;
				else if(msg.equals("retry"))
				{
					answerData = data; //new String(packetData);
					isWaiting = false;
				}
				else
				{
					String[] array = msg.substring(8, msg.indexOf("}]}")).split("::");
					if(array[1].equals("004"))
					{
						if(array[3]==array[4] && cnt>=Integer.parseInt(array[3]))
							isWaiting = false;
					}
					else if(array[1].equals("405"))
					{
						if(array[2].equals("sha"))
							isWaiting = false;
					}
					else
						isWaiting = false;
				}
				
				answerData = data; //new String(packetData);
			}

		}

	}
	
/*	
	public static void main(String[] args) throws Exception
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String readLine;

		System.out.print("\nInput Agent IP: ");
		readLine = reader.readLine();
		//EdgeDeviceInfoClientTest clientTester = new EdgeDeviceInfoClientTest(readLine, EdgeDeviceInfoClientTest.socketUDP);
		EdgeDeviceInfoClient clientTester = new EdgeDeviceInfoClient(readLine, EdgeDeviceInfoClient.socketTCP);
//		EdgeDeviceInfoClientTest clientTester = new EdgeDeviceInfoClientTest("127.0.0.1", EdgeDeviceInfoClientTest.socketTCP);
		
		clientTester.startWaitingResponse();

		do
		{
			System.out.print("\nInput text: ");
			readLine = reader.readLine();
		
			clientTester.sendPacket(readLine.getBytes(), readLine.length());
		} while(!readLine.equals("exit"));

	}
*/	
}
