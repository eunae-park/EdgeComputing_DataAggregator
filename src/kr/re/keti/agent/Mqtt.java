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
}
