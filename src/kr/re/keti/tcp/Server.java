package kr.re.keti.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.agent.AgentPacket;

public class Server{
	private final int CAPACITY = 5000;
	private final int DEFAULT_BUFFER_SIZE = 5000;
	private ServerSocket serverSocket;
	private ArrayBlockingQueue<Socket> acceptQueue;
	private ArrayBlockingQueue<AgentPacket> receivQueue;
	private Thread acceptThread;
	private Thread receiveThread;
	
	
	public Server(int port, ArrayBlockingQueue<AgentPacket> receivQueue) {
		try {
			serverSocket = new ServerSocket(port);
			this.receivQueue = receivQueue;
			acceptQueue = new ArrayBlockingQueue<>(CAPACITY);
			initThread();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
