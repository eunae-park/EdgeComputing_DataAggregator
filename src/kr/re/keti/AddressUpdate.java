package kr.re.keti;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;


public class AddressUpdate {
	int port;
	String address;
	Thread thread;
	public String localAddressUpdate() {
		Random random = new Random();
		
		port = random.nextInt(49151 - 1024 + 1)+1024;
		receive();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		send();
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return address;
		
	}
	private void receive() {
		thread = new Thread(()->{
			try {
				byte[] buf = new byte[1024];
				DatagramSocket socket = new DatagramSocket(port);
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				address = packet.getAddress().getHostAddress();
				socket.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		});
		thread.start();
		
	}
	private void send() {
		Thread thread = new Thread(()->{
			try {
				byte[] buf = "update".getBytes();
				DatagramPacket packet = new DatagramPacket(
					buf, 
					buf.length, 
					InetAddress.getByName("255.255.255.255"), 
					port
				);
				DatagramSocket socket = new DatagramSocket();
				socket.send(packet);
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		thread.start();
		
	}
}
