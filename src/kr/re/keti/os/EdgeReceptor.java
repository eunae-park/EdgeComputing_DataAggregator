package kr.re.keti.os;

import java.io.FileWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import kr.re.keti.Main;

public class EdgeReceptor {
	public static final EdgeReceptor instance = new EdgeReceptor();
	public static ArrayList<String> slaveList = new ArrayList<>();
}
