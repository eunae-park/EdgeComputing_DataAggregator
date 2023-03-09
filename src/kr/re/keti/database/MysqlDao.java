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
	}
}
