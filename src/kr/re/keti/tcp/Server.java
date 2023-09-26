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
}
