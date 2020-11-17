package kr.re.keti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;


public class ReceiveWorker implements Runnable 
{
	/*
	 * public static void main(String[] args) { while (true) {
	 * System.out.println("waiting"); // extend Thread RemoteNode tsm1 = new
	 * RemoteNode(); tsm1.start(); //System.out.println(getName()); // extend Thread
	 * // implements Runnable Runnable r = new RemoteNode(); Thread tsm2 = new
	 * Thread(r); tsm2.start();
	 * //System.out.println(Thread.currentThread().getName()); // implements
	 * Runnable try { tsm1.join(); } catch (InterruptedException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } } }
	 */
	ReceiveWorker(String fname)
	{
		foldername = fname;
    	try {
			Class.forName("com.mysql.cj.jdbc.Driver"); //com.mysql.jdbc.Driver
        	conn = DriverManager.getConnection(url, "file_user", "edgecomputing"); // DB_URL, USER_NAME, PASSWARD
        	state = conn.createStatement();
        	System.out.println("File Management DB connection : success\n");
        	rs = state.executeQuery(select_sql); //SQL문을 전달하여 실행
        	while(rs.next()){
        		// 레코드의 칼럼은 배열과 달리 0부터 시작하지 않고 1부터 시작한다.
                // 데이터베이스에서 가져오는 데이터의 타입에 맞게 getString 또는 getInt 등을 호출한다.
        		String file_name = rs.getString("file_name");
        		String uuid = rs.getString("uuid");
        		int security = rs.getInt("security");
        		int sharing = rs.getInt("sharing");
        		String location = rs.getString("location");
        		System.out.print("Name: "+ file_name + "\nUUID: " + uuid + "\nSecurity: " + security); 
        		System.out.println("\nSharing: "+ sharing + "\nLocation: " + location + "\n--------------------------\n");
        	}
        	rs.close();        	

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void run() {
		int check = 0;
		while(true)
		{
			try {
				ServerSocket ss = new ServerSocket(8888);
				Socket cs = ss.accept();
				String file_name="", uuid="", location="";
	    		int security=-1, sharing=-1;

				OutputStream os = cs.getOutputStream();
				InputStream is = cs.getInputStream();
//	            BufferedReader read = new BufferedReader(new InputStreamReader(is));
//	            String message = read.readLine();

				byte[] data = new byte[100];
				int n = is.read(data);
				String message = new String(data, 0, n);

				if (message.equals("list")) {
	//v1102				System.out.println(message);
					System.out.println("\n* Send the slaves IP list");
					try {
						BufferedReader br = new BufferedReader(new FileReader("slave_ipList.txt"));
						String lines = "";
						while (true) {
							String line = br.readLine();
							if (line == null)
								break;

							if (lines.length() + line.length() > 100) {
								os.write(lines.getBytes());
								os.flush();
//								System.out.print(lines+"\t");
								lines = "";
								Thread.sleep(5);
							}
//	                        line = line;
							lines += line + "\n";
						}
						os.write(lines.getBytes());
						os.flush();
//						System.out.println(lines);

						br.close();
						message = "finish";

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} 
				else {
					while (!message.equals("finish")) {
						System.out.println("\tReceive the request : " + message); //v1102
						String[] array = message.split("#");
						
						if (array[0].equals("exist")) {
//							System.out.println(foldername + array[1]); //v1102
				        	rs = state.executeQuery(select_sql); //SQL문을 전달하여 실행
				        	while(rs.next()){
				        		file_name = rs.getString("file_name");
//				        		uuid = rs.getString("uuid");
//				        		security = rs.getInt("security");
//				        		sharing = rs.getInt("sharing");
//				        		location = rs.getString("location");
//				        		System.out.print("Name: "+ file_name + "\nUUID: " + uuid + "\nSecurity: " + security); 
//				        		System.out.println("\nSharing: "+ sharing + "\nLocation: " + location + "\n--------------------------\n");
				        		if(array[1].equals(file_name))
				        		{
				        			uuid = rs.getString("uuid");
					        		security = rs.getInt("security");
					        		sharing = rs.getInt("sharing");
					        		location = rs.getString("location");
				        			foldername = rs.getString("location");
				        			break;
				        		}
				        	}
				        	rs.close();        	

//				        	System.out.println(foldername + array[1]);
							File file = new File(foldername + array[1]); // real exist : inspect
							if (file.exists()) {
								System.out.println("\tSend the answer : Yes"); //v1102
								os.write("yes".getBytes());
								os.flush();
								Thread.sleep(5);
								
								String msg = "#DATA#" + uuid + "#" + security + "#" + sharing + "#" + location + "#DATA#";
								os.write(msg.getBytes());
								os.flush();
								Thread.sleep(5);
							} 
							else {
								System.out.println("\tSend the answer : No"); //v1102
								os.write("no".getBytes());
								os.flush();
							}

							data = new byte[100];
							n = is.read(data);
//	                        System.out.print(n);
							if (n < 0)
								message = "finish";
							else
								message = new String(data, 0, n);

						} 
						else if (array[0].equals("write")) {
							FileWriter fw = new FileWriter(foldername + array[1], true);
							while (true) {
								data = new byte[100];
								n = is.read(data);
								message = new String(data, 0, n);
//	                            message = read.readLine();

//								System.out.println(message); //v1102
								if (message.equals(":wq"))
									break;
								fw.write(message + "\n");

							}
							fw.close();
							message = "finish";

						} 
						else if (array[0].equals("read")) {
							check = 0;
							try {
								BufferedReader br = new BufferedReader(new FileReader(foldername + array[1]));
								String lines = "";
								System.out.println("	["); //v1102
								while (true) {
									String line = br.readLine();
									if (line == null)
										break;

									if (lines.length() + line.length() > 100) {
										os.write(lines.getBytes());
										os.flush();
//										System.out.println("\t\t" + lines); //v1102
										
										lines = "";
										Thread.sleep(5);
									}
//	                                line = line;
									check = 1;
									lines += line + "\n";
									int len = line.length();
									int start=0;
									while(start+100 < len)
									{
										System.out.println("\t\t" + line.substring(start, start+100)); //v1102
										start += 100;
									}
									System.out.println("\t\t" + line.substring(start, len)); //v1102
								}
								os.write(lines.getBytes());
								os.flush();
								System.out.println("	]"); //v1102
								if (check == 1)
									System.out.println("\tfinish the sending : " + array[1]); //v1102

								br.close();
								message = "finish";

							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						} 
						else if (array[0].equals("length")) {
							File file = new File(foldername + array[1]);
							os.write(String.valueOf(file.length()).getBytes());
							os.flush();
							message = "finish";
						} 
						else if (array[0].equals("remove")) {
							String cmd = "rm " + foldername + array[1];
							Runtime.getRuntime().exec(cmd);
							message = "finish";
						}
					}

				}
				os.close();
				is.close();
				cs.close();
				ss.close();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}

//		System.out.println("-> finish"); //v1102
//		run();
	}

//	public static String filename;
	public static String foldername = "/home/eunae/keti/";
	public Connection conn = null;
	public Statement state = null; 
	public PreparedStatement pstate = null;
	public ResultSet rs = null;
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
	public String select_sql = "SELECT * FROM file_management"; //(file_name, uuid, security, sharing, location)

}
