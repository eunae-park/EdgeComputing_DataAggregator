package kr.re.keti;

import java.util.Scanner;

import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;

public class Command extends Thread{
	Database database;
	RequestProcess requestProcess;
}
