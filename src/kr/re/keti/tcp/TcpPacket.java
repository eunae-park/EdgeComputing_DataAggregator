package kr.re.keti.tcp;

import java.net.Socket;

public class TcpPacket {
	private Socket socket;
	private int port;
	private int length;
	private byte[] data;
	
	public TcpPacket(Socket socket, byte[] data, int length) {
	}
}
