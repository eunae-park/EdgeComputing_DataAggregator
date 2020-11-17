package kr.re.keti;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class DataProcess
{
	public DataProcess()
	{
    	try {
			Class.forName("com.mysql.cj.jdbc.Driver"); //com.mysql.jdbc.Driver
        	conn = DriverManager.getConnection(url, "file_user", "edgecomputing"); // DB_URL, USER_NAME, PASSWARD
        	state = conn.createStatement();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int fileIs(String filename, Socket socket) // real exists in node
	{
		try {
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			remote_cmd = "exist#" + filename ;
//			os.write( "exist#".getBytes() ); // file list
//			os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();
			
			byte[] data =new byte[100];
			int len = is.read(data);
			if(len >= 0)
			{
				String resultFromServer =new String(data,0,len);
				if(resultFromServer.equals("no"))
				{
//			        System.out.println("\tfile_list need to update.");
			        return -1;				
				}
			}			

//			System.out.println(resultFromServer);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}
	
	public int fileExist(String filename) // real exists in node
	{
		File file = new File(filename);
		if(!file.exists())
		{
//	        System.out.println("\tfile_list need to update.");
	       return -1;
		}
		System.out.println("\tlocal : file exist");
		return 1;
	}
	public int fileExist(String filename, String ipAddress)
	{
		int devcnt = -1;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			// because of receive role - socket.isConnected() always true;
			//boolean connected = socket.isConnected() && ! socket.isClosed(); 
//			while(socket.isClosed())	 {				}

			if(fileIs(filename, socket) == 1)
			{
				devcnt = 1;
//				System.out.println("\t" + ipAddress + " : file exist");
				Thread.sleep(5);
				
				InputStream is = socket.getInputStream();
				byte[] data =new byte[100];
				int len = is.read(data);
				String msgs ="";
				if(len >= 0)
				{
					msgs =new String(data,0,len);
				}

				String msg[] = msgs.split("#");
//				System.out.print("\t -> " + msg[2] + ":"  + msg[3] + ":"  + msg[4] + ":" + msg[5]);
				if(msg[1].equals("DATA") && msg[msg.length-1].equals("DATA") && msg[4].equals("0")) // 원본 파일이 있는 주소를 db에 저장
				{
					System.out.printf("\t" + ipAddress);
//					String msg = "#DATA#" + uuid + "#" + security + "#" + sharing + "#" + location + "#DATA#";
					uuid = msg[2];
					security = Integer.parseInt(msg[3]);
					sharing = 1; //Integer.parseInt(msg[4]) = if ipAddress 
					location = ipAddress + ":/" + msg[5];
					
					String select_sql = "SELECT * FROM file_management where uuid='" + uuid +"'";
		        	rs = state.executeQuery(select_sql); //SQL문을 전달하여 실행
		        	if(rs.next())
		        	{
		                System.out.println("\t - Original DATA DB_info already is existed.");
		        	}
		        	else
		        	{
			        	String insert_sql = "insert into file_management (file_name, uuid, security, sharing, location) values(?, ?, ?, ?, ?);";
						pstate = conn.prepareStatement(insert_sql);
			        	pstate.setString(1, filename);
			        	pstate.setString(2, uuid);
			        	pstate.setInt(3, security);
			        	pstate.setInt(4, sharing);
			        	pstate.setString(5, location);
			        	// INSERT는 반환되는 데이터들이 없으므로 ResultSet 객체가 필요 없고, 바로 pstmt.executeUpdate()메서드를 호출
			        	// INSERT, UPDATE, DELETE 쿼리는 같은 메서드를 호출
			        	// @return     int - 몇 개의 row가 영향을 미쳤는지를 반환
			        	int count = pstate.executeUpdate();
			        	if( count == 0 ){
			                System.out.println("\t - Original DATA DB_info Received : failure");
			            }
			        	else{
			                System.out.println("\t - Original DATA DB_info Received : success");
			            }
		        	}
				}
//				else{
//	                System.out.println("\t - Original DATA DB_info Received : failure");
//	            }
			}

			OutputStream os = socket.getOutputStream();
			os.write(finish_cmd.getBytes() ); // file list
			os.flush();
			socket.close();
			while(!socket.isClosed()) {			}// socket close wait - success
				
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return devcnt;	
	}
	public int fileExist(String filename, ArrayList<String> ipList)
	{
		int devcnt=0, check=-1;
		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);
//				while(socket.isClosed())	 {				}
	
				if(fileIs(filename, socket) == 1)
				{
					check += 1;
					System.out.println("\t" + ipList.get(devcnt) + " : file exist");
				}
				OutputStream os = socket.getOutputStream();
				os.write(finish_cmd.getBytes() ); // file list
				os.flush();
				socket.close();
				
//				if(check == 1)
//					break;
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		if(devcnt == ipList.size())
//			devcnt = -1;
//		return devcnt;
		return check;
	}		
	
	public int fileOpen(String filename, String ipAddress) // not yet v0818
	{
		int devcnt = 0;
		try {
			Socket socket = new Socket(ipAddress, 8888);

			if(fileIs(filename, socket) == -1)
			        devcnt = -1;
				
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			remote_cmd = "open#" + filename;
//			os.write( "open#".getBytes() ); // file list
//			os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();

			byte[] data =new byte[100];
			int len = is.read(data);
			if(len >= 0)
			{
				String resultFromServer =new String(data,0,len);
				if(resultFromServer.equals("no"))
				{
			        System.out.println("\tfile_list need to update.");
			        return -2;				
				}
			}			

			os.write(finish_cmd.getBytes() ); // file list
			os.flush();

			os.close();
			is.close();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return devcnt;	
	}

	public int fileClose(String filename, String ipAddress) {

		return 0;
	}
	
/*
	public int fileWrite(String filename) 
	{
		int devcnt=-1, i;
		FileWriter fw;
		Scanner sc = new Scanner(System.in);
		for(i=0; i<pathcnt && devcnt==-1; i++)
			devcnt = fileExist(i, filename);

//		System.out.print(devcnt);
		if (devcnt == -1)
		{
			// file list add
			try {
				fw = new FileWriter(path_list[0]+"0node_filelist.txt", true); // local always
				fw.write(filename+"\n");
				file_list[0].add(filename);
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// file contents add
			try {
				fw = new FileWriter(path_list[0]+filename, true);
				while (true)
				{
					System.out.print("\tcontents input(declare an end = :wq )\t ? ");
					String contents = sc.nextLine();
					if(contents.equals(":wq"))
						break;				
					fw.write(contents+"\n");
				}
				fw.close();
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			}
		
		}

		devcnt = i-1;
		if (devcnt == 0)
		{
			fileIs(devcnt, filename);
			// file contents add
			try {
				fw = new FileWriter(path_list[devcnt]+filename, true);
				while (true)
				{
					System.out.print("\tcontents input(declare an end = :wq )\t ? ");
					String contents = sc.nextLine();
					if(contents.equals(":wq"))
						break;				
					fw.write(contents+"\n");
				}
				fw.close();
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			}
		
		}

		else if(devcnt > 0)
		{			
			try {
				Socket socket = new Socket(path_list[devcnt], 8888);
				fileIs(devcnt, filename, socket);
//				Thread.sleep(500);
				OutputStream os = socket.getOutputStream();
//				InputStream is = socket.getInputStream();

				os.write( "write#".getBytes() ); // file list
				os.write( filename.getBytes() ); // file list

				while (true)
				{
					System.out.print("\tcontents input(declare an end = :wq )\t ? ");
					String contents = sc.nextLine();
					os.write(contents.getBytes());
//					os.flush();
					if(contents.equals(":wq"))
						break;				
				}

//				System.out.print("test");
//				Thread.sleep(500);
//				os.flush();
//				os.write("\n".getBytes());
//				os.write(finish_cmd.getBytes());
//				os.flush();


				socket.close();
			} catch (UnknownHostException e) {				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (FileNotFoundException e) {				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			} catch (IOException e) {				// TODO Auto-generated catch block
				//e.printStackTrace();
				return -1;
			} 
		}
		
		return 0;
	}
*/	
	public int fileWrite(String filename) 
	{
		if(fileExist(filename) == -1)
			return -1;
		
		FileWriter fw;
		Scanner sc = new Scanner(System.in);
		try {
			fw = new FileWriter(filename, true);
			while (true)
			{
				System.out.print("\tcontents input(declare an end = :wq )\t ? ");
				String contents = sc.nextLine();
				if(contents.equals(":wq"))
					break;				
				fw.write(contents+"\n");
			}
			fw.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	public int fileWrite(String filename, int check) 
	{
		FileWriter fw;
		Scanner sc = new Scanner(System.in);
		try {
			fw = new FileWriter(filename, true);
			while (true)
			{
				System.out.print("\tcontents input(declare an end = :wq )\t ? ");
				String contents = sc.nextLine();
				if(contents.equals(":wq"))
					break;				
				fw.write(contents+"\n");
			}
			fw.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		
		return 0;
	}
	public int fileWrite(String filename, String ipAddress) 
	{
		FileWriter fw;
		Scanner sc = new Scanner(System.in);
		int check = 0;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			check = fileIs(filename, socket); 
			if(check == -1)
				return -1;
//			Thread.sleep(500);
			OutputStream os = socket.getOutputStream();
//			InputStream is = socket.getInputStream();

			remote_cmd = "write#" + filename;
//			os.write( "write#".getBytes() ); // file list
//			os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();

			while (true)
			{
				System.out.print("\tcontents input(declare an end = :wq )\t ? ");
				String contents = sc.nextLine();
				os.write(contents.getBytes());
//				os.flush();
				if(contents.equals(":wq"))
					break;				
			}
			
			socket.close();
		} catch (UnknownHostException e) {				// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {				// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} catch (IOException e) {				// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} 
		
		return 0;
	}
	public int fileWrite(String filename, ArrayList<String> ipList) 
	{
		FileWriter fw;
		Scanner sc = new Scanner(System.in);
		int devcnt=0, check=-1;

		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);

				check = fileIs(filename, socket); 
				if(check == -1)
				{
					OutputStream os = socket.getOutputStream();
					os.write(finish_cmd.getBytes() ); // file list
					os.flush();
					socket.close();
					
					continue;
				}
				
//				Thread.sleep(500);
				OutputStream os = socket.getOutputStream();
//				InputStream is = socket.getInputStream();

				remote_cmd = "write#" + filename;
//				os.write( "write#".getBytes() ); // file list
//				os.write( filename.getBytes() ); // file list
				os.write(remote_cmd.getBytes());
				os.flush();

				while (true)
				{
					System.out.print("\tcontents input(declare an end = :wq )\t ? ");
					String contents = sc.nextLine();
					os.write(contents.getBytes());
//					os.flush();
					if(contents.equals(":wq"))
						break;				
				}
				
				socket.close();

				if(check == 1)
					break;
			}
		} catch (UnknownHostException e) {				// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {				// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} catch (IOException e) {				// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} 
		
		if(devcnt == ipList.size())
			devcnt = -1;		
		return devcnt;
	}

	public int fileRead(String filename) 
	{
		if (fileExist(filename) == -1)
			return -1;

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filename));
			System.out.println("\t[");
			while(true)
			{
	    	   String line = br.readLine();
	    	   if (line==null) break;
	    	   System.out.println("\t\t" + line);
	        }
			System.out.println("\t]");
			br.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 1;
	}
	public int fileRead(String filename, String ipAddress) // exist and read direct.
	{
		int devcnt=1, i;

		try {
			Socket socket = new Socket(ipAddress, 8888);

			if(fileIs(filename, socket) == -1)
				return -1;

			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			remote_cmd = "read#" + filename;
//				os.write( "read#".getBytes() ); // file list
//				os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();

			System.out.println("\t[");
			String resultFromServer = "";
			byte[] data = new byte[100];
			while (true)
			{
				int len = is.read(data);
				if(len < 0)
					break;
				resultFromServer = new String(data,0,len);
				String[] read_text = resultFromServer.split("\n");
				for(i=0; i<read_text.length; i++)
				{
					System.out.println("\t\t" + read_text[i]);
				}
			}
			System.out.println("\t]");

			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		
		return devcnt;
	}
	public int fileRead(String filename, ArrayList<String> ipList) // node_list that has a file
	{
		int check = 0;
		//check == 1 : success // else : false
		
		return check ;
	}
/* version #2
	public int fileRead(String filename, ArrayList<String> ipList) 
	{
		int devcnt=0, i, check=0;

		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt ++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);
	
				check = fileIs(filename, socket);
				if(check == -1)
				{
					OutputStream os = socket.getOutputStream();
					os.write(finish_cmd.getBytes() ); // file list
					os.flush();
					socket.close();
					
					continue;
				}
	
				OutputStream os = socket.getOutputStream();
				InputStream is = socket.getInputStream();
				
				remote_cmd = "read#" + filename;
	//				os.write( "read#".getBytes() ); // file list
	//				os.write( filename.getBytes() ); // file list
				os.write(remote_cmd.getBytes());
				os.flush();
	
				System.out.println("\t[");
				String resultFromServer = "";
				byte[] data = new byte[100];
				while (true)
				{
					int len = is.read(data);
					if(len < 0)
						break;
					resultFromServer = new String(data,0,len);
					String[] read_text = resultFromServer.split("\n");
					for(i=0; i<read_text.length; i++)
					{
						System.out.println("\t\t" + read_text[i]);
					}
				}
				System.out.println("\t]");
	
				socket.close();
				break;
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return -1;
		}
		
		if(devcnt == ipList.size())
			devcnt = -1;		
		return devcnt;
	}
*/
	public int fileWhere(String filename) 
	{
		int devcnt=0;

		if (fileExist(filename) == -1)
			return -1;

		return devcnt;	
	}
	public int fileWhere(String filename, String ipAddress) 
	{
		int devcnt=0, i;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			if(fileIs(filename, socket) == -1)
				devcnt = -2;

			OutputStream os = socket.getOutputStream();
			os.write(finish_cmd.getBytes() ); // file list
			os.flush();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.print(devcnt);
		return devcnt;	
	}
	public int fileWhere(String filename, ArrayList<String> ipList) 
	{
		int devcnt=0, check=0;

		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt ++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);
				check = fileIs(filename, socket); 

				OutputStream os = socket.getOutputStream();
				os.write(finish_cmd.getBytes() ); // file list
				os.flush();
				socket.close();
				
				if(check == 1)
					break;
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.print(devcnt);
		if(devcnt == ipList.size())
			devcnt = -2;
		
		return devcnt;	
	}
	
	public int fileLength(String filename)
	{
		long len = 0;

		if (fileExist(filename) == -1)
			return -1;
		
		File file = new File(filename);
		len = file.length();	
		return (int)len;
	}
	public int fileLength(String filename, String ipAddress)
	{
		int devcnt=0, i;
		long len = 0;
		try {
			Socket socket = new Socket(ipAddress, 8888);
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();

			if(fileIs(filename, socket) == -1)
			{
				os.write(finish_cmd.getBytes() ); // file list
				os.flush();
				return devcnt;
			}

			remote_cmd = "length#" + filename;
//				os.write( "length#".getBytes() ); // file list
//				os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();

			byte[] data =new byte[100];
			int n = is.read(data);
			if(n > 0)
			{
				String length =new String(data,0,n);
				len = Long.parseLong(length);
			}

			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
//		System.out.println("\tfile Size : " + len);
		return (int)len;
	}
	public int fileLength(String filename, ArrayList<String> ipList)
	{
		int devcnt=0, i;
		long len = 0;
		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);
				OutputStream os = socket.getOutputStream();
				InputStream is = socket.getInputStream();

				if(fileIs(filename, socket) == -1)
				{
					os.write(finish_cmd.getBytes() ); // file list
					os.flush();
					continue;
				}

				remote_cmd = "length#" + filename;
//					os.write( "length#".getBytes() ); // file list
//					os.write( filename.getBytes() ); // file list
				os.write(remote_cmd.getBytes());
				os.flush();

				byte[] data =new byte[100];
				int n = is.read(data);
				if(n > 0)
				{
					String length =new String(data,0,n);
					len = Long.parseLong(length);
				}

				socket.close();
				
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
//		System.out.println("\tfile Size : " + len);
		return (int)len;
	}
	
	public int fileCreate(String filename, String ipAddress)
	{
		int devcnt=0, i;

		return -1;
	}
	
	public int fileRemove(String filename, String ipAddress)
	{
		int devcnt=0, i;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			OutputStream os = socket.getOutputStream();

			if(fileIs(filename, socket) == -1)
			{
				os.write(finish_cmd.getBytes() ); // file list
				os.flush();
				return -1;
			}

			remote_cmd = "remove#" + filename;
//				os.write( "remove#".getBytes() ); // file list
//				os.write( filename.getBytes() ); // file list
			os.write(remote_cmd.getBytes());
			os.flush();

			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(cmd);
		return devcnt;
	}
	
	private static String finish_cmd="finish";
	public String remote_cmd="";
	public Connection conn = null;
	public Statement state = null; 
	public PreparedStatement pstate = null;
	public ResultSet rs = null;
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
	public String uuid="", location="";
	public int security=-1, sharing=-1;
}


//Runtime.getRuntime().exec("chmod 777" + foldername[i]); 
//Runtime.getRuntime().exec("echo keti2014 | sudo chmod 777" + foldername[i]); 
/*
String cmd = "sudo chmod 777 -R" + foldername[i] + "\nketi2014\n"; // sudo + password
Runtime rt = Runtime.getRuntime();
Process prc = rt.exec(cmd);
//prc.waitFor();
//System.out.flush();
//AccessController.checkPermission(new FilePermission(foldername[i]+"*", "read,write"));

//Runtime.getRuntime().exec("sudo vi " + path[i] + "\nketi2014\n" + ":wq\\n"); 
//Runtime.getRuntime().exec("sudo chmod 777 " + path[i] + "\nketi2014\n"); 
path[i].createNewFile(); // error : permission denied / case : remote
path[i].setReadable(true, false);
*/				
// local file create = success, remote file create = false
