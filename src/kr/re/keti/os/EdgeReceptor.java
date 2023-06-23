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
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
