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
}
