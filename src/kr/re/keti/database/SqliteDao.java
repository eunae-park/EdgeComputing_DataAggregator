package kr.re.keti.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class SqliteDao implements Database {
	private String path;

	public SqliteDao(String path, String databaseName) {
		this.path = "jdbc:sqlite:" + path + databaseName + ".db";
	}

	@Override
	public Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

}
