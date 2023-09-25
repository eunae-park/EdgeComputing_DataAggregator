package kr.re.keti.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.simple.SimpleLogger;

import kr.re.keti.Main;
import kr.re.keti.PortNum;

public class Kafka {
	private String topic;
	private ArrayBlockingQueue<AgentPacket> sendQueue;
	private ArrayBlockingQueue<AgentPacket> receiveQueue;
	private KafkaProducer<String, byte[]> producer;
	private KafkaConsumer<String, byte[]> consumer;
	private Thread producerThread;
	private Thread consumerThread;
	private String serverIP;

	public Kafka(String topic, ArrayBlockingQueue<AgentPacket> sendQueue, ArrayBlockingQueue<AgentPacket> receiveQueue) {
		System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "OFF");

		serverIP = Main.masterIP + ":" + PortNum.DEFAULT_KAFKA_PORT;
		this.topic = topic;
		this.sendQueue = sendQueue;
		this.receiveQueue = receiveQueue;
		producer();
		consumer();
	}

	public void producer() {
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverIP);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

		producer = new KafkaProducer<>(properties);

	}

	public void consumer() {
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverIP);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, Main.uuid);
		//        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		properties.put(ConsumerConfig.CLIENT_ID_CONFIG, Main.uuid);
		properties.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, (Main.uuid));

		consumer = new KafkaConsumer<>(properties);
	}

	public void admin() {
		Properties properties = new Properties();
		properties.put("bootstrap.servers", serverIP);
		AdminClient adminClient = AdminClient.create(properties);

		int numPartitions = 3;
		short replicationFactor = 1;

		// NewTopic 객체 생성
		NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor);

		// 토픽 생성
		adminClient.createTopics(Collections.singletonList(newTopic));

		adminClient.close();

	}

	public void start() {
		initThread();
		producerThread.start();
		consumerThread.start();
	}

	public void stop() {
		if(producerThread != null)
			producerThread.interrupt();
		if(consumerThread != null)
			consumerThread.interrupt();
		producer.close();
		consumer.close();
	}

	public void initThread() {
		producerThread = new Thread(() -> {
			try {
				while (!Thread.currentThread().isInterrupted()) {
					AgentPacket packet = sendQueue.take();
					byte[] data = packet.getData();
					ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, data);

					producer.send(producerRecord, new Callback() {
						@Override
						public void onCompletion(RecordMetadata metadata, Exception exception) {
							if(exception != null) {
								// 데이터 전송이 실패한 경우
								//					            System.err.println("Failed to send data: " + exception.getMessage());
							}
							else {
								// 데이터 전송이 성공한 경우
								//						            System.out.println("Data sent successfully: " + metadata);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}
