package kr.re.keti.os;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.PortNum;

public class TcpReceptor {
	public static ArrayList<String> slaveList = new ArrayList<>();
	private HashMap<String, String> slaveMap = new HashMap<>();
	private final int CAPACITY = 5000;
}