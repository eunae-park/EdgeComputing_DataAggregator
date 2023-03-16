package kr.re.keti.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MysqlDao implements Database {
	private String url;
	private String id;
	private String pw;

	public MysqlDao(String databaseName, String id, String pw) {
		this.url = "jdbc:mysql://localhost:3306/" + databaseName + "?serverTimezone=UTC&autoReconnect=true";
		this.id = id;
		this.pw = pw;
	}

	@Override
	public Connection getConnection() {
		Connection connection = null;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connection = DriverManager.getConnection(url, id, pw);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return connection;
	}

	@Override
	public boolean exists(String table, String pk) {
		boolean result = false;

		if(table.equals("file_management")) {
			result = existsFileManagement(pk);
		}
		else if(table.equals("file_uuid")) {
			result = existsFileUuid(pk);
		}
		else {
			System.out.println("*** exists Invalid table name [" + table + "] ***");
		}
		return result;
	}

	@Override
	public boolean insert(Object objectDto) {
		boolean result = false;
		if(objectDto instanceof FileManagementDto) {
			FileManagementDto dto = (FileManagementDto) objectDto;
			result = insertFileManagement(dto);
		}
		else if(objectDto instanceof FileUuidDto) {
			FileUuidDto dto = (FileUuidDto) objectDto;
			result = insertFileUuid(dto);
		}
		else {
			System.out.println("***  insert an unsupported DTO type into the database. ***");
		}
		return result;
	}

	@Override
	public ArrayList<Object> select(String table) {
		ArrayList<Object> dtos = new ArrayList<>();

		if(table.equals("file_management")) {
			dtos.addAll(selectFileManagement());
		}
		else if(table.equals("file_uuid")) {
			dtos.addAll(selectFileUuid());
		}
		else {
			System.out.println("*** select Invalid table name [" + table + "] ***");
		}

		return dtos;
	}
}
