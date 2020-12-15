package kr.re.keti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;

import javax.imageio.stream.FileImageInputStream;

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
		origin_foldername = fname;
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
						BufferedReader br = new BufferedReader(new FileReader("edge_ipList.txt"));
						String lines = "";
						String line = br.readLine();
						while (true) {
							line = br.readLine();
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
						Thread.sleep(5);
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
//				        			foldername = rs.getString("location");
				        			foldername = origin_foldername;
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
								Thread.sleep(5);
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
//								System.out.println(security);
								if(security == 1)
								{
									n = is.read(data);
									String hash_code = new String(data, 0, n);
									String[] hash = hash_code.split("#");
//									System.out.println(hash[0] + " : " + hash[1]); 

									StringBuffer sb = new StringBuffer();
									try {

										MessageDigest md = MessageDigest.getInstance("SHA-256");
										FileInputStream fis = new FileInputStream("hash_info.txt");
								        
										byte[] dataBytes = new byte[1024];
								     
										int nread = 0; 
										while ((nread = fis.read(dataBytes)) != -1) {
											md.update(dataBytes, 0, nread);
								  		};
//								  		System.out.println(hash[0]);
										md.update(hash[0].getBytes(), 0, hash[0].length());
								  		byte[] mdbytes = md.digest();
								     
										for (int i = 0; i < mdbytes.length; i++) {
											sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
										}
										fis.close();										
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									
									if(sb.toString().equals(hash[1]))
									{
										System.out.println("\tauthentication hash code (" + sb.toString() + ") is correct");
										os.write("yes".getBytes());
										os.flush();
									}
									else
									{
										System.out.println("\tauthentication hash code (" + sb.toString() + ") is not correct");
										os.write("no".getBytes());
										os.flush();
										break;
									}
									
								}
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
									System.out.println("\tfinish to transmit the DATA : " + array[1]); //v1102

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
						}else if(array[0].equals("save")){
							check = 0;
							int dividence = Integer.parseInt(array[1].split("!")[1]);
							//String dvfile_name = array[1].split("_")[0];
							
							//String Myshare_tmp = array[1].split("_")[1];
							
							//String Myshare = Myshare_tmp.split(".part")[0];
							
							
							String[] delpart = array[1].split(".part!");
							//System.out.println(delpart[0]);
							//System.out.println(delpart.length);
							String Myshare = delpart[0].split("_")[delpart[0].split("_").length-1]; 
							//System.out.println(Myshare);
							String dvfile_name = delpart[0].replace("_"+Myshare, "");
							//System.out.println(dv_file_name);
							
												
							File file = new File(foldername + dvfile_name);
							
							int readBytes;
							int DEFAULT_BUFFER_SIZE = 100000;
							byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
							long totalReadBytes = 0;
							long fileSize = file.length();
							
							//System.out.println("dvfile : " + dvfile_name);
							
							try {
								System.out.println("Requested data(" +  dvfile_name +") Hash : " + sha(foldername + dvfile_name));
								split(foldername + dvfile_name, false, dividence);
								
								
								FileInputStream fis = new FileInputStream(foldername + dvfile_name + "_" + Myshare); //editing khlee
					            while ((readBytes = fis.read(buffer)) > 0) {
					            	//System.out.println("read byte : " + readBytes);
					                os.write(buffer, 0, readBytes);
					                os.flush();
					                totalReadBytes += readBytes;
//					                System.out.println("In progress: " + totalReadBytes + "/"
//					                        + fileSize + " Byte(s) ("
//					                        + (totalReadBytes * 100 / fileSize) + " %)");
					            }
					            fis.close();
					            
					            System.out.println("Divided data(" + dvfile_name + "_" + Myshare + ") Hash : " + sha(foldername + dvfile_name + "_" + Myshare));
					            
					            //new File(foldername + dvfile_name + "_" + Myshare).delete();
					            
					            //System.out.println("Send file is deleted");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							File info[] = new File(foldername).listFiles();
				    		for(File fi : info) {
				    			if(fi.getName().contains(dvfile_name+"_")) {
				    				if(fi.delete()) {
				    					//System.out.println("delete success");
				    				}else {
				    					//System.out.println("delete false");
				    				}
				    			}
				    		}
							message = "finish";
						}
						else if (array[0].equals("hash")) {

							
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
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}

//		System.out.println("-> finish"); //v1102
//		run();
		
	}
	public static void split(String filename, boolean deleteOrgFile, int dividence) throws Exception{
		try {
			File file = new File(filename);
			String path = file.getParent();
			FileInputStream in = new FileInputStream(file);
			
			int cut = dividence;
			long originTotalFileLength = file.length();
			long cutsize = (((originTotalFileLength / (1024*1024)) / cut) + 1) * (1024*1024);
			long jobProcess = cutsize;
			long chunkCnt = (long)Math.ceil((double)originTotalFileLength / (double)cutsize);
			
			int ofcnt = 8;
			for (int i = 1; i <= chunkCnt; i++) {
				FileOutputStream fout = new FileOutputStream(filename + "_" + String.format("%02d", (i-1)));
				int len = 0;
				byte[] buf = new byte[1024];
				while ((len = in.read(buf, 0, 1024)) != -1) {
					fout.write(buf, 0, len);
					jobProcess = jobProcess + len;
					if (cutsize * (i + 1) == jobProcess) break;
				}
				fout.flush();
				fout.close();
			}
			in.close();

			if (deleteOrgFile && new File(filename).exists()) new File(path, filename).delete();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
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

//	public static String filename;
	public static String origin_foldername = "/home/eunae/keti/";
	public static String foldername = "/home/eunae/keti/";
	public Connection conn = null;
	public Statement state = null; 
	public PreparedStatement pstate = null;
	public ResultSet rs = null;
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
	public String select_sql = "SELECT * FROM file_management"; //(file_name, uuid, security, sharing, location)
	String file_name="", uuid="", location="";
	int security=-1, sharing=-1;
}
