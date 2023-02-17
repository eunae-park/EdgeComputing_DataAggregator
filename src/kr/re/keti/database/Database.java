package kr.re.keti.database;

import java.sql.Connection;
import java.util.ArrayList;

public interface Database {
	public Connection getConnection();
	public boolean exists(String table, String pk);
	public boolean update(Object ObjectDto);
	public boolean delete(String table, String pk);
	public boolean insert(Object objectDto);
	public ArrayList<Object> select(String table);
	public Object select(String table, String pk);
	public ArrayList<Object> executeQuery(String query);
	public boolean executeUpdate(String query);
}
