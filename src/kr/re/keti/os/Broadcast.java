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
	    System.arraycopy(b, 0, buf, 0, len);
	    send(buf);
	}
	public void send(byte[] data) {
		DatagramPacket packet = null;
		try {
			addr = Main.deviceIP.getBytes();
			packet = new DatagramPacket(addr, addr.length, InetAddress.getByName(DEFAULT_BROADCAST_ADDRESS),PortNum.DEFAULT_RECEIVE_PORT);
			packet.setData(data);
			send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	public void send(DatagramPacket packet) {
		try {
			sendSocket.send(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//------------------------receive packet-----------------------------------------
	public DatagramPacket receive() {
		DatagramPacket packet = null;
		try {
			byte[] buf = new byte[DEFAULT_BUF_LENGTH];
			packet = new DatagramPacket(buf, DEFAULT_BUF_LENGTH);
			receiverSocket.receive(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return packet;
	}
	
	//----------------------get local IP--------------------------------------
	public void selfSend() {
		try {
			addr = Main.deviceIP.getBytes();
			DatagramPacket packet =  new DatagramPacket(addr, addr.length, InetAddress.getByName(DEFAULT_BROADCAST_ADDRESS), PortNum.DEFAULT_SELF_PORT);
			DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	public String selfReceive() {
		try {
			byte[] buf = new byte[DEFAULT_BUF_LENGTH];
			DatagramSocket socket = new DatagramSocket(PortNum.DEFAULT_SELF_PORT);
			DatagramPacket packet = new DatagramPacket(buf, DEFAULT_BUF_LENGTH);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
