package kr.re.keti.os;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.agent.Agent;
import kr.re.keti.Ssl;

public class UdpReceptor {
	public static ArrayList<String> edgeList = new ArrayList<>();
	private HashMap<String, String> edgeMap = new HashMap<>();
	
	public final int DEFAULT_RECEIVE_PORT = PortNum.DEFAULT_RECEIVE_PORT;
	public final int DEFAULT_SEND_PORT = PortNum.DEFAULT_SEND_PORT;
	private final int DEFAULT_BUF_LENGTH = 64;
	private final String DEFAULT_BROADCAST_ADDRESS = "255.255.255.255";
	
	private ArrayBlockingQueue<DatagramPacket> queue;
	private DatagramSocket receiveSocket, sendSocket;
	private Thread receiveThread, sendThread;
	private Agent agent;
	
	public UdpReceptor() {
		try {
			queue = new ArrayBlockingQueue<>(5000);
			receiveSocket = new DatagramSocket(DEFAULT_RECEIVE_PORT);
			receiveSocket.setReuseAddress(true);
			
			sendSocket = new DatagramSocket(DEFAULT_SEND_PORT);
			sendSocket.setReuseAddress(true);
			sendSocket.setBroadcast(true);
			sendSocket.setSoTimeout(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public String getMaster() {
		String masterIP = "none";
		
		try {
			byte[] buf = new byte[DEFAULT_BUF_LENGTH];
			send(DEFAULT_RECEIVE_PORT, Main.uuid.getBytes());
			DatagramPacket packet = new DatagramPacket(buf, DEFAULT_BUF_LENGTH);
			sendSocket.receive(packet);
			masterIP= packet.getAddress().getHostAddress();
			

			byte[] request = ("{[{REQ::"+masterIP+"::020::EDGE_KEYS}]}").getBytes();
			String response = new String(agent.send(masterIP, request));
			String responseData = response.split("::")[3];
			String[] datas = response.split(":");
			for(String data : datas) {
				String uuid = data.substring(0, 36);
				String key = data.substring(36, data.length());
				byte[] keyData = key.getBytes();
				Ssl.addKey(responseData, uuid, keyData);
			}
			String path = Ssl.getPath()+"private/private.key";
			byte[] keyData = Files.readAllBytes(Path.of(path));
			String key = new String(keyData);
			agent.send(("{[{REQ::"+Main.deviceIP+"::019::"+Main.uuid+"::"+key+"}]}").getBytes());
//			e.printStackTrace();
//			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return masterIP;
	}
	public void start() {
		agent = Agent.getInstance();
		queue = new ArrayBlockingQueue<>(5000);
		receiveThread = new Thread(()->{
			while(!Thread.currentThread().isInterrupted()) {
				byte[] buf = new byte[DEFAULT_BUF_LENGTH];
				DatagramPacket packet = new DatagramPacket(buf, DEFAULT_BUF_LENGTH);
				try {
					receiveSocket.receive(packet);
					queue.put(packet);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		sendThread = new Thread(()->{
			while(!Thread.currentThread().isInterrupted()) {
				try {
					DatagramPacket packet = queue.take();
					String address = packet.getAddress().getHostAddress();
					String uuid = new String(packet.getData());
					if(address.equals(Main.deviceIP)) continue;
					send(packet.getPort(), Main.uuid.getBytes());
					newEdge(address, uuid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		receiveThread.setName("UDP_Listner");
		receiveThread.start();
		
		sendThread.setName("UDP_Send");
		sendThread.start();
	}
	public void stop() {
		queue.clear();
		if(sendThread != null) sendThread.interrupt();
		if(receiveThread != null) receiveThread.interrupt();
	}
	private void send(int targetPort, byte[] data) {
		try {
			DatagramPacket packet = createPacket(targetPort, data);
			sendSocket.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private DatagramPacket createPacket(int port, byte[] data) {
		try {
			DatagramPacket packet = new DatagramPacket(
				data,
				data.length,
				InetAddress.getByName(DEFAULT_BROADCAST_ADDRESS),
				port
			);
			return packet;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private void newEdge(String address, String uuid) {
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date nowTime = new Date();
		String logTime = timeFormat.format(nowTime);
		
		if(edgeList.contains(address)) {
			System.out.println("\t"+edgeMap.get(address)+" : "+address +" : delete");
			edgeList.remove(address);
			edgeMap.remove(address);
		}
		
		edgeList.add(address);
		edgeMap.put(address, logTime);
		
		newEdgeLog();
		if(!logWrite()) {
			System.out.println("edge_ipList write error");
		}
		String list = getEdgeList();
		agent.send(("{[{REQ::"+address+"::001::EDGE_LIST::"+list+"}]}").getBytes());
		
		
	}
	public void newEdgeLog() {
		if(edgeList.size() == 0) return;
		System.out.println("\n* Slave List");
		for(String slave : edgeList) {
			if(slave.equals(edgeList.get(edgeList.size()-1)))
				// last slave == new slave
				System.out.println("\t"+edgeMap.get(slave)+" : "+slave+" : new");
			else
				System.out.println("\t"+edgeMap.get(slave)+" : "+slave);
		}
		
	}
	public boolean logWrite() {
		
		try {
			FileWriter writer = new FileWriter("edge_ipList.txt", false);
			writer.write("master\n");
			writer.flush();

			for(int i=0; i<edgeList.size(); i++) {
				writer.write(edgeList.get(i)+"\n");
				writer.flush();
			}
			
			if(writer != null) writer.close();
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public String getEdgeList() {
	}
}
