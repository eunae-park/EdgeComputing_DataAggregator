package kr.re.keti.tcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import kr.re.keti.DataProcess;
import kr.re.keti.FileHandler;
import kr.re.keti.FileMonitor;
import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.agent.Agent;

public class UnitShared {
	private static int port = PortNum.KETI_PORT;
	private static Map<String, UnitShared> instances = new HashMap<String, UnitShared>();
	private static Map<String, String> uuidMap = new HashMap<String, String>();
	private static BlockingQueue<UnitShared> queue = new ArrayBlockingQueue<>(500);
	private static Thread processThread = processThread();
	private Agent agent;
	private File file;
	private String fileName;
	private String uuid;
	private int securityLevel;
	private int chunkLength;
	private AtomicInteger chunkShareStatus;
	private AtomicInteger chunkCreateStatus;
	private int progress;
	private AtomicInteger progressCount;
	private int standard;
	private int edgeCount;
	private AtomicInteger readyCount;
	
	private UnitShared() {}
	public static UnitShared getInstance(String fileName, String uuid) {
		if(!instances.containsKey(fileName)) {
			UnitShared unit = new UnitShared();
			unit.agent = Agent.getInstance();
			unit.chunkShareStatus = new AtomicInteger(0);
			unit.chunkCreateStatus = new AtomicInteger(0);
			unit.readyCount = new AtomicInteger(0);
			unit.setFileName(fileName);
			unit.setUuid(uuid);
			instances.put(fileName, unit);
			uuidMap.put(uuid, fileName);
		}
		return instances.get(fileName);
	}
	public static void delInstance(String uuid) {
		if(uuidMap.containsKey(uuid)) {
			String fileName = uuidMap.get(uuid);
			uuidMap.remove(uuid);
			instances.remove(fileName);
		}
		else {
			String fileName = uuidMap.get(uuid);
			System.out.println("instance delete fail");
			System.out.println("uuid: "+uuid+"\tfileName: "+fileName);
		}
	}
	public static UnitShared getInstanceUuid(String uuid) {
		if(uuidMap.containsKey(uuid)) {
			return getInstance(uuidMap.get(uuid), uuid);
		}
		return null;
	}
	public void setStandard(int standard) {
		this.standard = standard;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getUuid() {
		return this.uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public void setLength(int length) {
	}
}
