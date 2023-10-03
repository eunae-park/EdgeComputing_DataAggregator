package kr.re.keti.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import kr.re.keti.agent.AgentPacket;

public class Client extends Thread{
	private final int DEFAULT_TIMEOUT = 10 * 1000;
	private final int DEFAULT_BUFFER_SIZE = 5000;
	private final int MAX_RETRIES = 3;
	private ArrayBlockingQueue<AgentPacket> queue;
	
	public Client(ArrayBlockingQueue<AgentPacket> queue) {
		this.queue = queue;
	}
	public boolean send(Socket socket, byte[] data, Consumer<byte[]> callback) {
		AtomicBoolean check = new AtomicBoolean(false);
		Thread thread = new Thread(()->{
			try {
				int currReteries = 0;
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				while(!Thread.currentThread().isInterrupted()) {
					try {
						outputStream.write(data);
						outputStream.flush();
						
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
