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
	}
}
