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

	private boolean insertFileManagement(FileManagementDto dto) {

		String insert_sql = "insert into " + "file_management" + " (dataid, availability_policy, cert, data_priority, data_signature, data_size, data_type, directory, file_type, linked_edge, security_level, timestamp)" + " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(insert_sql);) {
			pstmt.setString(1, dto.getDataId());
			pstmt.setInt(2, dto.getAvailabilityPolicy());
			pstmt.setString(3, dto.getCert());
			pstmt.setInt(4, dto.getDataPriority());
			pstmt.setString(5, dto.getDataSign());
			pstmt.setLong(6, dto.getDataSize());
			pstmt.setInt(7, dto.getDataType());
			pstmt.setString(8, dto.getDirectory());
			pstmt.setString(9, dto.getFileType());
			pstmt.setString(10, dto.getLinkedEdge());
			pstmt.setInt(11, dto.getSecurityLevel());
			pstmt.setTimestamp(12, dto.getTimestamp());
			int check = pstmt.executeUpdate();
			if(check == 0) {
				System.out.println("Data '" + dto.getDataId() + "' insert fail");
			}
			else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	private boolean insertFileUuid(FileUuidDto dto) {
		String insert_sql = "insert into " + "file_uuid" + " (fileName, fileUuid)" + " values(?, ?)";
		try (Connection connection = getConnection(); PreparedStatement pstmt = connection.prepareStatement(insert_sql);) {
			pstmt.setString(1, dto.getFileName());
			pstmt.setString(2, dto.getFileUuid());
			int check = pstmt.executeUpdate();
			if(check == 0) {
				System.out.println("Data '" + dto.getFileName() + "' insert fail");
			}
			else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<FileManagementDto> selectFileManagement() {
		ArrayList<FileManagementDto> dtos = new ArrayList<>();
		String query = "select * from file_management";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			while (resultSet.next()) {
				FileManagementDto dto = new FileManagementDto();
				dto.setDataId(resultSet.getString("dataid"));
				dto.setAvailabilityPolicy(resultSet.getInt("availability_policy"));
				dto.setCert(resultSet.getString("cert"));
				dto.setDataPriority(resultSet.getInt("data_priority"));
				dto.setDataSign(resultSet.getString("data_signature"));
				dto.setDataSize(resultSet.getLong("data_size"));
				dto.setDataType(resultSet.getInt("data_type"));
				dto.setDirectory(resultSet.getString("directory"));
				dto.setFileType(resultSet.getString("file_type"));
				dto.setLinkedEdge(resultSet.getString("linked_edge"));
				dto.setSecurityLevel(resultSet.getInt("security_level"));
				dto.setTimestamp(resultSet.getTimestamp("timestamp"));
				dtos.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dtos;
	}

	public FileManagementDto selectFileManagement(String pk) {
		String query = "select * from " + "file_management" + " where dataID='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			if(resultSet.next()) {
				FileManagementDto dto = new FileManagementDto();
				dto.setDataId(resultSet.getString("dataid"));
				dto.setAvailabilityPolicy(resultSet.getInt("availability_policy"));
				dto.setCert(resultSet.getString("cert"));
				dto.setDataPriority(resultSet.getInt("data_priority"));
				dto.setDataSign(resultSet.getString("data_signature"));
				dto.setDataSize(resultSet.getLong("data_size"));
				dto.setDataType(resultSet.getInt("data_type"));
				dto.setDirectory(resultSet.getString("directory"));
				dto.setFileType(resultSet.getString("file_type"));
				dto.setLinkedEdge(resultSet.getString("linked_edge"));
				dto.setSecurityLevel(resultSet.getInt("security_level"));
				dto.setTimestamp(resultSet.getTimestamp("timestamp"));
				return dto;
			}
			else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private ArrayList<FileUuidDto> selectFileUuid() {
		ArrayList<FileUuidDto> dtos = new ArrayList<>();
		String query = "select * from file_uuid";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			while (resultSet.next()) {
				FileUuidDto dto = new FileUuidDto();
				dto.setFileName(resultSet.getString("fileName"));
				dto.setFileUuid(resultSet.getString("fileUuid"));
				dtos.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dtos;
	}

	private FileUuidDto selectFileUuid(String pk) {
		String query = "select * from " + "file_uuid" + " where dataID='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			if(resultSet.next()) {
				FileUuidDto dto = new FileUuidDto();
				dto.setFileName(resultSet.getString("fileName"));
				dto.setFileUuid(resultSet.getString("fileUuid"));
				return dto;
			}
			else {
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean updateFileManagement(FileManagementDto dto) {
		String query = "update file_management" + " set availability_policy=?, cert=?, data_priority=?, data_signature=?, data_size=?, data_type=?, " + "directory=?, file_type=?, linked_edge=?, security_level=?, timestamp=? " + "where dataId=?";
		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(query);) {
			statement.setInt(1, dto.getAvailabilityPolicy());
			statement.setString(2, dto.getCert());
			statement.setInt(3, dto.getDataPriority());
			statement.setString(4, dto.getDataSign());
			statement.setLong(5, dto.getDataSize());
			statement.setInt(6, dto.getDataType());
			statement.setString(7, dto.getDirectory());
			statement.setString(8, dto.getFileType());
			statement.setString(9, dto.getLinkedEdge());
			statement.setInt(10, dto.getSecurityLevel());
			statement.setTimestamp(11, dto.getTimestamp());
			statement.setString(12, dto.getDataId());

			int check = statement.executeUpdate();
			if(check == 0) {
				System.out.println("Database '" + dto.getDataId() + "' update fail");
			}
			else
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	private boolean updateFileUuid(FileUuidDto dto) {
		String query = "update file_uuid set fileName=?, fileUuid=?";
		try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(query);) {
			statement.setString(1, dto.getFileName());
			statement.setString(2, dto.getFileUuid());

			int check = statement.executeUpdate();
			if(check == 0) {
				System.out.println("Database '" + dto.getFileName() + "' update fail");
			}
			else
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean deleteFileManagement(String pk) {
		String query = "delete from file_management where dataId='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement();) {
			int check = statement.executeUpdate(query);
			if(check == 0) {
				System.out.println("Database '" + pk + "' delete fail");
			}
			else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean deleteFileUuid(String pk) {
		String query = "delete from file_Uuid where dataId='" + pk + "'";
		try (Connection connection = getConnection(); Statement statement = connection.createStatement();) {
			int check = statement.executeUpdate(query);
			if(check == 0) {
				System.out.println("Database '" + pk + "' delete fail");
			}
			else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<FileManagementDto> excuteQueryFileManagement(String query) {
		ArrayList<FileManagementDto> dtos = new ArrayList<>();
		try (Connection connection = getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query);) {
			while (resultSet.next()) {
				FileManagementDto dto = new FileManagementDto();
				dto.setDataId(resultSet.getString("dataid"));
				dto.setAvailabilityPolicy(resultSet.getInt("availability_policy"));
				dto.setCert(resultSet.getString("cert"));
				dto.setDataPriority(resultSet.getInt("data_priority"));
				dto.setDataSign(resultSet.getString("data_signature"));
				dto.setDataSize(resultSet.getLong("data_size"));
				dto.setDataType(resultSet.getInt("data_type"));
				dto.setDirectory(resultSet.getString("directory"));
				dto.setFileType(resultSet.getString("file_type"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
