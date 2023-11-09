package kr.re.keti;

import java.util.Scanner;

import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;

public class Command extends Thread{
	Database database;
	RequestProcess requestProcess;
	Scanner scanner;
	
	public Command(Database database) {
		requestProcess = new RequestProcess(database);
		this.database = database;
		this.scanner = new Scanner(System.in);
	}
	@Override
	public void run() {
		String command;
		String type;
		
		while(!isInterrupted()) {
			try {
				System.out.println();
				System.out.print(">>>");
				command = scanner.nextLine();
				type = command.split(" ")[0];
				
				if(command.equals("exit")) break;
				
				
				if(type.matches("[0-9]+")) {
					switch(Integer.parseInt(type)) {
					case 1: deviceInformation(command); break;
					case 2: wholeDataInformation(command); break;
					case 3: individualMetaDataInformation(command);break;
					case 4: individualDataRead(command); break;
					case 5: requestProcess.newEdge("[10.0.0.126]");
					}
					continue;
				}
				switch(type) {
					case "update": updateFile(); break;
					case "help": help(); break;
					case "deviceIP": System.out.println("device-IP : "+Main.deviceIP); break;
					case "masterIP": System.out.println("master-IP : "+Main.masterIP); break;
					case "mode" : System.out.println("device mode : "+Main.mode); break;
					case "edgeList": edgeList(); break;
					case "deviceInformation": deviceInformation(command); break;
					case "wholeDataInformation": wholeDataInformation(command); break;
					case "individualMetaDataInformation": individualMetaDataInformation(command);break;
					case "distributed": distributed(command); break;
				}
				
//				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(scanner != null) scanner.close();
	}
	private void updateFile() {
		FileManagementDto dto = null;
		String dataId = "";
		while(true) {
			System.out.println("File Name : ");
			dataId = scanner.nextLine();
			if(dataId.equals("exit")) return;
			else {
				if(database.select(dataId) != null) {
					break;
				}
				else {
					System.out.println("not found data ["+dataId+"]");
				}
			}
		}
		
		while(true) {
			System.out.println("Security Level(1 - 5) : ");
			try {
				int level = Integer.parseInt(scanner.nextLine());
				if(level == -1) break;
				if(level<0 || level>5) throw new Exception();
				
				dto.setSecurityLevel(level);
				database.update(dto);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
	}
	private void help() {
		System.out.println("keyword : ");
		System.out.println("\thelp, deviceIP, masterIP, mode, edgeList");
		System.out.println("\t1. deviceInformation [ip]");
		System.out.println("\t2. wholeDataInformation [ip]");
		System.out.println("\t3. individualMetaDataInformation [dataid]");
	}
	private void edgeList() {
		String slaveList = requestProcess.requestEdgeList(PortNum.KETI_PORT);
		System.out.print("Edge List : "+Main.masterIP+"(master)\t["+slaveList+"]\n");			
	}
	private void deviceInformation(String command) {
		String[] commands = command.split(" ");
		if(commands.length == 1 || commands[1].equals(Main.deviceIP)) {
			System.out.println("request to mine");
			String data = requestProcess.deviceInformation();
			requestProcess.showDeviceInformation(data);
		}
		else {
			requestProcess.requestDeviceInformation(commands[1], PortNum.KETI_PORT);
		}
	}
	private void wholeDataInformation(String command) {
		String[] commands = command.split(" ");
		if(commands.length == 1 || commands[1].equals(Main.deviceIP)) {
			System.out.println("request to mine");
			String data = requestProcess.wholeDataInformation();
			requestProcess.wholeDataInformation(data);
		}
		else {
			System.out.println("request to "+commands[1]);
			requestProcess.requestWholeDataInformation(commands[1], PortNum.KETI_PORT);;
		}
	}
	private void individualMetaDataInformation(String command) {
	}
}
