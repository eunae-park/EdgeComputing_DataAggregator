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
		return result;
	}
	private String getMasterAddress() {
		String masterIP = "none";
		File checkPath = new File(mountPoint+checkFile);
		if(checkPath.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(checkPath));
				masterIP = reader.readLine();
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			masterIP = checkFileCreate();
		}
		return masterIP;
	}
	private String checkFileCreate() {
		String address = "none";
		File checkPath = new File(mountPoint+checkFile);
		try {
			address = InetAddress.getLocalHost().getHostAddress();
			BufferedWriter writer = new BufferedWriter(new FileWriter(checkPath));
			writer.write(address);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return address;
		
	}
	private boolean mount(String path) {
		File mountFolder = new File(mountPoint);
		if(!mountFolder.exists()) {
			mountFolder.mkdir();
		}
		
		try {
			Process process = Runtime.getRuntime().exec("sudo mount "+path+" "+mountPoint);
			process.waitFor();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	private boolean umount() {
		try {
			Process process = Runtime.getRuntime().exec("sudo umount "+mountPoint);
			process.waitFor();
			return true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	private String getSharedDiskPath() {
		String path = "none";
		double topSize = -1;
		try {
			Process process = new ProcessBuilder("lsblk", "--noheadings", "--output", "NAME,TYPE,SIZE").start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while((line = reader.readLine()) != null) {
				String[] parts = line.trim().split("\\s+");
				double size = convertToBytes(parts[2]);
				if(size > topSize) {
					path = parts[0];
					topSize = size;
				}
				
			}
			return "/dev/"+path;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return path;
	}
	private double convertToBytes(String size) {
		String[] units = {"B", "K", "M", "G", "T"};
		String unit = size.substring(size.length()-1);
		double value = Double.parseDouble(size.substring(0, size.length()-1));
	}
}
