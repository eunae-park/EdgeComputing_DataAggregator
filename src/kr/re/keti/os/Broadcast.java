package kr.re.keti.os;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import kr.re.keti.Main;
import kr.re.keti.PortNum;

public class Broadcast {
	private static final Broadcast instance = new Broadcast();
	private final int DEFAULT_BUF_LENGTH = 64;
	private final String DEFAULT_BROADCAST_ADDRESS = "255.255.255.255";
	
	private byte[] addr;
	private DatagramSocket receiverSocket;
	private DatagramSocket sendSocket;
	
	public static Broadcast getInstance() {
		return instance;
	}
	private Broadcast() {
		setting();
	}
	public void setting() {
		try {
			receiverSocket = new DatagramSocket(PortNum.DEFAULT_RECEIVE_PORT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
