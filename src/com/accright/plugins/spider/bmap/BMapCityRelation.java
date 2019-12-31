package com.accright.plugins.spider.bmap;

import com.accright.plugins.spider.utils.DBFactory;

import org.apache.commons.dbutils.QueryRunner;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;

public class BMapCityRelation {
    public static QueryRunner queryRunner = new QueryRunner();
    public static ThreadLocal<Connection> connHolder = new ThreadLocal<Connection>(){
        @Override
        protected Connection initialValue() {
            try {
                return DBFactory.getConnection();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    };
    public static Connection conn;
    static {
        try {
            //conn = DBFactory.getConnection();
            conn = connHolder.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String args[]) throws IOException {
        //使用文件流读取文件并存入数据库
        File file = new File("D:\\BaiduMap_cityCode_1102.txt");
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        try{
            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                System.out.println("line is "+line);
                String lineArr[] = line.split(",");
                String areaId = lineArr[0];
                String name = lineArr[1];
                //存入数据库
                String sql = "insert into t_bmap_city_relation(area_id,name) values (?,?)";
                queryRunner.update(conn,sql,areaId,name);
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (SQLException e){
            e.printStackTrace();
        }finally {
            bufferedReader.close();
        }
    }
}
