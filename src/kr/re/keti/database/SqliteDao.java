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

	@Override
	public Object select(String table, String pk) {
		Object dto = null;
		if(table.equals("file_management")) {
			dto = selectFileManagement(pk);
		}
		else if(table.equals("file_uuid")) {
			dto = selectFileUuid(pk);
		}
		else {
			System.out.println("*** select Invalid table name [" + table + "] ***");
		}
		return dto;
	}

	@Override
	public boolean update(Object objectDto) {
		boolean result = false;
		if(objectDto instanceof FileManagementDto) {
			result = updateFileManagement((FileManagementDto) objectDto);
		}
		else if(objectDto instanceof FileUuidDto) {
			result = updateFileUuid((FileUuidDto) objectDto);
		}
		else {
			System.out.println("***  update an unsupported DTO type into the database. ***");
		}
		return result;
	}

	@Override
	public boolean delete(String table, String pk) {
		boolean result = false;

		if(table.equals("file_management")) {
			result = deleteFileManagement(pk);
		}
		else if(table.equals("file_uuid")) {
			result = deleteFileUuid(pk);
		}
		else {
			System.out.println("*** delete Invalid table name [" + table + "] ***");
		}

		return result;
	}

	@Override
	public ArrayList<Object> executeQuery(String query) {
		ArrayList<Object> dtos = new ArrayList<>();

		if(query.indexOf("from file_management") != -1) {
			dtos.addAll(excuteQueryFileManagement(query));

		}
		else if(query.indexOf("from file_uuid") != -1) {
			dtos.addAll(excuteQueryFileUuid(query));

		}
		else {
			System.out.println("*** query Invalid ***");
		}
		return dtos;
	}

	@Override
	public boolean executeUpdate(String query) {
		try (Connection connection = getConnection(); Statement statement = connection.createStatement();) {

			int check = statement.executeUpdate(query);
			if(check == 0) {
				System.out.println("Database [" + query + "]: fail");
			}
			else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	//==========================================================================================

	private boolean existsFileManagement(String pk) {
		String query = "select dataid from file_management where dataid='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			if(resultSet.next()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean existsFileUuid(String pk) {
		String query = "select dataid from file_uuid whare fileName='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			if(resultSet.next()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
