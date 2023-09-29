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
	public void start() {
		acceptThread.start();
		receiveThread.start();
	}
	private void initThread() {
		receiveThread = new Thread(()->{
			try {
				while(!Thread.currentThread().isInterrupted()) {
					Socket socket = acceptQueue.take();
					try {
						InputStream inputStream = socket.getInputStream();

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						int len;
						while ((len = inputStream.read(buffer)) != -1) {
						    baos.write(buffer, 0, len);
						    if(inputStream.available() < 1) break;
						}
						byte[] data = baos.toByteArray();
						
						AgentPacket packet = new AgentPacket(socket, data);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
