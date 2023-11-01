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
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
