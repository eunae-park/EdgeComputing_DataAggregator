package kr.re.keti;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import kr.re.keti.agent.Agent;
import kr.re.keti.database.Database;
import kr.re.keti.database.FileManagementDto;
import kr.re.keti.database.FileUuidDto;

public class FileMonitor{
	private Agent agent;
	private String dir;
	private Database database;
//	private DataProcess dataProcess;
	private static Set<String> ignoredFiles = new HashSet<String>();;
	private FileAlterationMonitor monitor;
	private Ssl ssl;
	
	public FileMonitor(String dir, Agent agent, Database database, long interval) {
		this.dir = dir;
		this.agent = agent;
		this.database = database;
		this.monitor = new FileAlterationMonitor(interval);
		ssl = Ssl.getInstance();
	}
	public void start() {
		File directory = new File(dir);
		FileAlterationObserver observer = new FileAlterationObserver(directory);
		FileAlterationListener listener = new FileAlterationListener() {
			@Override
			public void onStop(FileAlterationObserver arg0) {
			}
			@Override
			public void onStart(FileAlterationObserver arg0) {
			}
			@Override
			public void onFileCreate(File file) {
				System.out.println("File create : "+file);
				String fileName = file.getName();
				if(fileName.endsWith(".swp")) return;
				if (ignoredFiles.contains(fileName)) return;
				
				int dotIndex = fileName.lastIndexOf(".");
				String dataid = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
				
				String sign = ssl.sign(file.getPath());				
				String uuid = UUID.randomUUID().toString();
				uuid = uuid.replace("-", "");
				FileManagementDto dto = new FileManagementDto(file, sign, uuid);
				
				FileUuidDto uuidDto = new FileUuidDto(dataid, uuid);
				dto = new FileManagementDto(file, uuid, sign);
				dto.setLinkedEdge(Main.deviceIP);
				database.insert(uuidDto);
				database.insert(dto);
				
				String metaData = dto.toString();
				byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "010", metaData);
				agent.send(message);
				
				File destFile = new File(Main.ramFolder + File.separator + fileName);
				try {
					Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				byte[] data = readFile(file);
				String dataSize = data.length+"";
				message = DataProcess.messageCreate(data, "REQ", Main.deviceIP, "011", uuid, dataSize);
				agent.send(message);
				
			}
			@Override
			public void onFileChange(File file) {
				System.out.println("File change : "+file);
				String fileName = file.getName();
				if(fileName.endsWith(".swp")) return;
				if (ignoredFiles.contains(fileName)) return;

				int dotIndex = fileName.lastIndexOf(".");
				String dataid = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
				String sign = ssl.sign(fileName);
				
				FileManagementDto dto = new FileManagementDto(file, dataid, sign);
				database.update(dto);
				byte[] data = readFile(file);
				String dataSize = data.length+"";
				byte[] message = DataProcess.messageCreate(data, "REQ", Main.deviceIP, "012", dataid, dataSize);
				agent.send(message);
			}
			@Override
			public void onFileDelete(File file) {
				System.out.println("File delete : "+file);
				String fileName = file.getName();
				if(fileName.endsWith(".swp")) return;
				if (ignoredFiles.contains(fileName)) return;

				int dotIndex = fileName.lastIndexOf(".");
				String dataid = (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;

				byte[] message = DataProcess.messageCreate("REQ", Main.deviceIP, "013", dataid);
				
				database.delete("file_uuid", fileName);
				database.delete("file_management", dataid);
				RamDiskManager.getInstance().deleteRamFile(fileName);
				agent.send(message);					
			}
			@Override
			public void onDirectoryCreate(File directory) {
			}
			@Override
			public void onDirectoryChange(File directory) {
			}
			@Override
			public void onDirectoryDelete(File directory) {
			}
		};
		
		observer.addListener(listener);
		monitor.addObserver(observer);
		try {
			monitor.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void stop() {
		try {
			monitor.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void ignoreFile(String fileName) {
        ignoredFiles.add(fileName);
    }

    public static void unignoreFile(String fileName) {
        ignoredFiles.remove(fileName);
    }
    public static Set<String> getIgnoreFile() {
    	return ignoredFiles;
    }
    
	
	private byte[] readFile(File file) {
	    byte[] data = null;
	    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            data = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
		return data;
	}
}
