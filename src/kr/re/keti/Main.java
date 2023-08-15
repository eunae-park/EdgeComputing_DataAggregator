package kr.re.keti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import kr.re.keti.agent.Agent;
import kr.re.keti.database.Database;
import kr.re.keti.database.MysqlDao;
import kr.re.keti.database.SqliteDao;
import kr.re.keti.os.Azure;
import kr.re.keti.os.EdgeFinder;
import kr.re.keti.os.Linux;
import kr.re.keti.os.OSProcess;

public class Main {
	public static String uuid;
	public static String deviceIP;
	public static String masterIP = "None";
	public static String storageFolder;
	public static String certFolder;
	public static String ramFolder;
	public static String mode;
	public static String programStartTime = programStartTime();
	public static OSProcess process;
	public static void main(String[] args){
		// -----------------IP in args---------------------------------------------
		if(args.length >0 ) {
			try {
				InetAddress addr = InetAddress.getByName(args[args.length - 1]);
				deviceIP = addr.getHostAddress();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Database database = null;
		
		//----------------------file read--------------------------
		try {
			FileReader file = new FileReader("info_device.txt");
			BufferedReader br = new BufferedReader(file);
		
			database = EdgeInformation(br);
		
			if(br!=null) br.close();
			if(file!=null) file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//----------------OS-----------------------------------
		String osName = System.getProperty("os.name");
		if(osName.equals("Linux")) {
			String osVersion = System.getProperty("os.version");
			if(osVersion.indexOf("azure") != -1) {
				process = new Azure();
			}
			else {
				process = new Linux();
			}
		}
		else if(osName.equals("Windows")) {
			System.out.println("\t**System is Windows**");
		}

		Ssl.selfSignedCertificate(certFolder, certFolder+"Private/", certFolder+"Private/private.key", 365);
		//--------------------Master Find---------------------------------
		masterIP = process.getMaster();
		if(masterIP.equals("none") || masterIP.equals(deviceIP)) {
			mode = "master";
			masterIP = deviceIP;
			process.start();
		}
		else {
			mode = "slave";
		}
		
		
		DataProcess dataProcess = new DataProcess(database);
		dataProcess.initWholeDataInformation();
		

		//------------------------master found----------------------------------
		Agent agent = Agent.getInstance();
		agent.setDatabase(database);
		agent.start();
		

		if(mode.equals("master")) {
			System.out.println("Waiting for connections from slaves...");
		}
		else {
			System.out.println("* Master found: " + masterIP + "\n");
			agent.send(("{[{REQ::"+deviceIP+"::001::EDGE_LIST}]}").getBytes());
			
			try {
				byte[] keyData = Files.readAllBytes(Path.of(certFolder+"Private/pub.key"));
				int keySize = keyData.length;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
