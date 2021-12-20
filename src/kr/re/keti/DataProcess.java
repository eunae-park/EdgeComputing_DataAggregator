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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.sun.management.OperatingSystemMXBean;

import kr.re.keti.ReceiveWorker.PacketProcessorImpl.ChunkTransfer;

public class DataProcess {

	public DataProcess(String dfname, String cfname, Database db, String ip, String tablename, String uuid)
	{
		database = db;
//		origin_foldername = fname;
		data_folder = dfname;
		folder = dfname.substring(dfname.length()-1);
		cert_folder = cfname;
		local_ip = ip;
		table_name = tablename;
		device_uuid = uuid;
		TCPSocketAgent.defaultPort = ketiCommPort;
	}
	
	public void SettingPort()
	{
		TCPSocketAgent.defaultPort = ketiCommPort;
	}
	public ArrayList<String> RequestSlaveList(String ip)
	{
		EdgeDeviceInfoClient client;
		ArrayList<String> slavelist = new ArrayList<String>();
		String remote_cmd="";

		try {
			remote_cmd = "{[{REQ::" + ip + "::001::SLAVE_LIST}]}";
//			System.out.println("!! RequestSlaveList : " + remote_cmd);
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestSlaveList : " + client.streamSocket_alive());
				return slavelist;
			}

			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());

			long start_client = System.currentTimeMillis();
			while(client.answerData == null)
			{			
				try {
					Thread.sleep(50);
					if(System.currentTimeMillis() - start_client > check_timeout )
					{
//						System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
						System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
			if(client.answerData != null)
			{
				String answer_data = new String(client.answerData, "UTF-8");
//				System.out.println("!! RequestSlaveList : " + answer);
				if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
					String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::"); // [0] = my_ip, [1]=003, [2]=metadata or none
					client.answerData = null;
					
//					System.out.println("!! slaveList : " + array[2]);
					if (array[2].equals("none"))
						return slavelist;
					for(int i=2; i<array.length; i++)
					{
//						System.out.println("!! slaveList : " + array[i]);
						slavelist.add(array[i]);
					}
				}
			}
//			client.stopWaitingResponse();
			client.stopRequest();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return slavelist;		
	}
	public void SendEdgeList(ArrayList<String> iplist, String result)
	{
		EdgeDeviceInfoClient client;

		try {
			for (String ip : iplist)
			{
//				System.out.println("!! dataprocess - " + result);
//				System.out.println("!! dataprocess - " + ip);
				client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP); // manaul setting
				if(!client.streamSocket_alive())
				{
					Logger logger = Logger.getLogger("MyLog");
				    FileHandler fh;
				    try {
				        // This block configure the logger with handler and formatter  
				        fh = new FileHandler("log/log");
				        logger.addHandler(fh);
				        SimpleFormatter formatter = new SimpleFormatter();
				        fh.setFormatter(formatter);
				    } catch (SecurityException e) {
				        e.printStackTrace();
				    } catch (IOException e) {
				        e.printStackTrace();
				    }
					logger.info("SendEdgeList : " + client.streamSocket_alive());
					return ;
				}
				client.startWaitingResponse();
				String remote_cmd = "{[{REQ::" + ip + "::" + "001" + "::EDGE_LIST::" + result + "}]}";

				client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
				long start_client = System.currentTimeMillis();
				while(client.answerData == null)
				{			
					try {
						Thread.sleep(50);
						if(System.currentTimeMillis() - start_client > check_timeout )
						{
//							System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
							System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
							break;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}			
				if(client.answerData != null)
				{
					String answer_data = new String(client.answerData, "UTF-8");
					if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
					{
//						System.out.println("!! RequestMessage : " + answer); // Send the answer 로 충분
						String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
						client.answerData = null;
						// [0] = my_ip, [1]=003, [2]=metadata or none
//							System.out.println("!! RequestMessage : " + req_code + array[1] + array[2]); // Send the answer 로 충분
//						if (array[1].equals("001"))
//						{
//							System.out.println("(Edge List transfer : " + array[2] + ")");
//						}
					}
					
				}

//				client.stopWaitingResponse();
				client.stopRequest();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public String RequestMessage(String req_content, String ip, String req_code) // real exists in node
	{
	    EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			if(req_code.equals("001"))
					remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::DEV_STATUS}]}";
			else if(req_code.equals("002"))
					remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::DATA_INFO}]}";
			else
				remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "}]}";
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestMessage : " + client.streamSocket_alive());
				return result;
			}

			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
//				System.out.println("!! RequestMessage start: " + client.answerData); // Send the answer 로 충분
			long start_client = System.currentTimeMillis();
			while(client.answerData == null)
			{			
				try {
					Thread.sleep(50);
//					System.out.println("!! delay time " + (System.currentTimeMillis() - start_client));
					if(System.currentTimeMillis() - start_client > check_timeout )
					{
//						result = "time";
//						System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
						System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
			if(client.answerData != null)
			{
				String answer_data = new String(client.answerData, "UTF-8");
				if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
//					System.out.println("!! RequestMessage : " + answer); // Send the answer 로 충분
					String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
					client.answerData = null;
					// [0] = my_ip, [1]=003, [2]=metadata or none
//						System.out.println("!! RequestMessage : " + req_code + array[1] + array[2]); // Send the answer 로 충분
					if (array[1].equals(req_code))
					{
						result = array[2];
					}
				}
			}
				
			
//			client.stopWaitingResponse();
			client.stopRequest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		System.out.println("!! RequestMessage : " + result); // Send the answer 로 충분
		return result;
	}
	public String RequestMessage(String req_content, String ip, String req_code, int start, int finish) // real exists in node
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "::" + start + "::" + finish + "}]}";
			
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestMessage2 : " + client.streamSocket_alive());
				return result;
			}

			client.startWaitingResponse();
			client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
//				System.out.println("!! RequestMessage start: " + client.answerData); // Send the answer 로 충분
			long start_client = System.currentTimeMillis();
			while(client.answerData == null)
			{			
				try {
					Thread.sleep(50);
					if(System.currentTimeMillis() - start_client > check_timeout )
					{
//						System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
						System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
			if(client.answerData != null)
			{
				String answer_data = new String(client.answerData, "UTF-8");
				if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
//					System.out.println("!! RequestMessage : " + answer); // Send the answer 로 충분
					String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
					client.answerData = null;
					// [0] = my_ip, [1]=003, [2]=metadata or none
//						System.out.println("!! RequestMessage : " + req_code + array[1] + array[2]); // Send the answer 로 충분
					if (array[1].equals(req_code))
					{
						result = array[2];
					}
				}
			}
//			client.stopWaitingResponse();
			client.stopRequest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String RequestMessage(String req_content, String ip, int data_code) // real exists in node 
	{
		EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestMessage3 : " + client.streamSocket_alive());
				return result;
			}
			client.startWaitingResponse();
//			System.out.println("!! RequestMessage2 : " + remote_cmd);
			if(data_code == 1) // read 
			{
				remote_cmd = "{[{REQ::" + ip + "::004::" + req_content + "}]}";
//				System.out.println("!! RequestMessage2 : " + remote_cmd);
				client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());

				if(securityLevel==5 && dataSize>=4)
				{ // == data가 분할되서 들어오는 경우
					boolean isWaiting = true;

					while(isWaiting)
					{
						long start_client = System.currentTimeMillis();
						while(client.answerData == null)
						{			
							try {
								Thread.sleep(50);
								if(System.currentTimeMillis() - start_client > check_timeout )
								{
//									System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
									System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
									break;
								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}			
						if(client.answerData != null)
						{
							String answer_data = new String(client.answerData, "UTF-8");
							String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
//							System.out.println("!! RequestMessage : " + array[2] + array[3]);
							if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
							{
									client.answerData = null;			// [0] = my_ip, [1]=003, [2]=metadata or none
//									System.out.println("!! RequestMessage : " +  array[3].equals("1"));
									if (array[1].equals("004") && array[3].equals("1"))
										result = array[5];
									else
										result += array[5];
							}
							if(array[1].equals("004") && array[3].equals(array[2]))
								isWaiting = false;
						}
					}
//					System.out.println("!! RequestMessage : " + result);
//					client.stopWaitingResponse();
				}
				else
				{
					long start_client = System.currentTimeMillis();
					while(client.answerData == null)
					{			
						try {
							Thread.sleep(50);
							if(System.currentTimeMillis() - start_client > check_timeout )
							{
////								System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
								System.out.println("\t!! Response Time is delayed over : " + remote_cmd);

								break;
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}			
					if(client.answerData != null)
					{
						String answer_data = new String(client.answerData, "UTF-8");
//						System.out.println("!! Dataprocess - " + answer);
						if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
						{
//								System.out.println("!! RequestMessage2 : " + answer); // Send the answer 로 충분
								String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
								client.answerData = null;
								// [0] = my_ip, [1]=003, [2]=metadata or none
								if (array[1].equals("004"))
								{
//									System.out.println(array[2]);
//									return array[2];
									result = array[5];
								}
						}
					}
					
				}
				
			}
			else if(data_code == 2) // write
			{
				Scanner sc = new Scanner(System.in);
//				System.out.println("What Do you Want to Write\t(Sign of Finish : !!finish!!)");
//				String contents="";
//				while(true)
//				{
//					String input = sc.nextLine();
//					if(input.equals("!!finish!!"))
//					{
//						break;
//					}
//					contents += input + '\n';
//				}
				
//				remote_cmd = "{[{REQ::" + ip + "::005::" + req_content + "::" + contents + "}]}";
				remote_cmd = "{[{REQ::" + ip + "::005::" + req_content + "}]}";
				client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
				long start_client = System.currentTimeMillis();
				while(client.answerData == null)
				{			
					try {
						Thread.sleep(50);
						if(System.currentTimeMillis() - start_client > check_timeout )
						{
//							System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
							System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
							break;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}			
				if(client.answerData != null)
				{
//					System.out.println(client.answerData);
//					System.out.println(client.answerData.indexOf("}]}"));
//					System.out.println(client.answerData.length()); // 왜 512가 나오는지 모르겠음.
//					System.out.println(client.answerData.indexOf("{[{ANS"));
					String answer_data = new String(client.answerData, "UTF-8");
					if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
					{
//							System.out.println("!! RequestMessage2 : " + answer); // Send the answer 로 충분
							String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
							client.answerData = null;
							// [0] = my_ip, [1]=003, [2]=metadata or none
							if (array[1].equals("005"))
							{
//								System.out.println(array[2]);
//								return array[2];
								result = array[2];
							}
					}
				}
			}
			else if(data_code == 3) // remove
			{
				remote_cmd = "{[{REQ::" + ip + "::006::" + req_content + "}]}";
				client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
				
				long start_client = System.currentTimeMillis();
				while(client.answerData == null)
				{			
					try {
						Thread.sleep(50);
						if(System.currentTimeMillis() - start_client > check_timeout )
						{
//							System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
							System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
							break;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}			
				if(client.answerData != null)
				{
//					System.out.println(client.answerData);
//					System.out.println(client.answerData.indexOf("}]}"));
//					System.out.println(client.answerData.length()); // 왜 512가 나오는지 모르겠음.
//					System.out.println(client.answerData.indexOf("{[{ANS"));
					String answer_data = new String(client.answerData, "UTF-8");
					if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
					{
//							System.out.println("!! RequestMessage2 : " + answer); // Send the answer 로 충분
							String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
							client.answerData = null;
							// [0] = my_ip, [1]=003, [2]=metadata or none
							if (array[1].equals("006"))
							{
//								System.out.println(array[2]);
//								return array[2];
								result = array[2];
							}
					}
				}
			}
			client.stopRequest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public byte[] RequestMessageByte(String req_content, String ip, int data_code) // real exists in node 
	{
		EdgeDeviceInfoClient client;
		byte[] result = null;
		int total_len=0;
		String remote_cmd="";

		try {
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestMessageByte : " + client.streamSocket_alive());
				return result;
			}
			client.startWaitingResponse();
//			System.out.println("!! RequestMessage2 : " + remote_cmd);
			if(data_code == 1) // read 
			{
				remote_cmd = "{[{REQ::" + ip + "::004::" + req_content + "}]}";
//				System.out.println("!! RequestMessage2 : " + remote_cmd);
				client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());

				if(securityLevel==5 && dataSize>=4)
				{ // == data가 분할되서 들어오는 경우
					boolean isWaiting = true;
					byte[] content_b = null;

					while(isWaiting)
					{
						long start_client = System.currentTimeMillis();
						while(client.answerData == null)
						{			
							try {
								Thread.sleep(50);
								if(System.currentTimeMillis() - start_client > check_timeout )
								{
//									System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
									System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
									break;
								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}			
						if(client.answerData != null)
						{
							String answer_data = new String(client.answerData, "UTF-8");
							String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");

//							System.out.println("!! RequestMessage : " + array[2] + array[3]);
							if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
							{
								System.out.println(answer_data.substring(0, answer_data.indexOf(array[5])));
								byte[] start = answer_data.substring(0, answer_data.indexOf(array[5])).getBytes("UTF-8"), finish = "}]}".getBytes("UTF-8");
								byte[] content; // = new byte[client.answerData.length-start.length-finish.length];
								
								if (array[1].equals("004") && array[3].equals("1"))
								{
									content = new byte[client.answerData.length-start.length-finish.length];
//									System.out.println("!! read request length: " + content.length);
									System.arraycopy(client.answerData, start.length, content, 0, content.length);
//									System.out.println("!! read request : " + new String(content));
									content_b = content;
								}
								else
								{
									content = new byte[client.answerData.length-start.length-finish.length + content_b.length];
//									System.out.println("!!read request length : " + content.length);
									System.arraycopy(content_b, start.length, content, 0, content_b.length);
									System.arraycopy(client.answerData, start.length, content, content_b.length, content.length);
//									System.out.println("!! read request : " + new String(content));
									content_b = content;
								}
								
								result = content;
							}
							if(array[1].equals("004") && array[3].equals(array[2]))
								isWaiting = false;
						}
					}
//					System.out.println("!! RequestMessage : " + result);
				}
				else
				{
					long start_client = System.currentTimeMillis();
					while(client.answerData == null)
					{			
						try {
							Thread.sleep(50);
							if(System.currentTimeMillis() - start_client > check_timeout )
							{
//								System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
								System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
								break;
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}			
					if(client.answerData != null)
					{
						String answer_data = new String(client.answerData, "UTF-8");
//						System.out.println("!! Dataprocess - " + answer_data);
						if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
						{
//								System.out.println("!! RequestMessage2 : " + answer); // Send the answer 로 충분
								String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
								// [0] = my_ip, [1]=003, [2]=metadata or none
								if (array[1].equals("004"))
								{
//									System.out.println("!!read request : " + answer_data.substring(0, answer_data.indexOf(array[5])));
									byte[] start = answer_data.substring(0, answer_data.indexOf(array[5])).getBytes("UTF-8"), finish = "}]}".getBytes("UTF-8");

									result = new byte[client.answerData.length-start.length-finish.length];
									System.arraycopy(client.answerData, start.length, result, 0, result.length);
//									System.out.println("!!read request : " + new String(result));
								}
						}
					}
					
				}
				
			}
			
			client.stopRequest();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	public String RequestMessageKETIRead(String req_content, String ip, String req_code)
	{
	    EdgeDeviceInfoClient client;
		String result = "none";
		String remote_cmd="";

		try {
			client = new EdgeDeviceInfoClient(ip, EdgeDeviceInfoClient.socketTCP);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("RequestMessageKETIRead : " + client.streamSocket_alive());
				return result;
			}
			client.startWaitingResponse();
			
			remote_cmd = "{[{REQ::" + ip + "::" + req_code + "::" + req_content + "}]}";
			client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
//			System.out.println("!! RequestMessageRead : " + remote_cmd); //datasize
//			System.out.println("!! RequestMessageRead : " + client.answerData); //datasize
			long start_client = System.currentTimeMillis();
			while(client.answerData == null)
			{			
				try {
					Thread.sleep(50);
					if(System.currentTimeMillis() - start_client > check_timeout )
					{
//						System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
						System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}			
			if(client.answerData != null)
			{
//				System.out.println("!! RequestMessageRead : " + client.answerData); //datasize
				
				String answer_data = new String(client.answerData, "UTF-8");
				String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");

				if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1 && array[1].equals(req_code))
				{
	/*			 //String
					if(array[1].equals("404")) // 기본 양식 맞음
					{
//						System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//						System.out.println("!! RequestMessageRead : " + array[3]); //datasize
						FileWriter fw = null; // 원본 공유 허용 데이터인 경우, 무조건 저장
						fw = new FileWriter(data_folder+"chunk/"+req_content, false);
						fw.write(array[3]);
						fw.close();
						result = "success";
					}
	*/
					//Byte
					if(array[1].equals("404")) // 기본 양식 맞음
					{
//						System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//						System.out.println("!! RequestMessageRead : " + array[3].length()); // 4kb보다 높다
//						System.out.println("!! RequestMessageRead 404 : " + array[3].getBytes()); //datasize
			            FileOutputStream fos = new FileOutputStream(data_folder+"chunk/"+req_content);
						System.out.println(answer_data.substring(0, answer_data.indexOf(array[3])));
						byte[] start=null, finish=null;
						try {
							start = answer_data.substring(0, answer_data.indexOf(array[3])).getBytes("UTF-8");
							finish = "}]}".getBytes("UTF-8");
						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						byte[] content; // = new byte[client.answerData.length-start.length-finish.length];
						
						try {
							content = new byte[client.answerData.length-start.length-finish.length];
//							System.out.println("!! read request length: " + content.length);
							System.arraycopy(client.answerData, start.length, content, 0, content.length);
//							System.out.println("!! read request : " + new String(content));

			                fos.write(content, 0, Integer.parseInt(array[2]));
			                fos.flush();
			                fos.close();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						result = "success";
					}
					else if(array[1].equals("444")) // 기본 양식 맞음
					{
//						System.out.println("!! RequestMessageRead : " + array[2]); //datasize

						if(array[2].equals(sha(req_content)))
						{
							System.out.println("* Recieved Data SHA code is the same as original.");
							result = "success";
						}
						else
						{
							System.out.println("* Recieved Data SHA code isn't the same as original.");
							result = "false";
						}
						System.out.println("\tOriginal Data SHA code :\t" + array[2]);
						System.out.println("\tRecieved Data SHA code :\t" + sha(req_content));
					}
				}
				
			}
			client.answerData = null;
//			System.out.println("!! RequestMessage : " + result);
			client.stopRequest();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			result = "false";
			e.printStackTrace();
		}
		
		return result;		
	}
	
	public static String cpu_shellCmd()
	{
		String command = "cat /proc/cpuinfo"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
//		System.out.println(command);
		Process process = null;
		InputStream is;
		InputStreamReader isr;
		BufferedReader br;
		String line="none";
		try {
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			int cnt=0;
			while(cnt < 4)
			{
				line = br.readLine();
				cnt ++;
//				System.out.println(line);
			}
			line = br.readLine();
			if(line == null)
				line = "none";
			String[] array = line.split(":");
			line = array[1].replaceAll(" ","");

			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return line;
	}
	public static int netload_shellCmd()
	{
//		cat /proc/net/dev | grep enp3s0 | awk '{print $2}'
//		cat /proc/net/dev | head -n 3 | tail -n 1
		String command = "cat /proc/net/dev"; //"gssdp-discover -t \"upnp:edgedevice\" -i \"br0\""; //"ls -al";  

		Runtime runtime = Runtime.getRuntime();
//		System.out.println(command);
		Process process = null;
		InputStream is;
		InputStreamReader isr;
		BufferedReader br;
		String line="none";
		String[] array;
		double rx=0, tx=0;
		try {
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			line = br.readLine();
			line = br.readLine();
			line = br.readLine();
//			System.out.println(line);
			if(line != null)
			{
				array = line.split(":");
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
//				System.out.println(array[1]);
				array = array[1].split(" ");
				rx = Double.parseDouble(array[1]); //55206812351
				tx = Double.parseDouble(array[9]);
//				System.out.println(rx);
//				System.out.println(tx);
//				System.out.println(array[1]);
//				System.out.println(array[9]);
			}
			
			br.close();
			isr.close();

			Thread.sleep(100);
			
			process = runtime.exec(command);
			is = process.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			line = br.readLine();
			line = br.readLine();
			line = br.readLine();
			if(line != null)
			{
				array = line.split(":");
//				System.out.println(array[1]);
				while(array[1] != array[1].replaceAll("  ", " "))
					array[1] = array[1].replaceAll("  ", " ");
//				System.out.println(array[1]);
				array = array[1].split(" ");
//				System.out.println(Math.abs(rx-Double.parseDouble(array[1])));
				rx =  (Math.abs(rx-Double.parseDouble(array[1])) * 8.0 / 1024.0 / 1024.0 / 0.1); //Mbps
				tx =  (Math.abs(tx-Double.parseDouble(array[9])) * 8.0  / 1024.0 / 1024.0 / 0.1); //Mbps
//				System.out.println(rx);
//				System.out.println(tx);
			}
			
			br.close();
			isr.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int load = (int) ((rx + tx) / 2);
		if(load==0 && rx!=0 || tx!=0)
			load = 1;
		else if(load>99)
			load = 99;
		
		return load;
	}
	public void DeviceInfomation() // 210428 add int func
	{
		double mb = 1024.0 * 1024.0;
		double gb = 1024.0 * 1024.0 * 1024.0;
		
		// 프로세서 부하량
		OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
		/* getSystemLoadAverage ()
		// 1보다 낮은 값을 유지하면 서버의 상태 원활하고, 1보다 높으면 서버가 부하를 받기 시작한다고 판단할 수 있다. 
		// 평균적으로 5 이상을 시스템에 과부하가 있다고 판단하지만 절대적인 판단의 기준은 절대 아니다.*/
		int process_load=0;
		if (osBean.getSystemLoadAverage() != -1) // window 에선 -1 
			process_load = (int) Math.ceil(osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100); // cpu사용량 - osBean.getSystemCpuLoad() * 100;
		
		// 물리적 실제 메모리 
		// memory 사용량 : free -m	
		double mem_free = (double)osBean.getFreePhysicalMemorySize();
		double mem_total = (double)osBean.getTotalPhysicalMemorySize();
		double mem_usage = Math.ceil((mem_total-mem_free) / mem_total * 100);
//		System.out.println("!! DeviceStatusInfo - Total Memory : " + mem_total/gb + "[GB]"); 			
//		System.out.println("!! DeviceStatusInfo - free Memory : " + mem_free/gb + "[GB]"); 			
//		System.out.println("!! DeviceStatusInfo - Memory Usage : " + String.format("%.2f", (mem_total-mem_free)/mem_total*100.0) + "%"); 
		mem_total /= gb;
		
		
		// memory 사용량 : free -m				
		// 스토리지 여유량
		int storage_total=0, storage_free=0;
		try
		{
			File root = new File( data_folder );
			storage_total = (int)Math.round(root.getTotalSpace() / gb); // 우분투 = round
			storage_free = (int)Math.round(root.getUsableSpace() / gb);
//			System.out.println( "!! DeviceStatusInfo -Total  Space: " + storage_total + "MB" );
//			System.out.println( "!! DeviceStatusInfo - Usable Storage: " + storage_free + "MB" );
		}
		catch ( Exception e )
		{
			e.printStackTrace( );
		}
		
		String cpu_id=cpu_shellCmd();
		if(cpu_id.length() < 8)
			cpu_id = cpu_id + "    ";
		else if(cpu_id.length() > 8)
			cpu_id = cpu_id.substring(0, 8);
		if(cpu_id.equals("none    "))
			cpu_id = "none";
		
		int net_load = netload_shellCmd();
		
		if(mem_total > 99)
			mem_total = 99;
		if(storage_total > 9999)
			storage_total = 9999;
		if(storage_free > 9999)
			storage_free = 9999;
		if(process_load > 99)
			process_load = 99;
		if(mem_usage > 99)
			mem_usage = 99;

		System.out.print("\tdevice information : \n\t\tuuid=" + device_uuid + ",     cpu=" + cpu_id + ",     memory=" + mem_total + "[GB],     storage=" + storage_total);
		System.out.println("[GB],     process load=" + process_load + "[%],     memory usage=" + mem_usage + "[%],     network load=" + net_load + "[Mbps],     free storage=" + storage_free + "[GB]");
//		System.out.println("\tprocess load=" + new String(device_load) + "[%],     memory usage=" + new String(device_mem_usage) + "[%],     network load=" + device_net + "[Mbps],     free storage=" + device_free_storage + "[MB]");
	}
	public int DeviceInfomation(String ipAddress) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(null, ipAddress, "001");
		
		if(msgs.equals("none") || msgs.equals("time"))
			return devcnt;
		
		devcnt = 1;
		
		String uuid, device_cpu;
		int device_mem, device_storage, device_net, device_free_storage, device_load, device_mem_usage;
		
		uuid = msgs.substring(0,36);
		device_cpu =  msgs.substring(36,36+8);
		if(device_cpu.equals("none    "))
			device_cpu = "none";
		device_mem =  Integer.parseInt(msgs.substring(44,44+2));
		device_storage =  Integer.parseInt(msgs.substring(46,46+4));
//		reserved = msgs.substring(50,50+8);
		device_load =  Integer.parseInt(msgs.substring(58,58+2)); // .getBytes()
		device_mem_usage =  Integer.parseInt(msgs.substring(60,60+2)); // msgs.substring(60,60+2).getBytes(); //Integer.parseInt(msgs.substring(60,60+2));
		device_net =  Integer.parseInt(msgs.substring(62,62+2));
		device_free_storage =  Integer.parseInt(msgs.substring(64,64+4));

		System.out.print("\tdevice information : \n\t\tuuid=" + uuid + ",     cpu=" + device_cpu + ",     memory=" + device_mem + "[GB],     storage=" + device_storage);
		System.out.println("[GB],     process load=" + device_load + "[%],     memory usage=" + device_mem_usage + "[%],     network load=" + device_net + "[Mbps],     free storage=" + device_free_storage + "[GB]");
//		System.out.println("\tprocess load=" + new String(device_load) + "[%],     memory usage=" + new String(device_mem_usage) + "[%],     network load=" + device_net + "[Mbps],     free storage=" + device_free_storage + "[MB]");
		
		return devcnt;
	}
	
	public void WholeDataInfo()
	{
		ResultSet metadata = (ResultSet) database.query(select_sql + table_name);
		//int column = metadata.getMetaData().getColumnCount();
		try {
//			metadata.afterLast();
////			int row = metadata.getRow();
//			System.out.println("\tdevice uuid=" + device_uuid + "\n\tnumber of data=" +  metadata.getRow());
//			metadata.beforeFirst();
			
			int count = 0;
			String data = "";
			while (metadata.next()) {
				// 레코드의 칼럼은 배열과 달리 0부터 시작하지 않고 1부터 시작한다.
				// 데이터베이스에서 가져오는 데이터의 타입에 맞게 getString 또는 getInt 등을 호출한다.
				dataID = metadata.getString("dataid");
				securityLevel = metadata.getInt("security_level"); 
				dataSize = metadata.getLong("data_size");
				count ++;
				data += String.format("\t#%-4d", count) + String.format(" - DataID: %-70s", dataID) + String.format("\tDataSize[KB]: %-4d", dataSize) + "\tsecurityLevel: " + securityLevel +"\n";
			}
			System.out.println("\tdevice uuid: " + device_uuid + "\n\tnumber of data: " + count);
			System.out.println(data);
			
			
			metadata.close();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int WholeDataInfo(String ipAddress)
	{
		String msgs ="";
		int check = -1;

		msgs=RequestMessage("", ipAddress, "002");
//		System.out.println("!! MetaDataInfomation : " + msgs); // Send the answer 로 충분
		if(msgs.equals("time") || msgs.equals("none"))
			return check;
		
		check = 1;
//		System.out.println(msgs);
		String[] array = msgs.split("#");
		int number_data = Integer.parseInt(array[0].substring(36));	
//		System.out.println();
		System.out.println("\tdevice uuid=" + array[0].substring(0,36) + "\n\tnumber of data=" + number_data);
//				data +=  +  +  + "\tsecurityLevel: " + securityLevel +"\n";

		for(int i=0; i<number_data; i++) //check
			System.out.println(String.format("\t#%-4d", i+1) + String.format(" - DataID: %-70s", array[i*3+1]) + String.format("\tDataSize[KB]: %-4s", array[i*3+2]) + "\tsecurityLevel: " + array[(i+1)*3]);
//		System.out.println("\t#" + (i+1) + " - DataID: " + array[i*3+1] + "\tDataSize[KB]: " + array[i*3+2] + "\tsecurityLevel: " + array[(i+1)*3]);
		
		return check;
	}	
	public String MetaDataInfomation(String req_content) // 210428 add int func
	{
		String result = "none";	
//		String sql = select_sql + db_name + " where uuid = '" + message + "'";
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				dataID = metadata.getString("dataid");
				fileType = metadata.getString("file_type");
				if(req_content.equals(dataID))
				{
					timestamp = metadata.getTimestamp("timestamp");
					dataSignature = metadata.getString("data_signature");
					cert = metadata.getString("cert");
					directory = metadata.getString("directory");
					linked_edge = metadata.getString("linked_edge");
					dataType = metadata.getInt("data_type");
					securityLevel = metadata.getInt("security_level"); 
					dataPriority = metadata.getInt("data_priority");
					availabilityPolicy = metadata.getInt("availability_policy"); 
					dataSize = metadata.getLong("data_size");
					result = dataID + "#" + timestamp + "#" + fileType + "#" + dataType + "#" + securityLevel + "#" + dataPriority + "#" + availabilityPolicy + "#" + dataSignature + "#" + cert + "#" + directory + "#" + linked_edge + "#" + dataSize;
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return result;
	}
	public int MetaDataInfomation(String req_content, String ipAddress) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! MetaDataInfomation : " + msgs); // Send the answer 로 충분
		if(msgs.equals("time")) // System.out.println("* Bring the Information of Edge Device(" + device_ip + ") : Failure.");
			return devcnt;

		else if(!msgs.equals("none"))
		{
			devcnt = 1;
			System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//			Thread.sleep(5);
//			System.out.println("!! DataProcess : " + msgs);
			
			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			System.out.println("\tmetadata information :" 
					+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
					+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
					+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize[KB]: " + dataSize);
			
		}
		try {
			ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataid='" + dataID + "'");
			int check = 0;
			
			if(dataType == 0)
			{
				dataType = 1;
				linked_edge = ipAddress + ":" + directory;
				directory = null;
			}

			if(!metadata_list.next())
			{
//					check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
				int cnt = 0;
				check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
				while(check==0 && cnt++<3)
				{
					check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
				}

				if(check == 0)
					System.out.println("\tMetaData upload into DataBase : Failure");
				else
					System.out.println("\tMetaData upload into DataBase : Success");
			}
			metadata_list.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return devcnt;	
	}
	public String MetaDataInfomation(String req_content, String ipAddress, int check) // 210428 add int func
	{
		int devcnt = -1;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! MetaDataInfomation : " + msgs); // Send the answer 로 충분
		if(msgs.equals("time"))
			return msgs;

		if(check == 1 && !msgs.equals("none"))
		{
			devcnt = 1;
			//System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//				Thread.sleep(5);
//				System.out.println("!! DataProcess : " + msgs);
			
			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			try {
				ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataid='" + dataID + "'");
				
				if(dataType == 0)
				{
					dataType = 1;
					linked_edge = ipAddress + ":" + directory;
					directory = null;
				}

				if(!metadata_list.next())
				{
//						check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
					check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
					if(check == 0)
						System.out.println("\tMetaData upload into DataBase : Failure");
					else
						System.out.println("\tMetaData upload into DataBase : Success");
				}
				metadata_list.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return msgs;	
	}
	
	public int IndividualDataRead(String req_content) // local 검사
	{
		int devcnt = 0;
		String result = "none";	
//		String sql = select_sql + db_name + " where uuid = '" + message + "'";
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
//		System.out.println("!! slaves request dataID : " + req_content);
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				dataID = metadata.getString("dataid");
//				System.out.println("!! slaves request dataID : " + req_content);
				fileType = metadata.getString("file_type");
				if(req_content.equals(dataID))
				{
					devcnt = 1;
					timestamp = metadata.getTimestamp("timestamp");
					dataSignature = metadata.getString("data_signature");
					cert = metadata.getString("cert");
					directory = metadata.getString("directory");
					linked_edge = metadata.getString("linked_edge");
					
					dataType = metadata.getInt("data_type");
					securityLevel = metadata.getInt("security_level"); 
					dataPriority = metadata.getInt("data_priority");
					availabilityPolicy = metadata.getInt("availability_policy"); 
					dataSize = metadata.getLong("data_size");


					System.out.println("\tmetadata information :" 
							+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
							+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
							+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize[KB]: " + dataSize);
				}
			}
			else
			{
				return devcnt;
			}
			
    		//		String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if(dataType == 0 && devcnt==1 && (fileType.equals("csv") || fileType.equals("txt")))
    	{
			req_content = dataID + "." + fileType;
    		File f = new File(data_folder + req_content);
			if(f.exists()) {
				devcnt = 2;
				
				System.out.print("\n\tDATA [[\n\n\t");

				try {
					FileInputStream in = new FileInputStream(data_folder + req_content);
					BufferedInputStream bis = new BufferedInputStream(in);
					int len = 0;
					byte[] buf = new byte[4096];
					String lines ="";
					while ((len = bis.read(buf, 0, 4096)) != -1) {
//						System.out.println(new String(buf));
						lines += new String(buf, "UTF-8");
					}
					System.out.println(lines.replace("\n", "\n\t"));
					bis.close();
					in.close();
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    		
				System.out.println("\n\t]] DATA\n");
			}
    	}
    	else if(dataType == 0 && devcnt==1)
    	{
			req_content = dataID + "." + fileType;
    		File f = new File(data_folder + req_content);
			if(f.exists()) {
				devcnt = 2;
				
				System.out.println("\n\tDATA [[\n\t");
				System.out.println("\t FILE_TYPE is not text.");
				System.out.println("\t Confirm directly the file : " + data_folder + req_content);
				System.out.println("\n\t]] DATA\n");
			}
    	}
    	else
    	{
    		System.out.println("\n\tDATA [[\n\tSecurity Level is " + securityLevel + "\n\tConfirm directly the file : " + linked_edge + req_content + "\n\t]] DATA\n");
    	}

		return devcnt;
	}
	public int IndividualDataRead(String req_content, String ipAddress) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(msgs.equals("time"))
		{
			return -2;
//			return "time";
		}

		else if(!msgs.equals("none"))
		{
			devcnt = 1;

			String[] array = msgs.split("#");
			dataID = array[0];
			timestamp  = Timestamp.valueOf(array[1]);
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);
			dataPriority = Integer.parseInt(array[5]);
			availabilityPolicy = Integer.parseInt(array[6]);
			dataSignature = array[7];
			cert = array[8];
			if (dataType == 1)
			{
				directory = null;
				linked_edge = array[10];
			}
			else
			{
				directory = array[9];
				linked_edge = null;
			}
			dataSize = Long.parseLong(array[11]);
			System.out.println("\tmetadata information :" 
					+ "\n\tDataID: " + dataID + "\n\tTimeStamp: " + timestamp + "\n\tFileType: " + fileType + "\n\tDataType: " + dataType + "\n\tsecurityLevel: " + securityLevel
					+ "\n\tDataPriority: " + dataPriority + "\n\tAvailabilityPolicy: " + availabilityPolicy + "\n\tDataSignature: " + dataSignature + "\n\tCert: " + cert
					+ "\n\tDirectory: " + directory + "\n\tLinked_edge: " + linked_edge + "\n\tDataSize[KB]: " + dataSize);
			
		
			if(dataType == 1)
				return devcnt;
			
			req_content = dataID + "." + fileType;
			msgs = RequestMessage(req_content, ipAddress, 1);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3) //
			if(msgs.equals("time"))
			{
				return -2;
//				return "time";
			}
			if(!msgs.equals("none"))
			{
				devcnt = 2;

				if(!ipAddress.equals(local_ip))
				{
					ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataID='" + dataID + "'");
//					System.out.println("!! IndividualDataRead : " + select_sql + db_name + " where uuid='" + uuid + "';");
					if(securityLevel >= 4)
					{
						FileWriter fw = null; // 원본 공유 허용 데이터인 경우, 무조건 저장
						try {
							fw = new FileWriter(data_folder+req_content, false);
							fw.write(msgs);
							fw.flush();
							fw.close();
							dataType = 0;
							directory = data_folder;
							linked_edge = null;

							if(!metadata_list.next())
							{
//								check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
								check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							else
							{
								if(database.delete(dataID)) // 기존 메타데이터가 있으면 삭제하고 새로 업로드
//									check = database.update(req_content, uuid, security, datatype, data_folder); // metadata save
									check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							metadata_list.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}						
					}
					else // 링크데이터이면, db에 있는지만 확인하여 없으면 upload
					{
						try {
//							System.out.print("!! " + metadata_list.getString("file_name").equals(req_content));
//							System.out.print("!! " + metadata_list.next()); // 테스트용 - 여기서 한번 하면 if문에서 정상동작 안함
							if(dataType == 0)
							{
								dataType = 1;
								linked_edge = ipAddress + ":" + directory;
								directory = null;
							}

							if(!metadata_list.next())
							{
								check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
								if(check == 0)
									System.out.println("\tMetaData upload into DataBase : Failure");
								else
									System.out.println("\tMetaData upload into DataBase : Success");
							}
							metadata_list.close();
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
				}
			}

		}

		return devcnt;
	}


// unit edge thread
	public ArrayList<String> IndividualDataRead(String req_content, ArrayList<String> ipAddress) // 210428 add int func
	{
		File folder = new File(data_folder + "chunk"); //cert detail path
		if(!folder.exists())
			folder.mkdir();
		folder = new File(data_folder + "time"); //cert detail path
		if(!folder.exists())
			folder.mkdir();

		Scanner sc = new Scanner(System.in);
		int devcnt=-1, check=0, i=0, j=0;
		String msgs ="";
		ArrayList<String> edgeList = new ArrayList<String>();
		ArrayList<String> chunkList = new ArrayList<String>();
		int number_chunk=0, edge_size;
		int[] number_request;
		int chunk_size = 1; //KB
		double data_split_size = 4; //KB

		
		for(i=0; i<ipAddress.size(); i++)
		{
//			System.out.println("!! dataprocess - read : " + req_content);
			msgs=RequestMessage(req_content, ipAddress.get(i), "003");
//			System.out.println("!! dataprocess - read : " + msgs);
			if(msgs.equals("time"))
			{
//				return -2;
				System.out.println("* Bring the Information of Edge Device(" + ipAddress.get(i) + ") : Failure.");
				continue;
			}
			else if(!msgs.equals("none"))
			{
				String[] array = msgs.split("#");
				dataType = Integer.parseInt(array[3]);
				if(dataType == 0)
				{
					if(edgeList.size() == 0)
					{
						check ++;
						
						dataID = array[0];
						timestamp  = Timestamp.valueOf(array[1]);
						fileType = array[2];
						dataType = Integer.parseInt(array[3]);
						securityLevel = Integer.parseInt(array[4]);
						dataPriority = Integer.parseInt(array[5]);
						availabilityPolicy = Integer.parseInt(array[6]);
						dataSignature = array[7];
						cert = array[8];
						directory = array[9];
						linked_edge = null;
						dataSize = Long.parseLong(array[11]);
						if(securityLevel == 5)
						{
							number_chunk = (int)Math.ceil((double)dataSize / (double)chunk_size); // chunk size에 따른 chunk 갯수
							for(j=1; j<=number_chunk; j++)
								chunkList.add(req_content + "." + fileType + "_" + j);
//								System.out.println("!! individual data read : " + chunkList);
						}
					}
					edgeList.add(ipAddress.get(i));
					try { // 4-5th security level
						PrintWriter fprogress;
						fprogress = new PrintWriter(data_folder + dataID + ".meta");
						fprogress.println(dataType);  //linked or real data
						fprogress.println(dataSize);  //length
						fprogress.println(data_folder);  //data location
						if(securityLevel==5 && dataSize>4)
						{
							fprogress.println(dataSize);  //number of chunk
							fprogress.println(1);  //chunk size
						}
						else // no chunk
						{
							fprogress.println(1);  //number of chunk
							fprogress.println(dataSize);  //chunk size
						}
						fprogress.println(0);  //
//						fprogress.println(req_content+ "." + fileType);  //
						fprogress.close();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
				else // original data - another edge
				{
					try { // linked data sharing information
						PrintWriter fprogress;
						fprogress = new PrintWriter(data_folder + dataID + ".meta");
						fprogress.println(dataType);  //linked or real data
						//					        long file_length = fileLength(req_content, origin_ip); // 단위?
						fprogress.println(dataSize);  //length
						fprogress.println(linked_edge);  //data location
						fprogress.close();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					continue ;
				}
					
			}
		}
		
		edge_size = edgeList.size();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
		req_content = dataID + "." + fileType;
		if(check != 0)
		{
//			System.out.println("!! dataprocess - read : " + securityLevel + ", " + dataSize);
			if(securityLevel < 5 || (securityLevel==5 && dataSize<=data_split_size))
			{
//				System.out.println("!! dataprocess - read : " + securityLevel + ", " + dataSize);
//				System.out.println("!! dataprocess - read : " + req_content);
				String sha_result="none";
				Iterator<String> iter = edgeList.iterator();
				while(iter.hasNext())
				{
					String edge = iter.next();
//					File file = new File(data_folder+"time/"+req_content.replace(fileType, "txt")); // 1. check if the file exists or not boolean isExists = file.exists();
//					long start_time = System.currentTimeMillis(); // + 32400000;
//					try {
//						FileWriter fw = new FileWriter(file);
//						String str = format.format(new Date(start_time));
////							fw.write(req_ip+"\n");
////							fw.flush();
//						fw.write(str+"\n");
//						fw.flush();
//						fw.close();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					byte[] msgs_b = RequestMessageByte(req_content, edge, 1);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3) //
					try {
						msgs = new String(msgs_b, "UTF-8");
					} catch (UnsupportedEncodingException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
//					System.out.println("!! 004 - " + msgs);
					if(!msgs.equals("none"))
					{
						if(securityLevel >= 4)
						{
							try {
//add file - 1546line remove//	FileWriter fprogress = new FileWriter(data_folder + dataID + ".meta", true);
//								fprogress.write("1\n");  //number of trasfer edge 
//								fprogress.write(req_content + "\n");  //chunk name
//								fprogress.write(edge +", 1, 0\n");  //edge, start_chunk, end_chunk-(0=no chunk)
//								fprogress.write("1");  //received chunk
//								fprogress.close();
								PrintWriter fprogress; //
								fprogress = new PrintWriter(data_folder + dataID + ".meta");
								fprogress.println(dataType);  //linked or real data
								fprogress.println(dataSize);  //length
								fprogress.println(data_folder);  //data location
								fprogress.println(1);  //number of chunk
								fprogress.println(dataSize);  //chunk size
								fprogress.println("1");  //number of trasfer edge
								fprogress.println(req_content);  //chunk name
								fprogress.println(edge +", 1, 0");  //edge, start_chunk, end_chunk-(0=no chunk)
								fprogress.println("1");  //received chunk
								fprogress.close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
//							System.out.println("!! 004 - " +  securityLevel);
							try {
								File datafile = new File(data_folder+req_content);
								FileOutputStream fos = new FileOutputStream(datafile);
								fos.write(msgs_b, 0, msgs_b.length);
				                fos.flush();
				                fos.close();
//				                
								sha_result = RequestMessageKETIRead(req_content, edge, "444"); // chunk read request
//								System.out.println("!! 004 - sha result : " + sha_result);

//								FileWriter fw = new FileWriter(data_folder+req_content, false); // byte change need
//								fw.write(msgs);
//								fw.flush();
//								fw.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}					
							dataType = 0;
							directory = data_folder;
							linked_edge = null;

							if(fileType.equals("csv") || fileType.equals("txt"))
								System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
							else
								System.out.println("\n\tDATA [[\n\tFILE_TYPE is not text\n\tConfirm directly the file : " + data_folder + req_content + "\n\t]] DATA\n");
						}
						else // 링크데이터이면, db에 있는지만 확인하여 없으면 upload
						{
							dataType = 1;
							linked_edge = edge + ":" + directory;
							directory = null;
							try {
								PrintWriter fprogress;
								fprogress = new PrintWriter(data_folder + dataID + ".meta");
								fprogress.println(dataType);  //linked or real data
								//					        long file_length = fileLength(req_content, origin_ip); // 단위?
								fprogress.println(dataSize);  //length
								fprogress.println(linked_edge);  //length
								fprogress.close();
							} catch (FileNotFoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							
							if(fileType.equals("csv") || fileType.equals("txt"))
								System.out.println("\n\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
							else
								System.out.println("\n\tDATA [[\n\tFILE_TYPE is not text\n\tConfirm directly the file : " + linked_edge + req_content + "\n\t]] DATA\n");

// only notify //							System.out.println("\n\tDATA [[\n\tSecurity Level is " + securityLevel + "\n\tConfirm directly the file : " + linked_edge + req_content + "\n\t]] DATA\n");
						}

//						long end_time = System.currentTimeMillis(); // + 32400000;
//						try {
//							FileWriter fw = new FileWriter(file, true);
//							String str = format.format(new Date(end_time));
////								fw.write(req_ip+"\n");
////								fw.flush();
//							fw.write(str+"\n");
//							fw.flush();
//							str = Long.toString(end_time-start_time);
//							fw.write(str+"ms\n");
//							fw.flush();
//							fw.close();
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}


						database.delete(dataID);
						check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
						if(check == 0)
							System.out.println("\tMetaData upload into DataBase : Failure");
						else
							System.out.println("\tMetaData upload into DataBase : Success");

						break;
					}	
					else
					{
						System.out.println("* [" + edge + "] : have only MetaData.");
						iter.remove();
					}
				}

//				for(i=0; i<edgeList.size(); i++)
//				{
//				}
//					ResultSet metadata_list = (ResultSet) database.query(select_sql + table_name + " where dataID='" + dataID + "'");
				
			}
			else // securityLevel==5 && dataSize>4
			{
//					System.out.println("!! dataprocess - edgeList : " + edgeList);
				
//				Iterator<String> iter = edgeList.iterator();
//				//for(String edge : edgeList)
//				while(iter.hasNext())
//				{
//					String edge = iter.next();
//					String result = RequestMessage(req_content, edge, "400");  // chunk make request
//					if(result.equals("false"))
////							edgeList.remove(edge);
//						iter.remove();
////						else
////							System.out.println("dataprocess - chunk split : " + edge);
//				}
//				System.out.println("\tEdge List with Data Separation Completed : " + edgeList);
				// chunk request #0
				do
				{
					edge_size = edgeList.size();
					number_request = new int[edge_size];
					Arrays.fill(number_request, (int)(number_chunk/edge_size)); // working[number_chunk] = true;
					
					if(number_chunk % edge_size != 0)
					{
	//					number_request[i] += number_chunk % edge_size ; // 엣지 갯수로 데이터 조각이 나누어 떨어지지 않으면, 잔여조각만큼 마스터에 추가 요청
						for(i=0; i<number_chunk%edge_size; i++)
							number_request[i] ++; // 잔여 조각 갯수만큼 엣지들에게 추가 요청
					}
	
					Iterator<String> iter = edgeList.iterator();
					i = 0;
					j = 1;
					while(iter.hasNext())
					{
						String edge = iter.next();
						String result = RequestMessage(req_content, edge, "401", j, j+number_request[i] );  // chunk make request
						if(msgs.equals("time") || result.equals("false"))
							iter.remove();
						j += number_request[i];
						i++;
					}
					
					if(edgeList.size() == 0)
						return edgeList;
				}while(edge_size != edgeList.size());

//				FileWriter fprogress = null;
//				try {
//					FileWriter fprogress = new FileWriter(data_folder + dataID + ".meta", true);
//					fprogress.write(edgeList.size() + "\n");  // number of sharing edge
//					fprogress.write(req_content + "\n");  //chunk name
//					for(i=0, j=1; i<edge_size; i++)
//					{
//					}
//				} catch (FileNotFoundException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}				
				System.out.println("* Edge List with Data Separation Completed : " + edgeList);
				
				// chunk request #1
				UnitEdge[] edge_th = new UnitEdge[edge_size]; // thread
				chunk_check = new int[edge_size]; //thread check
				for(i=0, j=1; i<edge_size; i++)
				{
//						System.out.println("!! chunk read : " + i + ", " + j);
//						System.out.println("!! chunk read : " + chunkList.get(i));
		
// thread			
//					fprogress.print(edgeList.get(i) + " ");
//					fprogress.print(j + " " + (j+number_request[i]-1));
					chunk_check[i] = 0;
					// chunk request #2
					edge_th[i] = new UnitEdge(data_folder, req_content, edgeList.get(i), "405", fileType, i, j, j+number_request[i]); //
					edge_th[i].start();
					j += number_request[i];
				}
//				fprogress.close();
				
//				try { // for 공인인증
//					Thread.sleep(number_chunk);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} // for 공인인증
				
				boolean[] working = new boolean[number_chunk+1];
				Arrays.fill(working,true); // working[number_chunk] = true;
				int finisher = 0, edge_number=0;
				i = 0;  //순차적으로 다른 엣지에게 
				while(working[number_chunk])
				{
					//j=0; //요청했던 edge에게 다시 요청
					for(j=0; j<number_chunk; j++) // thread가 끝났는지 검사
					{
						if(new File(data_folder+"chunk/"+chunkList.get(j)).exists() && working[j])
						{
							finisher ++;
							working[j] = false;
						}
					}
					if(finisher == number_chunk)
						working[number_chunk] = false;
				}
				
//				try {
//					Thread.sleep(number_chunk);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}

				DataMerge(req_content, chunkList, chunk_size*1000);
				
				j=0;
				String chunk_result="none";
				while(chunk_result.equals("none"))
				{
//					System.out.print("!! sha test ");
					chunk_result = RequestMessageKETIRead(req_content, edgeList.get(j), "444"); // chunk read request
					j++;
					if((j+1)%edgeList.size() == 0)   //순차적으로 다른 엣지에게  
						j = 0;
				}

				dataType = 0;
				directory = data_folder;
				linked_edge = null;
				
				if(chunk_result.equals("success"))
				{
					database.delete(dataID);
					check = database.update(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize); // metadata save
					if(check == 0)
						System.out.println("* MetaData upload into DataBase : Failure");
					else
						System.out.println("* MetaData upload into DataBase : Success");
				}
			}

			
		}
		
		return edgeList;
	}
// unit edge thread

	public int IndividualDataWrite(String req_content, String input) // 210428 add int func
	{
		Scanner sc = new Scanner(System.in);
		int devcnt=0, check=0;
		String msgs ="";
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
//		System.out.println("!! slaves request dataID : " + req_content);
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				devcnt = 1;
				dataID = metadata.getString("dataid");
				fileType = metadata.getString("file_type");
				dataSize = metadata.getLong("data_size");
			}
			else
			{
				return devcnt;
			}
			
    		//		String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	if(securityLevel>1)
		{
			if(dataType != 1)// && (fileType.equals("csv") || fileType.equals("txt"))) // data delete
			{
				File file = new File(data_folder+req_content+"."+fileType);
//	            array[3] = array[3] + "";
	            try {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(input.getBytes("UTF-8"), 0, input.length());
		            fos.flush();
		            fos.close();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
//	            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
	            
//				boolean check2 = database.delete(dataID); 
				long fileSize = (long)Math.ceil((double)file.length() / 1000.0);
				String update_sql = "update " + table_name + " set data_size = " + fileSize +" where dataid = '" + req_content + "'";
//				"update file_management set security_level=1 where dataID='ggfe0bd2ee16937d772864dc12057eab83df6c23b58a699354c529d5b4730d77';"
//				System.out.println("!! slaves request dataID : " + req_content);
				check = database.update(update_sql);
				
				devcnt = 2;
			}
//			else if(dataType != 1)
//				devcnt = 0;
//			else // link data -> delete only metadata in db
//			{
//			}
		}
    	else
    		devcnt = 1;
		
		return devcnt;
	}
	public int IndividualDataWrite(String req_content, String ipAddress, String input) // 210428 add int func
	{
		int devcnt=0, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
		if(msgs.equals("time"))
			return -1;
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(!msgs.equals("none"))
		{
			devcnt = 0;

			String[] array = msgs.split("#");
			dataID = array[0];
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);

			req_content = dataID + "." + fileType + "::" + input;
			
			if(securityLevel>1 && dataType != 1)// && (fileType.equals("csv") || fileType.equals("txt")))
			{
//				System.out.println("!! 005 - 1");
				msgs = RequestMessage(req_content, ipAddress, 2);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3)
				if(msgs.equals("time"))
					return -1;
//				System.out.println("\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
//				if(!msgs.equals(null))
//					devcnt = 2;
				if(msgs.equals("permission"))
				{
					devcnt = 2;
//					System.out.println("\tIt is Possible to Remove the DATA\n");
				}
				
//				if(dataType != 1) // data delete - don't care
			}
//			else if(securityLevel>1 && dataType != 1)
//	    		devcnt = 0;
	    	else
	    		devcnt = 1;
			System.out.println("!! 005 - " + devcnt);

		}
		return devcnt;
	}

	public int IndividualDataRemove(String req_content) // 210428 add int func
	{
		int devcnt=0;
		String sql = select_sql + table_name + " where dataid = '" + req_content + "'";
//		System.out.println("!! slaves request dataID : " + req_content);
		ResultSet metadata = (ResultSet) database.query(sql);
    	try {
			if(metadata.next()) // metadata가 있으면 true, 없으면 false
			{
				devcnt = 1;
				dataID = metadata.getString("dataid");
//				System.out.println("!! slaves request dataID : " + req_content);
				fileType = metadata.getString("file_type");
				dataType = metadata.getInt("data_type");
				securityLevel = metadata.getInt("security_level"); 
			}
			else
			{
				return devcnt;
			}
			
    		//		String result = "{[{ANS::" + pkt.getAddress().getHostAddress() + "::003::" + uuid +  String.format("%03d", d_length) + location + String.format("%04d", dataSize) + security + sharing + "}]}";	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	if(securityLevel>2)
		{
			if(dataType != 1) // data delete
			{
				File file = new File(data_folder+req_content+"."+fileType);
				if( file.exists() )
					file.delete();
			}
//			else // link data -> delete only metadata in db
//			{
//			}
			boolean check2 = database.delete(dataID); 
			devcnt = 2;
		}
		
		return devcnt;
	}
	public int IndividualDataRemove(String req_content, String ipAddress) // 210428 add int func
	{
		int devcnt=0, check=0;
		String msgs ="";

		msgs=RequestMessage(req_content, ipAddress, "003");
		if(msgs.equals("time"))
			return -1;
//		System.out.println("!! IndividualDataRead : " + msgs);
		if(!msgs.equals("none"))
		{
			devcnt = 1;

			String[] array = msgs.split("#");
			dataID = array[0];
			fileType = array[2];
			dataType = Integer.parseInt(array[3]);
			securityLevel = Integer.parseInt(array[4]);

			req_content = dataID + "." + fileType;
			
			if(securityLevel>2)
			{
				msgs = RequestMessage(req_content, ipAddress, 3);// divide는 datasize로 판별 //data_code = read(1), write(2), remove(3)
				if(msgs.equals("time"))
					return -1;
//				System.out.println("\tDATA [[\n\t" + msgs.replace("\n", "\n\t") + "\n\t]] DATA\n");
//				if(!msgs.equals(null))
//					devcnt = 2;
				if(msgs.equals("permission"))
				{
					devcnt = 2;
//					System.out.println("\tIt is Possible to Remove the DATA\n");
				}
				
//				if(dataType != 1) // data delete - don't care
			}
		}
//		System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//		Thread.sleep(5);

		return devcnt;
	}
	
	byte[] IndividualDataSendReadByte(String filename)
	{
		int len = 0, total_len=0;
		byte[] buffer = new byte[1024];
		byte[] result = "none".getBytes();
		
		File file = new File(filename);
		if(!file.exists())
		{
			return result;
		}
		
		try {
			FileInputStream in = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(in);
			int cnt = 0;
			byte[] buf = new byte[1024];
			byte[] msg=null;
			byte[] msg_b=null;
			while ((len = bis.read(buf, 0, 1024)) != -1) {
				total_len += len;
				msg = new byte[total_len];

				if(cnt == 0)
				{
					System.arraycopy(buf, 0, msg, 0, len);
					msg_b =  msg;
				}
				else
				{
					System.arraycopy(msg_b, 0, msg, 0, msg_b.length);
					System.arraycopy(buf, 0, msg, msg_b.length, len);
					
					msg_b =  msg;
				}
				
				cnt ++;
			}
			bis.close();
			in.close();
			
			result = msg;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
		return result;
	}
	public int IndividualDataTransfer(String req_content, String ipAddress, String meta_info) // 210428 add int func
	{
	    int devcnt=0;
		String check_s="none";
		byte[] check=null; 
		String remote_cmd = "{[{REQ::" + ipAddress + "::007::"; //+ meta_info + "::";

		if(!meta_info.equals("none"))
		{
			String[] array = meta_info.split("#");
			req_content = dataID + "." + fileType;
			String data_file = dataID + "." + fileType;
			String cert_file = cert;
			
			EdgeDeviceInfoClient client = new EdgeDeviceInfoClient(ipAddress, EdgeDeviceInfoClient.socketTCP, ketiCommPort);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("IndividualDataTransfer : " + client.streamSocket_alive());
				return 0;
			}
			client.startWaitingResponse();
			File file = new File(cert_file);
			if(file.exists())
			{
//				devcnt = 1;
				int filesize = (int)file.length(), whole, chunksize=1000, i;
				
//				filesize = (int)file.length();
//				System.out.println("!! IndividualDataSend : " + filesize);
//				System.out.println("!! IndividualDataSend : " + data_file);
//				check = cert_file; //IndividualDataSendRead(cert_file);
				check = IndividualDataSendReadByte(cert_file);
				try {
					check_s = new String(check, "UTF-8");
				} catch (UnsupportedEncodingException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				if(!check_s.equals("none"))
				{
					String[] cert_info = cert_file.split(folder);
//					System.out.println("!! IndividualDataSend : " + cert_info[cert_info.length-2]);
//					System.out.println("!! IndividualDataSend : " + cert_info[cert_info.length-1]);
					String data = remote_cmd + "cert::" + cert_info[cert_info.length-2] + "::" + cert_info[cert_info.length-1] + "::" + filesize + "::";
//					System.out.println("!! IndividualDataSend : " + data);
					
					byte[] msg_b=null, msg_l=null, cert_cmd=null;
					try {
						msg_b = data.getBytes("UTF-8");
						msg_l =  "}]}".getBytes("UTF-8");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					int length = msg_b.length + check.length + msg_l.length;
					cert_cmd = new byte[length];
					
					System.arraycopy(msg_b, 0, cert_cmd, 0, msg_b.length);
					System.arraycopy(check, 0, cert_cmd, msg_b.length, check.length);
					System.arraycopy(msg_l, 0, cert_cmd, msg_b.length+check.length, msg_l.length);
					client.sendPacket(cert_cmd, cert_cmd.length);
				}
			}
			else
			{
//				result = "Fail";
				return devcnt;
			}
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			client.stopRequest();
			
//			System.out.println("!! IndividualDataSend meta : " );

			client = new EdgeDeviceInfoClient(ipAddress, EdgeDeviceInfoClient.socketTCP, ketiCommPort);
			if(!client.streamSocket_alive())
			{
				Logger logger = Logger.getLogger("MyLog");
			    FileHandler fh;
			    try {
			        // This block configure the logger with handler and formatter  
			        fh = new FileHandler("log/log");
			        logger.addHandler(fh);
			        SimpleFormatter formatter = new SimpleFormatter();
			        fh.setFormatter(formatter);
			    } catch (SecurityException e) {
			        e.printStackTrace();
			    } catch (IOException e) {
			        e.printStackTrace();
			    }
				logger.info("IndividualDataTransfer : " + client.streamSocket_alive());
				return 0;
			}
			client.startWaitingResponse();
			String meta_cmd = remote_cmd + "meta::" + meta_info + "}]}";
			client.answerData = null;
			try {
				client.sendPacket(meta_cmd.getBytes("UTF-8"), meta_cmd.length());
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			long start_client = System.currentTimeMillis();
			while(client.answerData == null)
			{			
				try {
					Thread.sleep(50);
					if(System.currentTimeMillis() - start_client > check_timeout )
					{
//						System.out.println("\t!! Response Time is delayed over " + check_timeout + "ms");
						System.out.println("\t!! Response Time is delayed over : " + remote_cmd);
						break;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if(client.answerData != null)
			{
				String answer_data = null;
				try {
					answer_data = new String(client.answerData, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				System.out.println("!! IndividualDataSend receive : " + answer_data);
				if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
				{
					devcnt = 2;
					String[] split = answer_data.substring(8, answer_data.indexOf("}]}")).split("::"); // [0] = my_ip, [1]=003, [2]=metadata or none
					client.answerData = null;
//					System.out.println("!! IndividualDataSend result : " + result);
				}
				
			}
			else
				return -1;

			client.stopRequest();
		}
//		System.out.println("\tAnswer : " + ipAddress + " has MetaData.");
//		Thread.sleep(5);

		return devcnt;
	}

	public static void DataMerge(String newFileName, ArrayList<String> filenames, int chunk_size)
	{
		FileOutputStream fout = null;
		FileInputStream in = null;
		BufferedInputStream bis = null;
		try {
			File newFile = new File(data_folder + newFileName);
//			System.out.println("!! data mergy : " + data_folder+newFileName);
//			System.out.println("!! data mergy : " + filenames);
			fout = new FileOutputStream(newFile);

			for (String fileName : filenames) {
//				System.out.println(fileName);
				File splittedFile = new File(data_folder + "chunk/" + fileName);
				while(!splittedFile.exists())
				{
					Thread.sleep(10);
				}
				if(splittedFile.exists())
				{
					in = new FileInputStream(splittedFile);
					bis = new BufferedInputStream(in);
					int len = 0;
					byte[] buf = new byte[chunk_size];
					while ((len = bis.read(buf, 0, chunk_size)) != -1) {
//						System.out.println(new String(buf));
						fout.write(buf, 0, len);
						fout.flush();
					}
//					splittedFile.delete();
					bis.close();
					in.close();
				}
			}
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String sha(String filepath) throws Exception
	{
		File file = new File(data_folder+filepath);
        if(!file.exists())
        {
        	Thread.sleep(100);
        }
        if(!file.exists())
        {
        	return "";
        }
        	
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = new FileInputStream(data_folder+filepath);
        
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
	public static String chunk_sha(String filepath) throws Exception
	{
		File file = new File(data_folder+filepath);
        for(int i=0; i<5; i++) // file creating time wait 500ms
        {
            if(!file.exists())
            {
            	Thread.sleep(100);
            }
        }
        if(!file.exists())
        {
        	return "";
        }
        	
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = new FileInputStream(data_folder+filepath);
        
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
	
	private static String finish_cmd="finish";
	public ResultSet rs = null;
/*	
	public Connection conn = null;
	public Statement state = null; 
	public PreparedStatement pstate = null;
	// rs.absolute(low_index);
	public String url = "jdbc:mysql://localhost:3306/fileManagement_DB?serverTimezone=UTC";
*/	
//	public String uuid_file="", uuid="", location="", origin_ip="", descriptor="";
//	public int security=-1, datatype=-1, d_length=-1, datasize=-1;
	static String dataID, fileType, dataSignature, cert, directory, linked_edge;	
	static Timestamp timestamp;
	static int dataType, securityLevel, dataPriority, availabilityPolicy, partSize;
	static long dataSize;

	static String select_sql = "SELECT * FROM "; //file_management"; //(file_name, uuid, security, sharing, location)
	static String table_name = "file_management"; //(file_name, uuid, security, sharing, location)
	static String device_uuid = "f1d6fc0c-1c51-11ec-a6c1-b75b198d62ab"; //(file_name, uuid, security, sharing, location)
//	public static String origin_foldername = "/home/keti/data/";
	public static String data_folder = "/home/keti/data/";
	public static String cert_folder = "/home/keti/data/";
	public static String folder = "/";
	public static String local_ip = "localhost";
	public static Database database = null;
	static int ketiCommPort = 5679; // KETI 내부통신
	public int[] chunk_check, chunk_edge;
//	public String[] chunk;
	public static int maxRetry = 10;
	static int check_timeout = 5000;

	
	class UnitChunk extends Thread
	{
//		public static String foldername="/data/";
		public String req_content, req_ip, req_code, file_type;
		int th_id;
		
		UnitChunk(String fd, String content, String ip, String code, String type, int id)
		{
			data_folder = fd;
			req_content = content;
			req_ip = ip;
			req_code = code;
			file_type = type;
			th_id = id;
		}
		
		public void run() // 동기화 synchronized - 소용없음
		{
//			synchronized (this) // 지역 동기화 - 소용없음
			while(chunk_check[th_id] == 0)
			{ 
		        EdgeDeviceInfoClient client = null;
				String remote_cmd="";
		
				try {
					// for 공인인증 - 데이터 공유 시작/종료 시간 파일 작성
					for(int i=0; i<maxRetry; i++)
					{
						try
						{
							client = new EdgeDeviceInfoClient(req_ip, EdgeDeviceInfoClient.socketTCP);
							if(!client.streamSocket_alive())
							{
								Logger logger = Logger.getLogger("MyLog");
							    FileHandler fh;
							    try {
							        // This block configure the logger with handler and formatter  
							        fh = new FileHandler("log/log");
							        logger.addHandler(fh);
							        SimpleFormatter formatter = new SimpleFormatter();
							        fh.setFormatter(formatter);
							    } catch (SecurityException e) {
							        e.printStackTrace();
							    } catch (IOException e) {
							        e.printStackTrace();
							    }
								logger.info("UnitChunk : " + client.streamSocket_alive());
								return ;
							}
							break;
						} catch (Exception e)
						{	
							if(i == maxRetry-1)
							{
//								System.out.println("!! error : client create : " + th_id+1);
								client.stopWaitingResponse();
								chunk_check[th_id] = 1;
								return ;								
							}
						}
					}
					client.startWaitingResponse();
					int work_cnt = 0;
					
					remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "}]}";
//					System.out.println("RequestMessage - chunk : " + remote_cmd); //datasize
					client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
					long start_client = System.currentTimeMillis();
					while(client.answerData == null)
					{			
						if(chunk_check[th_id] != 0) // 수신 대기 중, 다른 thread에서 수신완료한 경우
						{
//							System.out.println("!! error : client not need : " + th_id+1);
							client.stopWaitingResponse();
							return ;
						}
						else
						{
							try {
								Thread.sleep(50);
//								if(System.currentTimeMillis() - start_client > check_timeout ) //chunk except
//								{
////									result = "time";
//									break;
//								}
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}			
					if(client.answerData != null)
					{
						String answer_data = new String(client.answerData, "UTF-8");
						if(answer_data.equals("retry"))
						{
							System.out.println("error : client retry : " + th_id+1);
							client.answerData = null;
							client.stopWaitingResponse();
							chunk_check[th_id] = 1;
							return ;								
						}
						
						if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
						{
//							System.out.println("!! RequestMessageRead : " + answer); //
							String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
							if(!array[1].equals(req_code))
							{
								System.out.println("error : client retry : " + th_id+1);
								client.answerData = null;
								client.stopWaitingResponse();
								chunk_check[th_id] = 1;
								return ;								
							}
							client.answerData = null;
//							System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//							System.out.println("!! RequestMessageRead : " + array[3]); //datasize
							String[] chunk_array = req_content.split("_");
							if(!array[2].equals(chunk_array[1])) // chunk 프로토콜 규약
							{
//								System.out.println("!! chunk request : " + chunk_array[1] + ", but receive : " + array[4]);
								chunk_check[th_id] = 1;
//								chunk_edge[th_id] = chunk_edge[Integer.parseInt(array[4])-1]; //대신 저장한 chunk
								th_id = Integer.parseInt(array[2]) - 1; // chunk 프로토콜 규약
//					            chunk[th_id] = array[3];
								req_content = chunk_array[0] + "_" + array[2]; // 데이터 thread가 엉키는건가? 엉뚱한 thread에서 수신  // chunk 프로토콜 규약
							}
				            
//							if(req_content.equals("195240aa-92e4-40fd-8014-12033767f351.csv_307"))
//							{
//								System.out.println("!! RequestMessageRead : " + answer); //datasize
//								System.exit(0);
//							}
							File file = new File(data_folder+"chunk/"+req_content);
				            FileOutputStream fos = new FileOutputStream(file);
//				            array[3] = array[3] + "";
				            fos.write(array[4].getBytes("UTF-8"), 0, Integer.parseInt(array[3])); //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
//				            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
				            fos.flush();
				            fos.close();
				            
//							try { // for 공인인증 - interrupted error
//								Thread.sleep(100);
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							chunk_check} // for 공인인증

						}
						else
							chunk_check[th_id] = 1;
						
					}

					
			//			System.out.println("!! RequestMessage : " + result);
					client.stopRequest();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				chunk_check[th_id] = 2;
			}
		}
		// UnitChunk Thread
	}
	
	// UnitEdge Thread
	class UnitEdge extends Thread
	{
		public String req_content, req_ip, req_code, file_type;
		int index_s, index_f, th_id;
		
		UnitEdge(String fd, String content, String ip, String code, String type, int id, int start, int finish)
		{
			data_folder = fd;
			req_content = content;
			req_ip = ip;
			req_code = code;
			file_type = type;
			th_id = id;
			index_s = start;
			index_f = finish;
		}
		
		public void run() // 동기화 synchronized - 소용없음
		{
			// chunk request #2 - 405
//			while(chunk_check[th_id] == 0)
//			{ 
		        EdgeDeviceInfoClient client = null;
				String remote_cmd="";
		
				try {
					// for 공인인증 - 데이터 공유 시작/종료 시간 파일 작성
					for(int i=0; i<maxRetry; i++)
					{
						try
						{
							client = new EdgeDeviceInfoClient(req_ip, EdgeDeviceInfoClient.socketTCP);
							if(!client.streamSocket_alive())
							{
								Logger logger = Logger.getLogger("MyLog");
							    FileHandler fh;
							    try {
							        // This block configure the logger with handler and formatter  
							        fh = new FileHandler("log/log");
							        logger.addHandler(fh);
							        SimpleFormatter formatter = new SimpleFormatter();
							        fh.setFormatter(formatter);
							    } catch (SecurityException e) {
							        e.printStackTrace();
							    } catch (IOException e) {
							        e.printStackTrace();
							    }
								logger.info("UnitEdge : " + client.streamSocket_alive());
								return ;
							}
							break;
						} catch (Exception e)
						{	
							if(i == maxRetry-1)
							{
//								System.out.println("!! error : client create : " + th_id+1);
								client.stopWaitingResponse();
								chunk_check[th_id] = -1;
								return ;								
							}
						}
					}
					client.startWaitingResponse();
					int work_cnt = 0;
					
					remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "::" + index_s + "::" + index_f + "}]}";
					client.answerData = null;
					System.out.println("Request to : " + req_ip + ", chunk #"+ index_s + " to #" + (index_f-1)); //datasize
//					System.out.println("RequestMessage - chunk : " + remote_cmd); //datasize
					client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
					
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
					long start_time = System.currentTimeMillis(); // + 32400000;
					for(int i=index_s; i<index_f; i++)
					{
						try { // 요청 시간 작성
							File file = new File(data_folder+"time/request_"+req_content.replace(fileType, "txt")+"_"+Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
//							if(!file.exists()) // 데이터를 2번 요청 할 수도 있음 - sha 결과가 달라서
//							{
//							}
							FileWriter fw = new FileWriter(file);
							String str = format.format(new Date(start_time));
//								fw.write(req_ip+"\n");
//								fw.flush();
							fw.write(str+"\n");
							fw.flush();
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					// chunk request #7 - sha 수신
					boolean[] check_file = new boolean[index_f-index_s];
					Arrays.fill(check_file,true); // working[number_chunk] = true;
					do
					{
						long start_client = System.currentTimeMillis();
						while(client.answerData == null)
						{			
							if(chunk_check[th_id] >= index_f-index_s) // 수신 대기 중, 다른 thread에서 수신완료한 경우
							{
//								System.out.println("!! error : client not need : " + th_id+1);
								client.stopWaitingResponse();
								return ;
							}
							else
							{
								try {
									Thread.sleep(50);
//									if(System.currentTimeMillis() - start_client > check_timeout )  //chunk except
//									{
//										break;
//									}
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}			
						if(client.answerData != null)
						{
							String answer_data = new String(client.answerData, "UTF-8");
							if(answer_data.indexOf("{[{ANS")==0 && answer_data.indexOf("}]}")!=-1) // 기본 양식 맞음
							{
//								System.out.println("!! RequestMessageRead : " + answer); //
								String[] array = answer_data.substring(8, answer_data.indexOf("}]}")).split("::");
								client.answerData = null;
								if(!array[1].equals(req_code))
								{
									continue;
								}
//								System.out.println("!! RequestMessageRead : " + array[2]); //datasize
//								System.out.println("!! RequestMessageRead : " + array[3]); //datasize
								
								if(array[2].equals("sha")) //sha 검사하기
								{
									Thread.sleep(100);
									for(int i=index_s; i<index_f; i++)
						            {
						            	if(check_file[i-index_s])
						            	{
//											System.out.println("* Original Data SHA code -\t" + array[3+i-index_s]);
											long end_time = System.currentTimeMillis(); // + 32400000;
											String str = chunk_sha("chunk/" + req_content + "_" + Integer.toString(i));
//											System.out.println("* Recieved Data SHA code -\t" + str);
											if(array[3+i-index_s].equals(str))
											{
												File file = new File(data_folder+"time/request_"+req_content.replace(fileType, "txt") + "_" + Integer.toString(i)); // 1. check if the file exists or not boolean isExists = file.exists();
												// 수신 완료 시간 작성

												try {
													FileWriter fw = new FileWriter(file, true);
//														fw.write(req_ip+"\n");
//														fw.flush();
													fw.write(format.format(new Date(end_time))+"\n");
													fw.flush();
													fw.write(str+"\n"); //SHA 기록
													fw.flush();
													fw.close();
												} catch (IOException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
									            check_file[i-index_s] = false;
									            chunk_check[th_id] ++;
											}
											else
											{ // SHA가 다르면 재요청 - 
//												System.out.println("\tRecieved Chunk #" + Integer.toString(i) + " SHA code isn't the same as original : retry");
//												File file = new File(data_folder+"chunk/"+req_content + "_" + Integer.toString(i));
//												file.delete();
												client.answerData = null;
												remote_cmd = "{[{REQ::" + req_ip + "::" + req_code + "::" + req_content + "::" + Integer.toString(i) + "::" + Integer.toString(i+1) + "}]}";
												client.sendPacket(remote_cmd.getBytes("UTF-8"), remote_cmd.length());
												Thread.sleep(100); // test 필요

												i --;
												continue;
											}
						            		
						            	}
						            }
						            break;
								}
//								else
//								{
//									String chunk_content = req_content + "_" + array[2]; // chunk 프로토콜 규약
//									System.out.println("!! RequestMessageRead : " + chunk_content); //
//									File file = new File(data_folder+"chunk/"+chunk_content);
//						            FileOutputStream fos = new FileOutputStream(file);
////						            array[3] = array[3] + "";
//						            fos.write(array[4].getBytes("UTF-8"), 0, Integer.parseInt(array[3])); //Integer.parseInt(array[2]) chunk크기 // chunk 프로토콜 규약
////						            fos.write(Base64.getDecoder().decode(array[3]), 0, Integer.parseInt(array[2])); //Integer.parseInt(array[2]) chunk크기
//						            fos.flush();
//						            fos.close();
//								}
							}
						}
						
						
					}while(true);
//					System.out.println("!! RequestMessageRead exit : " + th_id); //datasize
					
					client.stopRequest();
//					System.out.println("!! RequestMessageRead : " + chunk_check[th_id]); //
//				} catch (UnknownHostException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//		}
		
	} // UnitEdge Thread
}




