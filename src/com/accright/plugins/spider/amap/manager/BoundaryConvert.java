package com.accright.plugins.spider.amap.manager;

import com.accright.plugins.spider.handler.MapListHandler;
import com.accright.plugins.spider.utils.DBFactory;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.StringUtils;

import javax.sql.rowset.serial.SerialClob;
import java.io.IOException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 将边界按照Arcgis规则调整
 */
public class BoundaryConvert {
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

    /**
     * 将边界按照Arcgis规则调整
     * @throws SQLException
     * @throws IOException
     */
    public static void convertLoc() throws SQLException, IOException {
        String querySql = "select * from t_amap_addr_temp_test t where t.gps_boundary is not null";
        String updateBoundarySql = "update t_amap_addr_temp_test t set t.GPS_BOUNDARY = ? where t.id = ?";
        //100条批量处理
        List<Map<String, Object>> mapList = queryRunner.query(conn,querySql,new MapListHandler());
        for (Map<String, Object> map:mapList){
            if (map != null && map.get("ID") != null){
                String id = map.get("ID").toString();
                if (map.get("GPS_BOUNDARY") != null){
                    String boundaryPoints = CoordinateBoundary.ClobToString((Clob)map.get("GPS_BOUNDARY"));
                    List<String> gpsBoundaryList = new ArrayList<String>();
                    String points[] = boundaryPoints.split(";");
                    //检查循环是否有多个闭环
                    for (int i = 0;i < points.length;i++){
                        if (points[0].equals(points[i]) && i != 0){
                            if (i != points.length - 1){
                                System.out.println("这个ID"+id+"没有形成闭合"+",其闭合点为"+i+1);
                            }else {
                                //如果是闭合的 直接更新为 polygon格式
                                for (String point : points){
                                    point = point.replaceAll(","," ");
                                    gpsBoundaryList.add(point);
                                }
                            }
                        }
                    }
                    String gpsBoundary = StringUtils.join(gpsBoundaryList,",");
                    gpsBoundary = "polygon (("+gpsBoundary+"))";
                    //操作大数据类型查询数据库
                    Clob gpsBoundaryClob = new SerialClob(gpsBoundary.toCharArray());
                    queryRunner.update(conn,updateBoundarySql,gpsBoundary,id);
                }
            }
        }
        System.out.println("当前已处理");
    }

    public static void main(String[] args) throws SQLException, IOException {
        convertLoc();
    }
}
