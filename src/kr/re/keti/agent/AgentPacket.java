package kr.re.keti.agent;

import java.net.Socket;
import java.util.function.Consumer;

public class AgentPacket {
	private Socket socket;
	private String address;
	private int port;
	private byte[] data;
}
