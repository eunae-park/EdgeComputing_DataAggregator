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
		this.chunkLength = length;
	}
	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public void countAdd(String address, String fileName) {
		int progressCount = this.progressCount.incrementAndGet();
		float status = 100;
		if(this.progress>0) {
			status = ((float)progressCount/this.progress)*100;
		}
		
		FileHandler handler = FileHandler.getInstance();
		handler.modifyRecord(this.uuid, status+"");
		handler.modifyRecord(this.uuid, "sharing("+status+"%)", 4);
//		System.out.println(progressCount+"/"+this.progress+"\t"+status);
		if(status>=100) {
			String log = Main.deviceIP+"#"+this.uuid+"#"+this.fileName+"#"+this.securityLevel+"#complte"+100;
			byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "500", log);
			this.agent.send(message);
			
			handler.modifyRecord(this.uuid, "complte", 4);
			FileMonitor.unignoreFile(this.fileName);
			delInstance(this.uuid);
		}
	}
//	public void countAdd() {
//		this.progressCount.incrementAndGet();
//		float status = 100;
//		if(this.progress>0) {
//			status = ((float)this.progressCount.incrementAndGet()/this.progress)*100;
//		}
//		String log = Main.deviceIP+"#"+this.uuid+"#"+this.fileName+"#"+this.securityLevel+"#sharing("+status+"%)#"+status;
//		byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "300", log);
//		FileHandler handler = FileHandler.getInstance();
//		handler.modifyRecord(this.uuid, status+"");
//		handler.modifyRecord(this.uuid, "sharing("+status+"%)", 4);
//
//		if(status>=100) {
//			handler.modifyRecord(this.uuid, "complte", 4);
//			FileMonitor.unignoreFile(this.fileName);
//			delInstance(this.uuid);
//			
//		}
//	}
	public static void send(UnitShared unit) {
		try {
			queue.put(unit);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static Thread processThread() {
		processThread = new Thread(()->{
			while(!Thread.currentThread().isInterrupted()) {
				try {
					UnitShared unit = queue.take();
					unit.preparData();
					for(int i=0; i<30; i++) {
						if(unit.readyCount.get() == unit.edgeCount) {
							unit.share();
							while(true) {
								int progressCount = unit.progressCount.get();
								unit.loadingBar("progress", progressCount, unit.progress);
								if(progressCount >= unit.progress) {
									byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "111", unit.uuid);
									unit.agent.send(message);
									break;
								}
								Thread.sleep(100);
							}
							break;
						}
						Thread.sleep(100);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		processThread.setName("UnitProcessThread");
		processThread.start();
		return processThread;
	}
	private void preparData() {
			this.edgeCount = DataProcess.requestEdgeList(port).split(", ").length;
			this.chunkLength = (int) Math.ceil((double) this.file.length() / standard);
			this.chunkCreate();
			this.progress = chunkLength*this.edgeCount;
			byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "399", this.fileName, this.uuid, chunkLength+"");
			this.agent.send(message);
	}
	public void addReadyCount() {
		this.readyCount.incrementAndGet();
//		if(this.readyCount.incrementAndGet() == this.edgeCount) {
//			this.share();
//			System.out.println(this.uuid+"\tshare start");
//		}
//		System.out.println(this.uuid+"\t"+this.readyCount.get()+"/"+this.edgeCount+"\tedge");
	}
	private void share() {
		// 스레드 풀 생성
		ExecutorService executor = Executors.newFixedThreadPool(10); // 10개의 스레드를 가진 스레드 풀
		
		this.progressCount = new AtomicInteger(0);
		for(int i=0; i<chunkLength; i++) {
			final int fileCount = i;
			executor.submit(() -> { // 스레드 풀에 작업 제출
				String fileName = file.getName()+"_"+fileCount;
				String filePath = Main.storageFolder+"chunk/"+fileName;
				File chunkFile = new File(filePath);
				byte[] data = DataProcess.readFileToByteArray(chunkFile);
				String encodingData = Base64.getEncoder().encodeToString(data);
				fileName = this.uuid+"_"+fileCount;
				byte[] chunkMessage = DataProcess.messageCreate("REQ", Main.deviceIP, "400", fileName, data.length+"", encodingData);
				this.agent.send(chunkMessage);
//				System.out.println("send data: "+LocalTime.now().format(DateTimeFormatter.ofPattern("H:m:s.SSS")));
		}
	}
		
		executor.shutdown(); // 모든 작업이 완료되면 스레드 풀 종료
}
	public int chunkCreate() {
	    FileHandler handler = FileHandler.getInstance();
	    if(handler.isExists(this.uuid)) {
	        handler.deleteRecord(this.uuid);			
	}
	    StringBuilder logBuilder = new StringBuilder();
	    logBuilder.append(Main.deviceIP).append("#").append(this.uuid).append("#").append(this.fileName).append("#").append(this.securityLevel).append("#loding(0%)#").append(0);
	    
	    handler.addRecord(logBuilder.toString());
	    byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "300", logBuilder.toString());
//	    this.agent.send(message);
	    
	    int maxCount = (int) Math.ceil((double) this.file.length() / standard);

	    final int CHUNK_SIZE = this.standard;
	    ExecutorService executor = Executors.newFixedThreadPool(10); // Thread pool with 10 threads

	    loadingBar("loading", 0, this.chunkLength);
	    new Thread(()->{
	    	while(true) {
	    		loadingBar("loading", chunkCreateStatus.get(), this.chunkLength);
	    		try {
					Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    		if(chunkCreateStatus.get() == chunkLength || chunkCreateStatus.get() == -1) {
		    		loadingBar("loading", chunkCreateStatus.get(), this.chunkLength);
		    		System.out.println();
	    			break;
			}
		}
	});
	    try (InputStream inputStream = new FileInputStream(this.file)) {
	        File directory = new File(Main.storageFolder + "chunk/");
	        if (!directory.exists()) directory.mkdirs();

	        byte[] buffer = new byte[CHUNK_SIZE];
	        int bytesRead;
	        while ((bytesRead = inputStream.read(buffer)) != -1) {
	            final byte[] bufferCopy = Arrays.copyOf(buffer, bytesRead);
	            final int currentChunkCount = chunkCreateStatus.getAndIncrement();
	            executor.submit(() -> {
	                String chunkFileName = this.file.getName() + "_" + currentChunkCount;
	                String chunkFilePath = directory.getPath() + File.separator + chunkFileName;
	                try (OutputStream outputStream = new FileOutputStream(chunkFilePath)) {
	                    outputStream.write(bufferCopy);
	} catch (Exception e) {
		e.printStackTrace();
	}
	                float chunkStatus = ((currentChunkCount)/(float) maxCount)*100;
	                StringBuilder threadLogBuilder = new StringBuilder();
	                threadLogBuilder.append(Main.deviceIP).append("#").append(this.uuid).append("#").append(this.fileName).append("#").append(this.securityLevel).append("#loding(").append(chunkStatus).append("%)#").append(chunkStatus);
	                String threadLog = threadLogBuilder.toString();

	                byte[] threadMessage = DataProcess.messageCreate("REQ", Main.deviceIP, "300", threadLog);
	                handler.modifyRecord(this.uuid, String.valueOf(chunkStatus));
	                handler.modifyRecord(this.uuid, "loding("+chunkStatus+"%)", 4);
//	                this.agent.send(threadMessage);
	}
	}
	        executor.shutdown(); // Shutdown the executor
	        while (!executor.isTerminated()) {} // Wait until all tasks are finished

	        float chunkStatus = ((chunkCreateStatus.get())/(float) maxCount)*100;
            StringBuilder threadLogBuilder = new StringBuilder();
            threadLogBuilder.append(Main.deviceIP).append("#").append(this.uuid).append("#").append(this.fileName).append("#").append(this.securityLevel).append("#loding(").append(chunkStatus).append("%)#").append(chunkStatus);
            String threadLog = threadLogBuilder.toString();
            byte[] threadMessage = DataProcess.messageCreate("REQ", Main.deviceIP, "300", threadLog);
            handler.modifyRecord(this.uuid, String.valueOf(chunkStatus));
            handler.modifyRecord(this.uuid, "loding("+chunkStatus+"%)", 4);
            this.agent.send(threadMessage);
            
	        return chunkCreateStatus.get();
	} catch (Exception e) {
		e.printStackTrace();
	}
	    return -1;
	}


	
	
	public void receive(String address, String fileName, byte[] data) {
		String filePath = Main.storageFolder+"chunk/"+fileName;
		if(!this.fileWrite(filePath, data)) {
			return;
		}
	    
	    String number = fileName.substring(fileName.indexOf("_"), fileName.length());

	    if(chunkLength == chunkShareStatus.incrementAndGet()) {
//		    System.out.println(fileName+"\t"+chunkShareStatus.get()+"/"+chunkLength);
			new Thread(()->{
				try {
					FileMonitor.ignoreFile(this.uuid);
					mergeFiles();
					Thread.sleep(Main.INTERVAL*3);
					FileMonitor.unignoreFile(this.uuid);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			delInstance(this.uuid);
	}
//	    System.out.println(fileName+"\t"+chunkShareStatus.get()+"/"+chunkLength);
	    byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "400", fileName, "success");
	    agent.send(address, message);
	}
