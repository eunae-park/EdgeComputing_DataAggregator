package kr.re.keti.agent;

import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import kr.re.keti.Main;
import kr.re.keti.PortNum;

public class Mqtt {
	private MqttClient client;
	private String topic;
	private String clientId = MqttClient.generateClientId();
	private ArrayBlockingQueue<AgentPacket> sendQueue, receiveQueue;
	private Thread publishThread;
	private String address;

	public Mqtt(String topic, ArrayBlockingQueue<AgentPacket> sendQueue, ArrayBlockingQueue<AgentPacket> receiveQueue) {
		this.topic = topic;
		this.sendQueue = sendQueue;
		this.receiveQueue = receiveQueue;
		address = "tcp://" + Main.masterIP + ":" + PortNum.DEFAULT_MQTT_PORT;
		try {
			client = new MqttClient(address, clientId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		process();

	}

	private void process() {
		client.setCallback(new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
				System.out.println("Connection lost");
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				String data = message + "";
				AgentPacket packet = new AgentPacket(data.getBytes());
			}
		}
	}
}
