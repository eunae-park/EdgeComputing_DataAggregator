package kr.re.keti.os;

import java.net.DatagramPacket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.Main;

public class EdgeFinder {
	private static EdgeFinder instance = new EdgeFinder();
	private Broadcast broadcast;
	private ArrayBlockingQueue<DatagramPacket> queue;
	private final int DEFAULT_CAPACITY_SIZE = 50000;
	public static final int DEFAULT_WAITING_TIME = 100;

	public static EdgeFinder getInstance() {
		return instance;
	}

	private EdgeFinder() {
		broadcast = Broadcast.getInstance();
		queue = new ArrayBlockingQueue<>(DEFAULT_CAPACITY_SIZE);
	}

	public void discoverMaster() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

		try {
			Thread.sleep(DEFAULT_WAITING_TIME);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LocalDateTime masterTime = LocalDateTime.parse(Main.programStartTime, formatter);
		String masterIP = Main.deviceIP;

		for (DatagramPacket packet : queue) {
			String packetData = new String(packet.getData()).trim();
			String[] datas = packetData.split("::");
			String packetIP = packet.getAddress().getHostAddress();

			//------------master answer--------------
			if(datas[1].equals("master")) {
				masterIP = packetIP;
				break;
			}

			//-----------------all device answer---------------------
			else {
				String packetTime = datas[2];
				LocalDateTime time = LocalDateTime.parse(packetTime, formatter);
				if(time.isBefore(masterTime)) {
					masterTime = time;
					masterIP = packetIP;
				}
			}
		}

		//----------------------master found log--------------------------
		if(Main.masterIP.equals("None")) {
			if(masterIP.equals(Main.deviceIP)) {
				System.out.println("Waiting for connections from slaves...");
			}
			else {
				System.out.println("* Master found: " + masterIP + "\n");
			}
		}
		else {
			if(!Main.masterIP.equals(masterIP)) {
				System.out.println("* Master(" + Main.masterIP + ") die");
				System.out.println("* Master change to " + masterIP);
			}
		}

		Main.masterIP = masterIP;
		//--------------------------mode setting------------------------
		if(Main.masterIP.equals(Main.deviceIP)) {
			Main.mode = "master";
		}
		else {
			Main.mode = "slave";
		}
		clearQueue();
	}

	public void addPacket(DatagramPacket packet) {
		queue.add(packet);
	}

	public void clearQueue() {
		queue.clear();
	}

	public void response() {
		String data = "ANS::" + Main.mode + "::" + Main.programStartTime;
		broadcast.send(data);
	}

	public void request() {
		String data = "REQ::" + Main.mode + "::" + Main.programStartTime;
		broadcast.send(data);
	}

	public void getLocalIPAddress() {
	}
}
