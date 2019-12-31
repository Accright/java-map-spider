package com.accright.plugins.spider.utils;

import org.apache.commons.dbcp.BasicDataSourceFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBFactory {
	
	private static DataSource dataSource = null;
	
	static {
		if (null == dataSource) {
			setDataSource();
		}
	}

	public static void setDataSource() {
        String driver = "oracle.jdbc.driver.OracleDriver";
		String url = "jdbc:oracle:thin:@*********";
		String username = "***";
		String password = "***";
		String initialSize ="20";
		String maxActive ="20";
		String maxIdle = "20";//最大空闲连接数
        String maxWait = "10000";
		if(initialSize==null||"".equals(initialSize.trim()))
		{
			initialSize = "10";
		}
		if(maxActive==null||"".equals(maxActive.trim()))
		{
			maxActive= "20";
		}
		
		Properties prop = new Properties();
		prop.put("driverClassName", driver);
		prop.put("url", url);
		prop.put("username", username);
		prop.put("password", password);
		prop.put("initialSize", initialSize);
		prop.put("maxActive", maxActive);
        prop.put("maxIdle", maxIdle);
        prop.put("maxWait ", maxWait);
		
		try {
			dataSource = BasicDataSourceFactory.createDataSource(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Connection getConnection() throws Exception {
		return dataSource.getConnection();
	}
	
	
	public static void exeInsert(String sql) throws Exception {
		Connection conn = null;
		Statement pstmt = null;
		ResultSet rs = null;
		try{
			conn = dataSource.getConnection();
			pstmt=conn.createStatement();
			pstmt.executeUpdate(sql);
		}catch(Exception e){
			throw e;
		}finally{
			//关闭连接
			closeConn(conn,pstmt,rs);
		}
	}
	/**
	 * @author wenwei
	 * 关闭连接
	 * @param conn
	 * @param pstmt
	 * @param rs
	 */
	public static void closeConn(Connection conn, Statement pstmt, ResultSet rs){
		try {
			if(rs!=null){
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			if(pstmt!=null){
				pstmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			if(conn!=null){
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
