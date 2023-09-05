package kr.re.keti.agent;

import java.net.Socket;
import java.util.function.Consumer;

public class AgentPacket {
	private Socket socket;
	private String address;
	private int port;
	private byte[] data;
	private Consumer<byte[]> callback;

	public AgentPacket(byte[] data) {
		this.data = data;
	}

	public AgentPacket(Socket socket, byte[] data) {
		super();
		this.socket = socket;
		this.data = data;
	}

	public AgentPacket(String address, int port, byte[] data) {
		super();
		this.address = address;
		this.port = port;
		this.data = data;
	}

	public AgentPacket(Socket socket, String address, int port, byte[] data) {
		this.socket = socket;
		this.address = address;
		this.port = port;
		this.data = data;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public String getAddress() {
		return address;
	}
}
