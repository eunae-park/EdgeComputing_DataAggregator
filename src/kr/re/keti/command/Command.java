package kr.re.keti.command;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import kr.re.keti.DataProcess;
import kr.re.keti.EdgeDataAggregator;
import kr.re.keti.db.Database;

public class Command implements Runnable{
	private boolean stop;
	private String currDirectory;
	private String mode;
	public String uuid;
	public String masterIp;
	public String currIp;
	public Database database;
	public kr.re.keti.Database databaseLib;
	public String ramFolder;
	public String storageFolder;
	public String certFolder;
	public DataProcess dataProcess;
	public String tableName;
	public String dbId;
	public String dbPw;
	public ArrayList<String> slaveList;
	
	public Command(String mode, String masterIp, String currIp, kr.re.keti.Database databaseLib,
			String tableName, String dbId, String dbPw) 
	{
		this.mode = mode;
		this.uuid = EdgeDataAggregator.dev_uuid;
		this.masterIp = masterIp;
		this.currIp = currIp;
		this.databaseLib = databaseLib;
		this.ramFolder = EdgeDataAggregator.data_folder;
		this.storageFolder = EdgeDataAggregator.storage_folder;
		this.certFolder = EdgeDataAggregator.cert_folder;
		this.tableName = tableName;
		this.dbId = dbId;
		this.dbPw = dbPw;
		this.stop = false;
		this.currDirectory = storageFolder;
	}
	@Override
	public void run() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scanner scanner = new Scanner(System.in);
		dataProcess = new DataProcess(ramFolder, certFolder, storageFolder, databaseLib, currIp, tableName, uuid);
		database = new Database(tableName, dbId, dbPw);
		
		int check = -1, ip_number=0, i;
		
		while(!stop) {
			System.out.print(currDirectory+":");
			String command = scanner.nextLine();
			String[] commands = command.split(" ");
			
			try {
				switch (commands[0]) {
				case "exit": stop = true; System.exit(0);
				case "help": help(); break;
				case "cd": currDirectory = cd(commands); break;
				case "ls": ls(commands); break;
				case "file": file(commands); break;
				case "cat": cat(commands); break;
				case "pwd": pwd(); break;
				case "uuid": uuid(); break;
				case "mode": mode(); break;
				case "edge-list": edgelist(); break;
				case "master-data": masterdata(); break;
				default:
					System.out.println("Command '"+commands[0]+"' not found");
				}
				
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void slaveSetting(ArrayList<String> slist) {
		slaveList = (ArrayList<String>) slist.clone();
		String remote_cmd = currIp;
		for (String slave : slaveList) {
			remote_cmd += ":" + slave;
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		dataProcess.SendEdgeList(slist, remote_cmd);
	}
	public void help() {
		System.out.println("exit");
		System.out.println("cd [path]");
		System.out.println("ls [path]");
		System.out.println("file [filename]");
		System.out.println("cat [filename]");
		System.out.println("pwd");
		System.out.println("uuid");
		System.out.println("mode");
		System.out.println("edge-list");
		System.out.println("master-data");
		System.out.println("device-information");
	}
	public String cd(String[] commands) {
		String path = currDirectory;
		if (commands.length == 1) {
		}
		else if(commands.length == 2){
			File absolute = new File(commands[1]);
			if(absolute.isAbsolute()) {
				path = absolute.getPath();
				path = path.endsWith("/")? path: path+"/";
			}
			else{
				String[] dirs = commands[1].split("/");
				for(String dir: dirs) {
					if(dir.equals("..")) {
						String parent = new File(path).getParent();
						path = parent == null? path: parent;
						path = path.endsWith("/")? path: path+"/";
					}
					else {
						path = path+dir+"/";
					}
				}
			}
			
			
		}
		else {
			System.out.println("cd: too many arguments");
		}
		return path;
	}
	public void ls(String[] commands) {
		File dir = null;
		if(commands.length == 1) {
			dir = new File(currDirectory);
		}
		else if(commands.length == 2){
			String path = cd(commands);
			dir = new File(path);
		}
		else {
			System.out.println("ls: too many arguments");
			return;
		}
		File files[] = dir.listFiles();
		
		for(File file : files) {
			System.out.println(file.getName());
		}
	}
	public void edgelist() {
		ArrayList<String> edgeList = null;
		if(mode.equals("slave")) {
			edgeList = dataProcess.RequestSlaveList(masterIp);	
		}
		else edgeList = slaveList;
		System.out.println("Edge List : " + masterIp + "(master)\t" +  edgeList);
	}
	public void pwd() {System.out.println(currDirectory);}
	public void uuid() {System.out.println(uuid);}
	public void mode() {
		if(mode.equals("master")) {
			System.out.println("mode: "+mode+"	ip:"+masterIp);			
		}
		else {
			System.out.println("mode: "+mode+"	masterIP:"+masterIp);
		}
	}
	public void file(String[] commands) {
		if(commands.length == 1) {
			dataProcess.WholeDataInfo();
		}
		else if(commands.length == 2) {
			String result = dataProcess.MetaDataInfomation(commands[1]);
			if(!result.equals("none")) {
				String[] data = result.split("#");
				System.out.println("\tDataId: "+data[0]);
				System.out.println("\tTimeStamp: "+data[1]);
				System.out.println("\tFileType: "+data[2]);
				System.out.println("\tDataType: "+data[3]);
				System.out.println("\tSecurityLevel: "+data[4]);
				System.out.println("\tDataPriority: "+data[5]);
				System.out.println("\tAvailabilityPolicy: "+data[6]);
				System.out.println("\tDataSignature: "+data[7]);
				System.out.println("\tCert: "+data[8]);
				System.out.println("\tDirectory: "+data[9]);
				System.out.println("\tLinked_edge: "+data[10]);
				System.out.println("\tDataSize: "+data[11]);
			}
			else {
				System.out.println("file: '"+commands[1]+"' not found");
			}
			
		}
		else {
			System.out.println("file: too many arguments");
		}
	}
	public void cat(String[] commands) {
		if(commands.length == 1) {
			System.out.println("cat: need filename");
		}
		else if(commands.length == 2) {
			if(database.existence(commands[1])) {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(commands[1]));
					String str;
					System.out.println("DATA:[[");
					while((str = reader.readLine())!= null) {
						System.out.println("\t"+str);
					}
					System.out.println("]]DATA");
					reader.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else {
				System.out.println("cat: '"+commands[1]+"' not found");
			}
		}
		else {
			System.out.println("cat: too many arguments");
		}
	}
	public void masterdata() {
		int check = dataProcess.WholeDataInfo(masterIp);
		if(check == -1) System.out.println("* Bring the Information of Edge Device(" + masterIp + ") : Failure.");
	}
	public void deviceInformation(String[] commands) {
		if(commands.length == 1) {
			dataProcess.DeviceInfomation();
		}
		else if(commands.length == 2) {
			int check = dataProcess.DeviceInfomation(commands[1]);
			if(check == -1) {
				System.out.println("* Bring the Information of Edge Device(" + commands[1] + ") : Failure.");
			}
		}
		else {
			System.out.println("device-information: too many arguments");
		}
	}
	
	
}
