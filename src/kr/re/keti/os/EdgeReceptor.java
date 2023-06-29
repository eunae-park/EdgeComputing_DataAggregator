package kr.re.keti.os;

import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.Main;

public class EdgeReceptor {
	public static final EdgeReceptor instance = new EdgeReceptor();
	public static ArrayList<String> slaveList = new ArrayList<>();
	private HashMap<String, String> slaveMap = new HashMap<>();
	private final int DEFAULT_WAITING_QUEUE_CAPACITY = 256;
	private Thread listenerThread;
	private Thread responseThread;
	private byte[] deviceIP;
	private Broadcast broadcast;
	//	private Agent agent;

	private ArrayBlockingQueue<DatagramPacket> waitingAddressQeue;

	public static EdgeReceptor getInstance() {
		return instance;
	}

	private EdgeReceptor() {
		//		agent = Agent.getInstance();
		broadcast = Broadcast.getInstance();
		waitingAddressQeue = new ArrayBlockingQueue<>(DEFAULT_WAITING_QUEUE_CAPACITY);
		try {
			deviceIP = InetAddress.getLocalHost().getHostAddress().getBytes();
		} catch (Exception e) {
			e.printStackTrace();
		}
		initThreads();
	}

	public void listenerStart() {
		listenerThread.start();
	}

	public void listenerStop() {
		listenerThread.interrupt();
		;
	}

	public void responseStart() {
		responseThread.start();
	}

	public void responseStop() {
		responseThread.interrupt();
	}

	public void close() {
		if(listenerThread != null)
			listenerStop();
		if(responseThread != null)
			responseStop();
		waitingAddressQeue.clear();
	}

	public void newEdgeListShow() {
		if(slaveList.size() == 0)
			return;
		System.out.println("\n* Slave List");
		for (String slave : slaveList) {
			if(slave.equals(slaveList.get(slaveList.size() - 1)))
				// last slave == new slave
				System.out.println("\t" + slaveMap.get(slave) + " : " + slave + " : new");
			else
				System.out.println("\t" + slaveMap.get(slave) + " : " + slave);
		}
	}

	public void delEdgeListShow(String address) {
		for (String slave : slaveList) {
			if(slave.equals(address)) {
				System.out.println("\t" + slaveMap.get(slave) + " : " + slave + " : delete");
				slaveList.remove(address);
				slaveMap.remove(address);
			}
			else {
				System.out.println("\t" + slaveMap.get(slave) + " : " + slave);
			}
		}
	}

	public void initThreads() {

		listenerThread = new Thread(() -> {
			try {
				EdgeFinder finder = EdgeFinder.getInstance();
				while (!Thread.currentThread().isInterrupted()) {
					DatagramPacket packet = broadcast.receive();
					String packetData = new String(packet.getData());

					//-------------------packet save------------------------
					if(packetData.indexOf("ANS") != -1) {
						finder.addPacket(packet);
					}
					if(packetData.indexOf("ANS") != -1 || packetData.indexOf("REQ") != -1) {
						waitingAddressQeue.put(packet);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		responseThread = new Thread(() -> {
			try {
				String addr;
				DatagramPacket packet;
				while (!Thread.currentThread().isInterrupted()) {
					packet = waitingAddressQeue.take();
					addr = packet.getAddress().getHostAddress();
					if(deviceIP != null && addr != null) {
						String packetData = new String(packet.getData()).trim();
						EdgeFinder finder = EdgeFinder.getInstance();

						String[] datas = packetData.split("::");

						String type = datas[0];
						//						String mode = datas[1];
						//						String time = datas[2];

						if(type.equals("REQ")) {
							finder.response();
							finder.discoverMaster();
						}
						//						System.out.println("DUP Data : "+packetData);
						if(Main.mode.equals("master") && !addr.equals(Main.deviceIP)) {
							if(logWrite(addr)) {
								newEdgeListShow();
								//								DataProcess.initEdgeList(PortNum.KETI_PORT);
								String edgeList = slaveList + "";
								edgeList.replace(Main.masterIP, "");
								//								String request = "{[{REQ::"+Main.masterIP+"::001::EDGE_LIST::"+edgeList+"}]}";
								//								agent.send(request.getBytes());

								//								String request = "{[{REQ::"+Main.masterIP+"::001::EDGE_LIST::"+edgeList+"}]}";
								//								agent.send(request.getBytes());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		listenerThread.setName("UDP_lisener");
		responseThread.setName("UDP_response");

	}

	public boolean logWrite(String slaveAddr) {
		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date nowTime = new Date();
		String logTime = timeFormat.format(nowTime);

		if(slaveList.contains(slaveAddr))
			return false;
	}
}
