package com.mup.pop3;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

public class SQLiteUIDStore {
	private final static Logger log = LoggerFactory.getLogger(SQLiteUIDStore.class);
	private final static String DEFAULT_DATABASE = "config/UIDStore.db";

	private static SQLiteUIDStore instance = null;

	private Connection connection;
	private String dbFileName;
	private String jdbcUrl;
	private boolean isOpened = false;



	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	public static SQLiteUIDStore getInstance() {
		if (instance == null) {
			instance = new SQLiteUIDStore(DEFAULT_DATABASE);
		}
		return instance;
	}

	private SQLiteUIDStore(String databaseFileName) {
		this.dbFileName = databaseFileName;
		this.jdbcUrl = "jdbc:sqlite:" + this.dbFileName;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setDbFileName(String dbFileName) {
		this.dbFileName = dbFileName;
		this.jdbcUrl = "jdbc:sqlite:" + this.dbFileName;
	}

	public boolean open() {
		return this.open(false);
	}

	public boolean open(boolean readOnly) {
		if (isOpened) {
			return true;
		}
		try {
			SQLiteConfig config = new SQLiteConfig();
			config.setReadOnly(readOnly);

			File dbFile = new File(this.dbFileName);

			if (!dbFile.getParentFile().exists()) {
				dbFile.getParentFile().mkdir();
			}

			this.connection = DriverManager.getConnection(this.getJdbcUrl(), config.toProperties());
			log.debug("Database connected using url=" + this.getJdbcUrl());

			this.createUIDStore();
		} catch (SQLException e) {
			log.error(e.getSQLState() + ": " + e.getMessage());
			return false;
		}

		isOpened = true;
		return true;
	}

	public boolean close() {
		if (this.isOpened == false) {
			return true;
		}

		try {
			this.connection.close();
			this.isOpened = false;
		} catch (SQLException e) {
			log.error(e.getSQLState() + ": " + e.getMessage());
			return false;
		}
		return true;
	}

	private void createUIDStore() throws SQLException {
		PreparedStatement prep = null;

		try {
			String cols = "USER_ID TEXT";
			cols += ", UID TEXT";
			cols += ", DEL_IND TEXT";
			cols += ", DEL_DT TEXT";
			cols += ", PRIMARY KEY (USER_ID,UID)";

			String query = "CREATE TABLE IF NOT EXISTS UID_STORE (" + cols + ");";

			prep = this.connection.prepareStatement(query);

			prep.execute();
		} finally {
			if (prep != null)
				prep.close();
		}
	}

	public boolean isDeleted(String user, String uid) throws SQLException {
		PreparedStatement prep = null;
		ResultSet row = null;

		try {
			String query = "SELECT UID, DEL_IND FROM UID_STORE WHERE USER_ID=? AND UID=?;";

			prep = this.connection.prepareStatement(query);
			prep.setString(1, user);
			prep.setString(2, uid);

			boolean result = false;

			row = prep.executeQuery();
			if( row.next() ) {
				if (row.getString("DEL_IND").equals("Y")) {
					result = true;
				}
			}
			else {
				prep.close();

				query = "INSERT INTO UID_STORE (USER_ID,UID,DEL_IND,DEL_DT) VALUES(?,?,?,'');";

				prep = this.connection.prepareStatement(query);
				prep.setString(1, user);
				prep.setString(2, uid);
				prep.setString(3, "N");
				prep.executeUpdate();
			}
			return result;
		}
		finally {
			if (row != null)
				row.close();
			if( prep != null ) 
				prep.close();
		}
	}

	public int delete(String user, String uid) throws SQLException {
		PreparedStatement prep = null;
		try {
			String query = "UPDATE UID_STORE SET DEL_IND='Y', DEL_DT = strftime('%Y%m%d','now', 'localtime') WHERE USER_ID=? AND UID=?;";

			prep = this.connection.prepareStatement(query);
			prep.setString(1, user);
			prep.setString(2, uid);

			prep.executeUpdate();
			
			int result = prep.getUpdateCount();
			
			prep.close();

			return result;
		} finally {
			if (prep != null)
				prep.close();
		}
	}
}
