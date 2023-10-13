package kr.re.keti.tcp;

import java.net.Socket;

public class TcpPacket {
	private Socket socket;
	private int port;
	private int length;
	private byte[] data;
	
	public TcpPacket(Socket socket, byte[] data, int length) {
		this.socket = socket;
		this.data = data;
		this.length = length;
	}
	public TcpPacket(int port, byte[] data, int length) {
		this.port = port;
		this.data = data;
		this.length = length;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public int getPort() {
	}
}
