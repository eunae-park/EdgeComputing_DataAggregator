package kr.re.keti.os;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.agent.Agent;
import kr.re.keti.Ssl;

public class UdpReceptor {
	public static ArrayList<String> edgeList = new ArrayList<>();
	private HashMap<String, String> edgeMap = new HashMap<>();
	
	public final int DEFAULT_RECEIVE_PORT = PortNum.DEFAULT_RECEIVE_PORT;
	public final int DEFAULT_SEND_PORT = PortNum.DEFAULT_SEND_PORT;
}
