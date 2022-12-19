package kr.re.keti;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import kr.re.keti.db.Database;

public class FileMonitor implements FileAlterationListener{
	private String master_ip;
	private String device_ip;
	private Mqtt client;
	public static DataProcess dataProcess;
	private Database database;
	private String dir;
//	private FileAlterationMonitor monitor;
	private int interva;
//	private ArrayList<String> dataList;
	
	public FileMonitor(Mqtt client, String master_ip, String device_ip, int interva, String dir) {
		database = new Database("file_management", "mecTrace", "penta&keti0415!");
		this.client = client;
		this.master_ip = master_ip;
		this.device_ip = device_ip;
		this.interva = interva;
		dir = dir.startsWith("/")? dir: "/"+dir;
		dir = dir.endsWith("/")? dir: dir+"/";
		this.dir = dir;
		
		if(master_ip.equals(device_ip)) {
			dataProcess = MasterWorker.dataprocess;
		}
		else {
			dataProcess = SlaveWorker.dataprocess;
		}
	}
	public FileAlterationMonitor work() {
		FileAlterationObserver observer = new FileAlterationObserver(dir);
		FileAlterationMonitor monitor = new FileAlterationMonitor(interva);
		observer.addListener(new FileMonitor(client, master_ip, device_ip, interva, dir));
		monitor.addObserver(observer);
		return monitor;
	}

	@Override
	public void onFileCreate(File file) {
		System.out.println("");
		System.out.println("<Monitor> creat file : "+file);
		String fileName = file+"";
		fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
		if(fileName.startsWith(".")) return;

		dataProcess.SettingPort();
		
		if(fileName.split("\\.")[0].equals("hello") || fileName.split("\\.")[0].equals("world")) {
			database.createFile(fileName, device_ip, 1);	
		}
		else {
			database.createFile(fileName, device_ip, 5);
		}

		if (master_ip.equals(device_ip)) {
			client.send(master_ip, "011", fileName.split("\\.")[0]);
		}
		else {
			dataProcess.RequestMqtt(master_ip, device_ip, "011", fileName.split("\\.")[0]);
		}
		
		tempFileCreate(fileName);
		sendFile(fileName.split("\\.")[0]);
		tempFileDelete(fileName);
	}
	
	@Override
	public void onFileChange(File file) {
		System.out.println("");
		System.out.println("<Monitor> FileChange:"+file);
		String fileName = file+"";
		fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
		if(fileName.startsWith(".")) return;
		
		dataProcess.SettingPort();
		if (master_ip.equals(device_ip)) {
			client.send(master_ip, "012", fileName);
		}
		else {
			dataProcess.RequestMqtt(master_ip, device_ip, "012", fileName);
		}

		tempFileCreate(fileName);
		sendFile(fileName.split("\\.")[0]);
		tempFileDelete(fileName);
	}


	@Override
	public void onFileDelete(File file) {
		System.out.println("");
		System.out.println("<Monitor> FileDelete:"+file);
		String fileName = file+"";
		fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
		if(fileName.startsWith(".")) return;
		
		dataProcess.SettingPort();
		
		String metaInfo = database.select(fileName.split("\\.")[0]);
		if(metaInfo.equals("none")) return;
		int security_level = Integer.parseInt(metaInfo.split("#")[4]);
		String linked_edge = metaInfo.split("#")[10];
		database.delete(fileName.split("\\.")[0]);
		
		if(security_level > 2 || linked_edge.equals(device_ip)) {
			if (master_ip.equals(device_ip)) {
				client.send(master_ip, "013", fileName.split("\\.")[0]);
			}
			else {
				dataProcess.RequestMqtt(master_ip, device_ip, "013", fileName.split("\\.")[0]);
			}			
		}
	}

	@Override
	public void onDirectoryCreate(File directory) {
		System.out.println("<Monitor> DirectoryCreate:"+directory);
		if (master_ip.equals(device_ip)) {
			client.send(master_ip, "014", directory+"");
		}
		else {
			dataProcess.RequestMessage(directory+"", (master_ip+","+device_ip), "014");
			
		}
		
	}
	@Override
	public void onDirectoryChange(File directory) {
		System.out.println("<Monitor> DirectoryChange:"+directory);
		if (master_ip.equals(device_ip)) {
			client.send(master_ip, "015", directory+"");
		}
		else {
			dataProcess.RequestMessage(directory+"", (master_ip+","+device_ip), "015");
			
		}
	}


	@Override
	public void onDirectoryDelete(File directory) {
		System.out.println("<Monitor> DirectoryDelete:"+directory);
		if (master_ip.equals(device_ip)) {
			client.send(master_ip, "016", directory+"");
		}
		else {
			dataProcess.RequestMessage(directory+"", (master_ip+","+device_ip), "016");
			
		}
		
	}


	@Override
	public void onStart(FileAlterationObserver arg0) {
//		System.out.println("Check Parse Start...");
		
	}

	@Override
	public void onStop(FileAlterationObserver arg0) {
//		System.out.println("Check Parse Stop...");
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getEdgeList(){
		ArrayList<String> edgeList = new ArrayList<>();
		if(master_ip.equals(device_ip)) {
			edgeList = (ArrayList<String>) MasterWorker.slaveList.clone();
		}
		else {
			edgeList = dataProcess.RequestSlaveList(master_ip);
			edgeList.add(master_ip);
			edgeList.remove(device_ip);
		}
		return edgeList;
	}
	
	
	private void sendFile(String filename) {
		dataProcess.SettingPort();
		ArrayList<String> dataList = new ArrayList<>();
		ArrayList<String> edgeList = getEdgeList();
		
		int check = -1;
		String meta_info = dataProcess.MetaDataInfomation(filename);
		if(meta_info.equals("none"))
		{
			System.out.println("* Don't have Data [" + filename + "]");
		}
		for(int i=0; i<edgeList.size(); i++){
			System.out.println("request to " + edgeList.get(i));
			check = dataProcess.IndividualDataTransfer(filename, edgeList.get(i), meta_info);
			if(check == 2)
				dataList.add(edgeList.get(i));
			else if(check == -1)
				System.out.println("* Bring the Information of Edge Device(" + edgeList.get(i) + ") : Failure.");
		}
		
		if(dataList.size() == edgeList.size())
			System.out.println("* [ All Edges ] : Transfermission is Success.");
		else if(dataList.size() != 0)
			System.out.println("* " + dataList + " : Transfermission is Success.");
		else
			System.out.println("* Cannot Transfer Data to Anyone.");
	}
	private boolean tempFileCreate(String fileName) {
		String directory = dir.substring(0, indexOf(dir, '/', 3))+"/temp";
		File folder = new File(directory);
		if(!folder.exists()) {
			folder.mkdir();
		}
		
		byte[] data = (device_ip).getBytes();
		try {
			File file = new File(directory+"/"+fileName);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	private boolean tempFileDelete(String fileName) {
		String directory = dir.substring(0, indexOf(dir, '/', 3))+"/temp";
		File file = new File(directory+"/"+fileName);
		if(file.exists()) {
			file.delete();
		}
		File folder = new File(directory);
		if(folder.exists()) {
			folder.delete();
			return true;
		}
		return false;
	}
	public int indexOf(String str, int index) {
		return indexOf(str, '/', index);
	}
	public int indexOf(String str, char c, int index) {
		int cnt = 0;
		for(int i=0 ;i<str.length();i++) {
			if(str.charAt(i) == c) {
				if(++cnt == index) {
					return i;
				}
			}
		}
		return -1;
	}

	

}
