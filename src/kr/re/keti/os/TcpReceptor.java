package kr.re.keti.os;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.PortNum;

public class TcpReceptor{
	private ArrayList<String> slaveList;
	private HashMap<String, String> slaveMap = new HashMap<>();
	private final int CAPACITY = 5000;
	private ServerSocket serverSocket;
	private ArrayBlockingQueue<Socket> acceptQueue;
	private Thread acceptThread;
	private Thread receiveThread;
	private final int PORT = PortNum.DEFAULT_TCP_RECEPTOR_PORT;
	
	public TcpReceptor(ArrayList<String> slaveList) {
		this.slaveList = slaveList;
		try {
			serverSocket = new ServerSocket(PortNum.DEFAULT_TCP_RECEPTOR_PORT);
			acceptQueue = new ArrayBlockingQueue<>(CAPACITY);
			initThread();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void start() {
		acceptThread.start();
		receiveThread.start();
	}
	public void stop() {
		acceptThread.interrupt();;
		receiveThread.interrupt();;
		acceptQueue.clear();
		
	}
	private void initThread() {
		receiveThread = new Thread(()->{
			try {
				while(!Thread.currentThread().isInterrupted()) {
					Socket socket = acceptQueue.take();
					String address = socket.getInetAddress().getHostAddress();
					if(!slaveList.contains(address)) {
						SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date nowTime = new Date();
						String logTime = timeFormat.format(nowTime);
						slaveList.add(address);
						slaveMap.put(address, logTime);
						newEdgeListShow();

						try {
							FileWriter writer = new FileWriter("edge_ipList.txt", false);
							writer.write("master\n");
							writer.flush();

							for(int i=0; i<slaveList.size(); i++) {
								writer.write(slaveList.get(i)+"\n");
								writer.flush();
							}
							
							if(writer != null) writer.close();
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (InterruptedException e) {
//				e.printStackTrace();
			}
		});
		acceptThread = new Thread(()->{
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Socket clientSocket = serverSocket.accept();
					acceptQueue.put(clientSocket);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
//					e.printStackTrace();
				}
			}
		});
		
		acceptThread.setName("TCP_AcceptThread");
		receiveThread.setName("TCP_ReceiveThread");
	}
	public boolean check(String masterIP) {
		try {
			String address = InetAddress.getLocalHost().getHostAddress();
			if(address.equals(masterIP)) return true;
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		try(Socket socket = new Socket(masterIP, PORT);
		){
			socket.setSoTimeout(1000);
		} catch (UnknownHostException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	public void newEdgeListShow() {
		if(slaveList.size() == 0 )return;
		System.out.println("\n* Slave List");
		for(String slave : slaveList) {
			if(slave.equals(slaveList.get(slaveList.size()-1)))
				// last slave == new slave
				System.out.println("\t"+slaveMap.get(slave)+" : "+slave+" : new");
			else
				System.out.println("\t"+slaveMap.get(slave)+" : "+slave);
		}
	}
}