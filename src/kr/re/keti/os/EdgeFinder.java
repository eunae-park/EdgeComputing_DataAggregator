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
}
