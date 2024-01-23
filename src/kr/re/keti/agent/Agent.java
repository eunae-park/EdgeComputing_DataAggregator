package kr.re.keti.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import kr.re.keti.DataProcess;
import kr.re.keti.FileHandler;
import kr.re.keti.Main;
import kr.re.keti.PortNum;
import kr.re.keti.RamDiskManager;
import kr.re.keti.ResponseProcess;
import kr.re.keti.Ssl;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.database.FileUuidDto;
import kr.re.keti.os.OSProcess;
import kr.re.keti.tcp.UnitEdge;
import kr.re.keti.tcp.UnitShared;

public class Agent extends EdgeDataAggregator{
	private static final Agent instance = new Agent();
	public Database database;
	public static Hashtable<String, AgentPacket> unitTable;
	private ResponseProcess responseProcess;
	private List<Integer> logFilters;
	private List<Integer> responseFilters;
	private RamDiskManager ramDiskManager;
	private Agent() {
		logFilters = Arrays.asList(-1, 10, 200, 300, 399, 400, 405, 406, 444, 19, 20);
		responseFilters = Arrays.asList();
		setStandard(1000);
	}
	
	public static Agent getInstance() {
		return instance;
	}
	public void setDatabase(Database database) {
		instance.database = database;
		responseProcess = new ResponseProcess(database);
		ramDiskManager = RamDiskManager.getInstance(Main.ramFolder, Main.storageFolder);
	}
	@Override
	void receive(AgentPacket packet) {
	}
}
