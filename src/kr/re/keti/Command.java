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
	}
}
