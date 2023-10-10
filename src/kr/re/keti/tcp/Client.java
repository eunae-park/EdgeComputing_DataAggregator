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
						
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						int len;
						while ((len = inputStream.read(buffer)) != -1) {
							baos.write(buffer, 0, len);
							if(inputStream.available() < 1) break;
						}
						byte[] responseData = baos.toByteArray();
						String response = new String(responseData);
						if(response.indexOf("fail") != -1) {
							currReteries++;
							if(currReteries>MAX_RETRIES) {
								break;
							}
							continue;
						}
						else {
							check.set(true);
							if (callback != null) {
								callback.accept(responseData);
							}
							break;
						}
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		thread.setName(socket.getInetAddress().getHostAddress()+"retryTCPThread");
		thread.start();
		try {
			thread.join();
			return check.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return check.get();
	}
	@Override
	public void run() {
		try {
			while(!Thread.currentThread().isInterrupted()) {
				AgentPacket packet = queue.take();
				Thread thread = new Thread(()->{
					try {
						Socket socket = packet.getSocket();
						String address = packet.getAddress();
						int port = packet.getPort();
						byte[] requestData = packet.getData();
						if(socket == null) {
							socket = new Socket(address, port);
						}
						socket.setSoTimeout(DEFAULT_TIMEOUT);
						
						InputStream inputStream = socket.getInputStream();
						OutputStream outputStream = socket.getOutputStream();
						
						outputStream.write(requestData);
						outputStream.flush();
						
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						int len;
						while ((len = inputStream.read(buffer)) != -1) {
							baos.write(buffer, 0, len);
							if(inputStream.available() < 1) break;
						}
						byte[] responseData = baos.toByteArray();
						boolean check = true;
						if(new String(responseData).indexOf("fail") != -1) {
							final Socket finalSocket = socket;
							Thread retryThread = new Thread(()->{
								if(check) {
									Consumer<byte[]> callback = packet.getCallback();
									send(finalSocket, requestData, callback);
								}
							} catch (Exception e) {
							retryThread.start();
								e.printStackTrace();
						else {
							Consumer<byte[]> callback = packet.getCallback();
                            if (callback != null) {
                                callback.accept(responseData);
}
						}
							}
						}
					} catch (Exception e) {
					
						e.printStackTrace();
				thread.setName(packet.getAddress()+"TCPThread");
					}
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
