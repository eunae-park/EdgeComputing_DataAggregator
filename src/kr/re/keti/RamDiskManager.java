package kr.re.keti;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class RamDiskManager {
	public static RamDiskManager instance = null;
    private String ramDiskPath;
    private String diskPath;
    private long maxRamDiskSize;
    private Map<String, Long> fileSizes;

    public static RamDiskManager getInstance() {
    	if(instance == null) {
    		System.out.println("RamDiskManager is null");
    	}
    	return instance;
    }
    public static RamDiskManager getInstance(String ramDiskPath, String diskPath, long maxRamDiskSize) {
    	if(instance == null) {
    		instance = new RamDiskManager(ramDiskPath, diskPath, maxRamDiskSize);
    	}
    	return instance;
    }
    private RamDiskManager(String ramDiskPath, String diskPath, long maxRamDiskSize) {
        this.ramDiskPath = ramDiskPath;
        this.diskPath = diskPath;
        this.maxRamDiskSize = maxRamDiskSize;
        this.fileSizes = new HashMap<>();
    }

    public void createFile(String fileName, byte[] data) {
    	FileMonitor.ignoreFile(fileName);
        // Check if there is enough space in the ramdisk
        long totalSize = 0;
        for (long size : fileSizes.values()) {
            totalSize += size;
        }
        if (totalSize + data.length > maxRamDiskSize) {
            // Not enough space, delete the oldest file
            String oldestFile = null;
            long oldestTime = Long.MAX_VALUE;
            for (Map.Entry<String, Long> entry : fileSizes.entrySet()) {
                File file = new File(ramDiskPath + File.separator + entry.getKey());
                if (file.lastModified() < oldestTime) {
                    oldestTime = file.lastModified();
                    oldestFile = entry.getKey();
                }
            }
            if (oldestFile != null) {
                deleteRamFile(oldestFile);
            }
        }

        // Create the file in the ramdisk
        File file = new File(ramDiskPath + File.separator + fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fileSizes.put(fileName, (long) data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Copy the file to the disk
       	copyFileToDisk(fileName);
    	try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
       	FileMonitor.unignoreFile(fileName);
    }

    public void deleteFile(String fileName) {
    	FileMonitor.ignoreFile(fileName);
        File file = new File(diskPath + File.separator + fileName);
        if (file.exists()) {
            file.delete();
        }
        deleteRamFile(fileName);
        try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        FileMonitor.unignoreFile(fileName);
    }
    public void deleteRamFile(String fileName) {
        File file = new File(ramDiskPath + File.separator + fileName);
        if (file.exists()) {
            file.delete();
            fileSizes.remove(fileName);
        }
    }

    private void copyFileToDisk(String fileName) {
        // Copy the file from the ramdisk to the disk
        File srcFile = new File(ramDiskPath + File.separator + fileName);
        File destFile = new File(diskPath + File.separator + fileName);
        try {
            Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
