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
			receiverSocket.setReuseAddress(true);
			
			sendSocket = new DatagramSocket(PortNum.DEFAULT_SEND_PORT);
			sendSocket.setReuseAddress(true);
			sendSocket.setBroadcast(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void close() {
		if(receiverSocket != null) receiverSocket.close();
		if(sendSocket != null) sendSocket.close();
	}
	//--------------------------send packet--------------------------------------------------
	public void send() {
		byte[] buf = new byte[DEFAULT_BUF_LENGTH];
		send(buf);
	}
	public void send(String data) {
		byte[] buf = new byte[DEFAULT_BUF_LENGTH];
	    byte[] b = data.getBytes();
	    int len = Math.min(b.length, buf.length);
	}
}
