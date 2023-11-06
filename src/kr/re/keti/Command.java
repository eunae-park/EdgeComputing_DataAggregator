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
		}
	}
}
