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
}
