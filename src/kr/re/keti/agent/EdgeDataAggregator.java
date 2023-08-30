package kr.re.keti.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import kr.re.keti.PortNum;
import kr.re.keti.database.Database;
import kr.re.keti.tcp.Client;
import kr.re.keti.tcp.Server;

abstract class EdgeDataAggregator {
	protected final SimpleDateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
	private int STANDARD;
	private final int CAPACITY = 5000;
	protected Database database;
	private ArrayBlockingQueue<AgentPacket> sendQueue;
	private ArrayBlockingQueue<AgentPacket> sendMqttQueue;
	private ArrayBlockingQueue<AgentPacket> sendTcpQueue;
	private ArrayBlockingQueue<AgentPacket> sendKafkaQueue;
	private ArrayBlockingQueue<AgentPacket> receiveQueue;
	private Mqtt mqtt;
	private Kafka kafka;
	private Server ketiServer;
	private Server pentaServer;
	private Thread sendThread;
	private Thread receiveThread;
	private Client client;

	public EdgeDataAggregator() {
		STANDARD = 5000;
		sendQueue = new ArrayBlockingQueue<>(CAPACITY);
		receiveQueue = new ArrayBlockingQueue<>(CAPACITY);
		sendMqttQueue = new ArrayBlockingQueue<>(CAPACITY);
		sendTcpQueue = new ArrayBlockingQueue<>(CAPACITY);
		sendKafkaQueue = new ArrayBlockingQueue<>(CAPACITY);
		ketiServer = new Server(PortNum.KETI_PORT, receiveQueue);
		pentaServer = new Server(PortNum.PENTA_PROT, receiveQueue);
		client = new Client(sendTcpQueue);
	}

	public void setStandard(int standard) {
		this.STANDARD = standard;
	}

	public int getStandard() {
		return this.STANDARD;
	}

	public void start() {
		if(sendThread == null || sendThread.getState() == Thread.State.TERMINATED) {
			initSendThread();
			sendThread.start();
		}
		if(receiveThread == null || receiveThread.getState() == Thread.State.TERMINATED) {
			initReceiveThread();
			receiveThread.start();
		}

		mqtt = new Mqtt("/", sendMqttQueue, receiveQueue);
		mqtt.start();

		kafka = new Kafka("keti", sendQueue, receiveQueue);
		kafka.start();

		client.start();
		ketiServer.start();
		pentaServer.start();
	}

	public void stop() {
		if(mqtt != null)
			mqtt.stop();
		if(kafka != null)
			kafka.stop();

		if(sendThread != null) {
			sendThread.interrupt();
			try {
				sendThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(receiveThread != null) {
			receiveThread.interrupt();
			try {
				receiveThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void initReceiveThread() {
		receiveThread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					AgentPacket packet = receiveQueue.take();
					receive(packet);
				}
			} catch (InterruptedException e) {
				return;
			}
		});
		receiveThread.setName("sendThread");
	}

	public void addReceive(AgentPacket packet) {
		try {
			receiveQueue.put(packet);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initSendThread() {
		sendThread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					AgentPacket packet = sendQueue.take();
					Socket socket = packet.getSocket();
					String address = packet.getAddress();
					byte[] data = packet.getData();
					int len = data.length;
					if(socket != null || address != null) { //TCP
						sendTcpQueue.put(packet);
					}
					else if(len < STANDARD) { // MQTT
						sendMqttQueue.put(packet);
					}
					else if(len >= STANDARD) { // Kafka
						sendKafkaQueue.put(packet);
					}
				}
			} catch (InterruptedException e) {
				return;
			}
		});
		sendThread.setName("sendKetiThread");
	}

	public void send(byte[] data) {
		AgentPacket packet = new AgentPacket(data);
		try {
			sendQueue.put(packet);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public byte[] send(String address, byte[] data) {
		AtomicReference<byte[]> response = new AtomicReference<>();
		try {
			if(address != null) {
				CountDownLatch latch = new CountDownLatch(1);
				Consumer<byte[]> callback = responseData -> {
					response.set(responseData);
					latch.countDown();
				};
				int port = portCategorization(new String(data));

				AgentPacket packet = new AgentPacket(address, port, data);
				packet.setCallback(callback);
				sendTcpQueue.put(packet);
				latch.await();
			}
			else {
				send(data);
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return response.get();
	}

	public void send(Socket socket, byte[] data) {
		AgentPacket packet = new AgentPacket(socket, data);
		if(socket != null) {
			String message = new String(data);
			if(message.indexOf("ANS") != -1) {
				try {
					OutputStream outputStream = socket.getOutputStream();
					outputStream.write(data);
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			try {
				sendTcpQueue.put(packet);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendThread() {

	}

	abstract int portCategorization(String massage);

	abstract void receive(AgentPacket packet);

}
