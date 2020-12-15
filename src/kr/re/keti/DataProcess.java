package kr.re.keti;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.MessageDigest;
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
	public DataProcess(String fname)
	{
		try {
			Class.forName("com.mysql.cj.jdbc.Driver"); //com.mysql.jdbc.Driver
			conn = DriverManager.getConnection(url, "file_user", "edgecomputing"); // DB_URL, USER_NAME, PASSWARD
			state = conn.createStatement();

			origin_foldername = fname;
			foldername = fname;

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
	public String fileInfo(String filename)
	{
		String message = null;
		if(filename.equals(uuid_file) && security==1) {
			message = "[name:" + filename + ", UUID:" + uuid +", location:" + location + "]\n";
			message += "* DATA is indirect access data.";

			//	      System.out.println();
			//	      System.out.println();
		}
		else if(filename.equals(uuid_file) && security==2) {
			message = "[name:" + filename + ", UUID:" + uuid +", location:" + location + "]\n";
			message += "* DATA is direct access data.";

			//	      System.out.println("* DATA is direct access data.");
			//	      System.out.println("* DATA is [name:" + filename + ", UUID:" + uuid +", location:" + location + "]");
		}
		return message;
	}

	public int fileExist(String filename) // real exists in node
	{
		File file = new File(filename);
		if(!file.exists())
		{
			//	        System.out.println("\tfile_list need to update.");
			return -1;
		}
		//		System.out.println("\tlocal : file exist");
		return 1;
	}
	public int fileExist(String filename, String ipAddress)
	{
		int devcnt = -1;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			// because of receive role - socket.isConnected() always true;
			//boolean connected = socket.isConnected() && ! socket.isClosed(); 
			while(!socket.isConnected())	 {				}

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
				System.out.println("Answer : " + msgs);
				//				System.out.print("\t -> " + msg[2] + ":"  + msg[3] + ":"  + msg[4] + ":" + msg[5]);
				if(msg[1].equals("DATA") && msg[msg.length-1].equals("DATA") && msg[4].equals("0")) // �뜝�럩�쐸�솻洹⑥삕 �뜝�럥�냱�뜝�럩逾у뜝�럩逾� �뜝�럩肉녑뜝�럥裕� �썒�슣�닔占쎄틬占쎈ご�뜝占� db�뜝�럥�뱺 �뜝�룞�삕�뜝�럩�궋
				{
					System.out.printf("\t" + ipAddress);
					//					String msg = "#DATA#" + uuid + "#" + security + "#" + sharing + "#" + location + "#DATA#";
					uuid_file = filename;
					uuid = msg[2];
					security = Integer.parseInt(msg[3]);
					//					sharing = security; //Integer.parseInt(msg[4]) but �뜝�럩�쐸�솻洹⑥삕(0), 占썩뫀踰딉옙占�-�솻洹ｋ샍�뇡�쉻�삕甕곕쵇臾억옙留⑵굢�맮�삕�얜�먯삕占쎈뎄(1 or 2) 
					sharing = 1; //Integer.parseInt(msg[4]) but �뜝�럩�쐸�솻洹⑥삕(0), 占썩뫀踰딉옙占�(1) 
					location = ipAddress + ":/" + msg[5];
					if(security==1 && Integer.parseInt(msg[4])==0) // link 占썩뫀踰딉옙占� -> �뜝�럩�쐸�솻洹⑥삕 ip save 
						origin_ip = ipAddress;

					String select_sql = "SELECT * FROM file_management where uuid='" + uuid +"'";
					rs = state.executeQuery(select_sql); //SQL占쎈닱筌뤾쑴諭� �뜝�럩�쓧�뜝�럥堉롥뜝�럥由��뜝�럥�뿰 �뜝�럥堉꾢뜝�럥六�
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
						// INSERT�뜝�럥裕� �뛾�룇瑗뱄옙�꼶�뜝�럥�뵹�뜝�럥裕� �뜝�럥�몥�뜝�럩逾졾뜝�럡�댉�뜝�럥援뜹뜝�럩逾� �뜝�럥�뵪�뜝�럩紐득쾬�꼻�삕�슖�댙�삕 ResultSet �뤆�룇鍮섊뙼�뭿泥롥뜝占� �뜝�럥�닡�뜝�럩�뭵 �뜝�럥�뵪占썩뫅�삕, �뛾�룆��餓ο옙 pstmt.executeUpdate()嶺뚮∥�뾼�땻�슪�삕獄��쓣紐닷뜝占� �뜝�럩源덌옙鍮듿뜝占�
						// INSERT, UPDATE, DELETE 占쎈쐩占쎈닑占쎈뉴�뜝�럥裕� �뤆�룇�듌�뜝占� 嶺뚮∥�뾼�땻�슪�삕獄��쓣紐닷뜝占� �뜝�럩源덌옙鍮듿뜝占�
						// @return     int - 嶺뚮쪋�삕 �뤆�룇裕뉛옙踰� row�뤆�룊�삕 �뜝�럩寃ュ뜝�럥�깿�뜝�럩諭� 亦껋꼶梨뤄옙占썲뜝�럥裕됬춯�쉻�삕占쎈ご�뜝占� �뛾�룇瑗뱄옙�꼶
						int db = pstate.executeUpdate();
						if( db == 0 ){
							System.out.println("\t - Original DATA DB_info Received : failure");
						}
						else{
							System.out.println("\t - Original DATA DB_info Received : success");
						}
					}
					/*		        	
		        	rs = state.executeQuery(select_sql); //SQL�뜝�럥�떛嶺뚮ㅎ�뫒獄�占� 占쎈쐻占쎈윪占쎌벁占쎈쐻占쎈윥�젆濡λ쐻占쎈윥�뵳占쏙옙�쐻占쎈윥占쎈염 占쎈쐻占쎈윥�젆袁��쐻占쎈윥筌묕옙
		        	while(rs.next()){
		        		// �뜝�럩�읉占쎄턀�겫�뼔援▼뜝�럩踰� �뇖�궡�닑占쎌뱿�뜝�룞�삕 �뛾�룄�ｈ굢�떥占썩뫅�삕 �뜝�럥堉롳옙逾녑뜝占� 0占쎄껀�뜝�룞�삕�땻占� �뜝�럥六삣뜝�럩�굚�뜝�럥由�嶺뚯쉻�삕 �뜝�럥瑜ワ옙�뫅�삕 1占쎄껀�뜝�룞�삕�땻占� �뜝�럥六삣뜝�럩�굚�뜝�럥由썲뜝�럥堉�.
		                // �뜝�럥�몥�뜝�럩逾졾뜝�럡�댉�뵓怨쀬쪠占쎈턄�뜝�럥裕욃뜝�럥�뱺�뜝�럡�맋 �뤆�룊�삕�뜝�럩二у뜝�럩沅롥뜝�럥裕� �뜝�럥�몥�뜝�럩逾졾뜝�럡�댉�뜝�럩踰� �뜝�룞�삕�뜝�럩肉��뜝�럥�뱺 嶺뚮씮�돰�떋占� getString �뜝�럩援℡뜝�럥裕� getInt �뜝�럥苡삣뜝�럩諭� �뜝�럩源덌옙鍮딉옙裕됮뇡�냲�삕占쎈펲.
		        		String file_name = rs.getString("file_name");
		        		String uuid = rs.getString("uuid");
		        		int security = rs.getInt("security");
		        		int sharing = rs.getInt("sharing");
		        		String location = rs.getString("location");
		        		System.out.print("\n\nName: "+ file_name + "\nUUID: " + uuid + "\nSecurity: " + security); 
		        		System.out.println("\nSharing: "+ sharing + "\nLocation: " + location + "\n--------------------------\n");
		        	}
		        	rs.close();        	
					 */
				}
			}
			else{
				System.out.println("Answer : doesn't had DATA.");
			}

			OutputStream os = socket.getOutputStream();
			os.flush();
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
	public int fileRead(String filename, String ipAddress) throws SocketTimeoutException // exist and read direct.
	{
		int devcnt=1, i;

		try {
			Socket socket = new Socket(ipAddress, 8888);
			while(!socket.isConnected() || socket.isClosed())	 {				}


			//			if(fileIs(filename, socket) == -1)
			//				return -1;

			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();

			remote_cmd = "read#" + filename;
			//				os.write( "read#".getBytes() ); // file list
			//				os.write( filename.getBytes() ); // file list
			os.flush();
			os.write(remote_cmd.getBytes());
			os.flush();

			Thread.sleep(5);
			String hash_code = hashExchange("hash_info.txt", ipAddress);
			System.out.print("authentication hash code : " + hash_code);
			hash_code = ipAddress + "#" + hash_code;
			os.write(hash_code.getBytes());
			os.flush();

			byte[] data = new byte[256];
			int len = is.read(data);
			String msgs ="";
			if(len >= 0)
			{
				msgs =new String(data,0,len);
			}
			//			System.out.println(msgs);
			if(msgs.equals("yes"))
			{
				System.out.println(" - success");
				msgs = msgs.replace("yes", "");
			}
			else if(msgs.indexOf("yes") == 0)
			{
				System.out.println(" - success");
				msgs = msgs.replace("yes", "");
			}
			else {
				System.out.println(" - failure");
				return -1;
			}


			Thread.sleep(5);
			System.out.println("\t[");
			String resultFromServer = "";
			while (true)
			{
				len = is.read(data);
				if(len < 0)
					break;
				resultFromServer = new String(data,0,len);
				if(msgs.length() > 0)
				{
					resultFromServer = msgs + resultFromServer;
					msgs = "";
				}
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
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return devcnt;
	}
	public int fileRead(String filename, ArrayList<String> ipList) {
		int check = 0;
		if(security == 1) //link
	    {
	      try {
	        System.out.println("request to " + origin_ip);
	        check = fileRead(filename, origin_ip);
	///////////////////////////////////////////////////////////////////v1209.3
	        PrintWriter fprogress;
	        fprogress = new PrintWriter(foldername + "/" + uuid + ".meta");
	        fprogress.println(security);  //length
	        long file_length = fileLength(filename, origin_ip);
	        fprogress.println(file_length);  //length
	        fprogress.println(origin_ip);  //length
	        fprogress.close();
	///////////////////////////////////////////////////////////////////v1209.3
	        
	      } catch (SocketTimeoutException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	      } catch (FileNotFoundException e1) {
	        // TODO Auto-generated catch block
	        e1.printStackTrace();
	      }
	    }
		else if(security == 2) // data receive & save
		{
			long file_length = fileLength(filename, ipList.get(0));

			//System.out.println("Request file name : " + filename);
			//System.out.println("Request file size : " + file_length);
			//System.out.println("Request file uuid : " + uuid);


			try {
				BufferedReader br = new BufferedReader(new FileReader("folder_name.txt"));
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;

					foldername = line;
				}
				//System.out.println("folder name : " + foldername);
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}


			//localhost�뤆�룊�삕 �뤆�룊�삕嶺뚯쉻�삕占썩뫅�삕 �뜝�럩肉녑뜝�럩紐든춯濡녹삕 �뜝�럥�닡占쎈닱占쎈뼬�떋�띿삕�뙴占� �뜝�럥�닱�뜝�럥由�占썩뫅�삕 �슖�돦裕뉛쭚袁ъ삕占쎄퉰�뜝�럥裕욃뜝�럥諭� �뜝�럥爰뽩뜝�럩占쏙옙紐닷뜝占� �뜝�럥瑜댐옙逾녑뜝占�.
			try {
				InetAddress ip = InetAddress.getLocalHost();
				//System.out.println("My ip : " + ip.getHostAddress());
				for(String ipl : ipList) {
					if(ipl.equals(ip.getHostAddress())){
						System.out.println("already have local");
						return -2;
					}
					if(ipl.equals("localhost")){
						System.out.println("already have local");
						return -2;
					}
					if(ipl.length() == 0) {
						System.out.println("Anyone doesn't had DATA.");
						return -2;
					}
				}
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}



			//int chunk_size = 100000;
			check = 0;
			//int chunk_num = 0;
			long cutsize = (((file_length / (1024*1024)) / ipList.size()) + 1) * (1024*1024);
			long wh_length = file_length;



			//filename_00.part �뜝�럩援ⓨ뜝�럥六�
			ArrayList<String> part_names = new ArrayList<String>();
			ArrayList<String> incomming_filename = new ArrayList<String>();
			for(int i = 0; i < ipList.size(); i++) {
				part_names.add("save#" + filename + "_" + String.format("%02d", i) + ".part!"+ipList.size());
				incomming_filename.add(foldername + filename + "_" + String.format("%02d", i));
				//System.out.println("part_names : " + part_names.get(i));
				//System.out.println("incomming_filename : " + incomming_filename.get(i));
			}



			PrintWriter fprogress;
			try {
				fprogress = new PrintWriter(foldername + "/" + uuid + ".meta");
				fprogress.println(security);  //length
				fprogress.println(file_length);  //length
				fprogress.println(ipList.size() * 10); //num of files
				fprogress.println((int)(file_length / (ipList.size()*10)));  //size of part
				fprogress.println(ipList.size());  //node num
				fprogress.println(filename);  //filename
				for(int i=0; i < ipList.size(); i++) {
					fprogress.print(ipList.get(i)+" ");  //iplist
					fprogress.println(i*10 + " " + ((i+1)*10-1));  //iplist
				}
				fprogress.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			

			ArrayList<Long> splitsize = new ArrayList<Long>();
			for(int i =0; i < ipList.size(); i++) {
				if(wh_length <= cutsize) {
					splitsize.add(wh_length);
				}else {
					splitsize.add(cutsize);
				}
				wh_length -= cutsize;
			}
			//		for(int i =0; i < ipList.size(); i++) {
			//			System.out.println("part size : " + splitsize.get(i));
			//		}

			int progresscnt = 0;

			//iplist
			//String ip, String uuid, String partname, String incomming_name, long length, int nodes
			if(file_length < 10000000) {//under 10MB
				Thread reqs = new Thread();
				reqs = new Thread(new EdgeRequest(ipList.get(0), uuid, "save#" + filename + "_00" + ".part!1", foldername + filename + "_00" , file_length, 1));
				reqs.start();
				try {
					reqs.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}else {
				Thread[] reqs = new Thread[ipList.size()];
				for(int i = 0; i < ipList.size(); i++) {
					reqs[i] = new Thread(new EdgeRequest(ipList.get(i), uuid, part_names.get(i), incomming_filename.get(i), splitsize.get(i), ipList.size()));
					reqs[i].start();
				}

				for(int i = 0; i < ipList.size(); i++) {
					try {
						reqs[i].join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			try {
				if(ipList.size() == 1) {
					//
					File ori = new File(incomming_filename.get(0));
					ori.renameTo(new File(foldername + filename));
					//System.out.println("just rename");
				}else {
					merge(foldername+filename, incomming_filename, true);
				}

				//System.out.println("finish file path : " + foldername + filename);
				System.out.println("Read Finish Hash : " + sha(foldername + filename));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}



			//fileRead(foldername + filename);

			//for占쎈닱筌뤾쑴紐드슖�댙�삕 �뤆�룄�궖�뚮슁�삕占쎈꺄 ipList�뜝�럥�뱺 �뜝�럥爰뽳옙�끂�굦占쎈굵 �뜝�럥�뿼�뇦猿됲�ч뇡占쏙옙�뫅�삕 read#ipList.size#�뜝�럥�뻹�뜝�럡�맋 占쎈ご�뜝占� 占쎄껀占쎈듌�굢�띿삕�땻占� 嶺뚮ㅏ援앲�곕뵃�삕�젆湲븍ご�뜝占� �뜝�럡�뀏�뜝�럩�졎繞벿우삕.
			//start�뜝�룞�삕 end�뜝�럥裕� �뜝�럥�냱�뜝�럩逾у뜝�럩踰� �뜝�럡苡욜뼨轅명�▼뜝�룞�삕 �뜝�럥瑜닷뜝�럥�닡�뜝�럥�뒍 占썩몿�뫒亦낆룊�삕�뇡占� �뜝�럥�빢 �뜝�럩肉녑뜝�럩踰�.
			//chunk �뜝�럡�뀬�뜝�럩逾좂춯�빖�쐞占쎈츎 file_size / ipList.size
			//
			//check == 1 : success // else : false

			check = 1;
		}


		return check ;
	}

	public static String sha(String filepath) throws Exception{
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
	public static void merge(String newFileName, ArrayList<String> filenames, boolean removeSplittedFiles) throws Exception {

		FileOutputStream fout = null;
		FileInputStream in = null;
		BufferedInputStream bis = null;
		try {
			//System.out.println("merge) newFilename : " + newFileName);
			File newFile = new File(newFileName);
			fout = new FileOutputStream(newFile);

			for (String fileName : filenames) {
				//System.out.println("merge) for merge filenames : " + fileName);
				File splittedFile = new File(fileName);
				in = new FileInputStream(splittedFile);
				bis = new BufferedInputStream(in);
				int len = 0;
				byte[] buf = new byte[1024];
				while ((len = bis.read(buf, 0, 1024)) != -1) {
					fout.write(buf, 0, len);
				}
				in.close();
				if(removeSplittedFiles && splittedFile.exists()) {
					if(splittedFile.delete()) {
						System.out.println("merge File deleted");
					}else {
						System.out.println("merge File deleted fail");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {
				fout.close();

				bis.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public String hashExchange(String filename, String ipAddress) throws SocketTimeoutException // exist and read direct.
	{
		int devcnt=1, i;

		StringBuffer sb = new StringBuffer();
		byte[] data = new byte[256];

		try {

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			FileInputStream fis = new FileInputStream(filename);

			byte[] dataBytes = new byte[1024];

			int nread = 0; 
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			};
			//			System.out.println(ipAddress);
			md.update(ipAddress.getBytes(), 0, ipAddress.length());
			byte[] mdbytes = md.digest();

			for (i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			//System.out.println("SHA-256 : " + sb.toString());
			fis.close();


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sb.toString();
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

	public long fileLength(String filename)
	{
		long len = 0;

		if (fileExist(filename) == -1)
			return -1;

		File file = new File(filename);
		len = file.length();	
		return len;
	}
	public long fileLength(String filename, String ipAddress)
	{
		int devcnt=0, i;
		long len = 0;
		try {
			Socket socket = new Socket(ipAddress, 8888);
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();

			//			if(fileIs(filename, socket) == -1)
			//			{
			//				os.write(finish_cmd.getBytes() ); // file list
			//				os.flush();
			//				return devcnt;
			//			}

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
		return len;
	}
	public long fileLength(String filename, ArrayList<String> ipList)
	{
		int devcnt=0, i;
		long len = 0;
		try {
			for(devcnt=0; devcnt<ipList.size(); devcnt++)
			{
				Socket socket = new Socket(ipList.get(devcnt), 8888);
				OutputStream os = socket.getOutputStream();
				InputStream is = socket.getInputStream();
				//
				//				if(fileIs(filename, socket) == -1)
				//				{
				//					os.write(finish_cmd.getBytes() ); // file list
				//					os.flush();
				//					continue;
				//				}
				//
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
		return len;
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
	// rs.absolute(low_index);
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
	public String uuid_file="", uuid="", location="", origin_ip="";
	public int security=-1, sharing=-1;
	public static String origin_foldername = "/home/eunae/keti/";
	public static String foldername = "/home/eunae/keti/";
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
