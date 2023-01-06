package kr.re.keti.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

public class Database {
	String url;
	String tableName;
	String uid;
	String pwd;
	
	public Database(String tableName, String uid, String pwd) {
		this.tableName = tableName;
		this.url = "jdbc:mysql://localhost:3306/" + "mecTrace" + "?serverTimezone=UTC&autoReconnect=true";
		this.uid = uid;
		this.pwd = pwd;
	}
	
	public Connection getConnection() {
		Connection connection = null;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(url, uid, pwd); // DB_URL, USER_NAME, // PASSWARD
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // com.mysql.jdbc.Driver
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return connection;
	}
	public int delete(String dataid) {
		String query = "delete from "+tableName+" where dataid='"+dataid+"';";
		try ( 	Connection connection = getConnection();
				Statement statement = connection.createStatement();
		){
			return statement.executeUpdate(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	public void createFile(String uuid, String fileName, String linkedEdge, int security_level, String sign) {
		SimpleDateFormat log_format = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS"); //hh = 12시간, kk=24시간
		String dataID = fileName.substring(0, fileName.indexOf("."));
		String fileType = fileName.substring(fileName.indexOf(".")+1, fileName.length());
		int dataType = (security_level > 3)? 0 : 1; 
		int securityLevel = security_level;
		int dataPriority = 0;
		int availabilityPolicy = 1;
		String dataSignature = sign;
		String cert = "/home/keti/cert/Vehicle/"+uuid+".crt";
		String directory = "/home/keti/data/";
		String linked_edge = linkedEdge;
		long dataSize = 0;
		try {
			dataSize = (Files.size(Paths.get(directory+dataID+"."+fileType)))/1024;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		Timestamp timestamp = Timestamp.valueOf(LocalDateTime.now());
		int check = insert(dataID, timestamp, fileType, dataType, securityLevel, dataPriority, availabilityPolicy, dataSignature, cert, directory, linked_edge, dataSize);
	}
	public String select(String dataid) {
		String query = "select * from "+tableName+" where dataid='"+dataid+"';";
		try (	Connection connection = getConnection();
				PreparedStatement pstmt = connection.prepareStatement(query);
				ResultSet rs = pstmt.executeQuery();
		){
			if(rs.next()) {

				String dataID = rs.getString("dataid");
				String fileType = rs.getString("file_type");
				Timestamp timestamp = rs.getTimestamp("timestamp");
				String dataSignature = rs.getString("data_signature");
				String cert = rs.getString("cert");
				String directory = rs.getString("directory");
				String linked_edge = rs.getString("linked_edge");
				int dataType = rs.getInt("data_type");
				int securityLevel = rs.getInt("security_level"); 
				int dataPriority = rs.getInt("data_priority");
				int availabilityPolicy = rs.getInt("availability_policy"); 
				long dataSize = rs.getLong("data_size");
				String result = dataID + "#" + timestamp + "#" + fileType + "#" + dataType + "#" + securityLevel + "#" + dataPriority + "#" + availabilityPolicy + "#" + dataSignature + "#" + cert + "#" + directory + "#" + linked_edge + "#" + dataSize;
				return result;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "none";
	}
	public int insert(String dataID, Timestamp timestamp, String fileType, int dataType, int securityLevel, int dataPriority, int availabilityPolicy, String dataSignature, String cert, String directory, String linked_edge, long dataSize)
	{
		String insert_sql = "insert into " + tableName
				+ " (dataid, availability_policy, cert, data_priority, data_signature, data_size, data_type, directory, file_type, linked_edge, security_level, timestamp)"
				+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (	Connection connection = getConnection();
				PreparedStatement pstmt = connection.prepareStatement(insert_sql);)
		{	
			pstmt.setString(1, dataID);
			pstmt.setInt(2, availabilityPolicy);
			pstmt.setString(3, cert);
			pstmt.setInt(4, dataPriority);
			pstmt.setString(5, dataSignature);
			pstmt.setLong(6, dataSize);
			pstmt.setInt(7, dataType);
			pstmt.setString(8, directory);
			pstmt.setString(9, fileType);
			pstmt.setString(10, linked_edge);
			pstmt.setInt(11, securityLevel);
			pstmt.setTimestamp(12, timestamp);
			return pstmt.executeUpdate();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	}
	public int getDataType(String dataid) {
		String meta = select(dataid);
		return Integer.parseInt(meta.split("#")[3]);
	}
	public String getSecurityLevel(String dataid) {
		String meta = select(dataid);
		return meta.split("#")[4];
	}
	public String getLinkedEdge(String dataid) {
		String meta = select(dataid);
		return meta.split("#")[10];
	}
	public String getSign(String dataid) {
		String meta = select(dataid);
		return meta.split("#")[7];
	}
	public String getCert(String dataid) {
		String meta = select(dataid);
		return meta.split("#")[8];
	}
	
	
}
