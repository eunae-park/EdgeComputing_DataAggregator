package kr.re.keti;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;

public class EdgeRequest implements Runnable {
	String ip;
	String partname;
	String incomming_name;
	String uuid;
	long length;
	int nodes;
	String file_name;
	String path;
	int partnum;
	

	
	
	public EdgeRequest(String ip, String uuid, String partname, String incomming_name, long length, int nodes) {
		// TODO Auto-generated constructor stub
		this.ip = ip;
		this.partname = partname;
		this.incomming_name = incomming_name;
		this.uuid = uuid;
		this.length = length;
		this.nodes = nodes;
		
		
		if (incomming_name.contains("/")) {
			String[] tfname = incomming_name.split("/");
			int tflen = tfname.length;
			file_name = tfname[tflen-1];
			//System.out.println("run first code ");
			//System.out.println("tfname : " + tfname[tflen-1]);
			//System.out.println("EdgeRequest init incomming_name : " + incomming_name);
			path = incomming_name.split(file_name)[0];
			//System.out.println("path : " + kpath);
			int partindex = partname.indexOf(".part");
			this.partnum = Integer.parseInt(partname.substring(partindex-2, partindex));
		}else {
			String[] tfname = incomming_name.split("\\\\");
			int tflen = tfname.length;
			file_name = tfname[tflen-1];
			//System.out.println("run second code : ");
			//System.out.println("tfname : " + tfname[tflen-1]);
			//System.out.println("EdgeRequest init incomming_name : " + incomming_name);
			path = incomming_name.split(file_name)[0];
			//System.out.println("path : " + kpath);
			int partindex = partname.indexOf(".part");
			this.partnum = Integer.parseInt(partname.substring(partindex-2, partindex));
		}
		
		
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Socket socket;
		try {
			socket = new Socket(ip, 8888);
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			FileOutputStream fos = new FileOutputStream(incomming_name);				
			os.write(partname.getBytes());
			os.flush();
			
			
			int DEFAULT_BUFFER_SIZE = 100000; 
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            long totalReadBytes = 0;
    		
            int progresscnt = partnum * 10; 
            while ((readBytes = is.read(buffer)) != -1) {
            	//System.out.println("readBytes : " + readBytes);
            	fos.write(buffer, 0, readBytes);
            	totalReadBytes += readBytes;
            	if(totalReadBytes >= (length / 10)){
            		ProgressWriter(path + uuid + ".meta", Integer.toString(progresscnt));
            		//fprogress = new PrintWriter(new FileWriter(foldername + "/" + uuid + ".txt", true));
            		//System.out.println("totalReadBytes : " + totalReadBytes);
            		totalReadBytes = 0;
            		//fprogress.println(progresscnt);
            		progresscnt++;
            		//fprogress.close();
            	}
            }      
            ProgressWriter(path + uuid + ".meta", Integer.toString(progresscnt));
//            fprogress = new PrintWriter(new FileWriter(foldername + "/" + uuid + ".txt", true));
//            fprogress.println(progresscnt);
//            System.out.println("totalReadBytes : " + totalReadBytes);
//            progresscnt++;
//            fprogress.close();

            os.close();
            is.close();
            fos.close();
			socket.close();
			
			System.out.println("incomming data Hash : " + sha(incomming_name));				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
	
	private static String sha(String filepath) throws Exception{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = new FileInputStream(filepath);
        
        byte[] dataBytes = new byte[1024];
     
        int nread = 0; 
        while ((nread = fis.read(dataBytes)) != -1) {
          md.update(dataBytes, 0, nread);
        };
        byte[] mdbytes = md.digest();
     
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
          sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
 
        //System.out.println("SHA-256 : " + sb.toString());
        fis.close();
        return sb.toString();
    }
	
	private synchronized void ProgressWriter(String filename, String message) {
		try {
			PrintWriter fprogress = new PrintWriter(new FileWriter(filename, true));
			fprogress.println(message);
			fprogress.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
