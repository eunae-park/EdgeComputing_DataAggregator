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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
