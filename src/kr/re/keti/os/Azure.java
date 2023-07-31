package kr.re.keti.os;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Arrays;

public class Azure implements OSProcess{
	private final String mountPoint = "/media/azure/";
	private final String checkFile = "check.txt";
	private String mountPath;
	private TcpReceptor receptor;
	@Override
	public String getMaster() {
		receptor = new TcpReceptor();
		String masterIP = "none";
		mountPath = getSharedDiskPath();
		mount(mountPath);
		masterIP = getMasterAddress();
		if(!receptor.check(masterIP)) {
			checkFileCreate();
			masterIP = getMasterAddress();
		}
		umount();
		return masterIP;
	}
	
	@Override
	public void start() {
		receptor.start();
	}
	
	@Override
	public void stop() {
		receptor.stop();
		mount(mountPath);
		checkFileDelete();
		umount();
	}
	
	public boolean checkFileDelete() {
		boolean result = false;
		File file = new File(mountPoint+checkFile);
		if(file.exists()) {
			file.delete();
			result = true;
		}
	}
}
