package com.accright.plugins.spider.amap.manager;

import com.accright.plugins.spider.pattern.PatternAddress;
import com.accright.plugins.spider.handler.MapListHandler;
import com.accright.plugins.spider.utils.CoordinateTransform;
import com.accright.plugins.spider.utils.DBFactory;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;

import javax.sql.rowset.serial.SerialClob;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 将百度坐标系范围转换为GPS坐标系范围 和将高德坐标系 转换为GPS坐标系
 */
public class CoordinateBoundary {
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
            conn = connHolder.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PatternAddress pattern = PatternAddress.getInstance();
    /**
     * 坐标系转换
     */
    public static void convertLoc() throws SQLException, IOException {
        String countSql = "select count(1) from t_amap_addr_temp_test ";
        long count = Long.parseLong(queryRunner.query(conn,countSql,new ScalarHandler<BigDecimal>()).toString());
        long start = 0L;
        long end = 100L;
        String querySql = "select * from ( select a.*,rownum pageNum from t_amap_addr_temp_test a) t " +
                "where t.pageNum > ? and t.pageNum <= ?";
        String updateSql = "update t_amap_addr_temp_test t set t.gps_longitude = ?,t.gps_latitude = ? where t.id = ?";
        String updateBoundarySql = "update t_amap_addr_temp_test t set t.GPS_BOUNDARY = ? where t.id = ?";
        //100条批量处理
        while(end < count){
            List<Map<String, Object>> mapList = queryRunner.query(conn,querySql,new MapListHandler(),start,end);
            for (Map<String, Object> map:mapList){
                if (map != null && map.get("ID") != null){
                    String id = map.get("ID").toString();
                    if (map.get("LONGITUDE") != null && map.get("LATITUDE") != null){
                        double longitude = Double.parseDouble(map.get("LONGITUDE").toString());
                        double latitude = Double.parseDouble(map.get("LATITUDE").toString());
                        Double[] gpsPoints = CoordinateTransform.gcj02towgs84(longitude,latitude);
                        //保存到数据库中
                        queryRunner.update(conn,updateSql,gpsPoints[0]+"",gpsPoints[1]+"",id);
                    }
                    if (map.get("BOUNDARY_POINTS") != null){
                        String boundaryPoints = ClobToString((Clob)map.get("BOUNDARY_POINTS"));
                        List<String> gpsBoundaryList = new ArrayList<String>();
                        String points[] = boundaryPoints.split(";");
                        for (String point : points){
                            String[] lngLat = point.split(",");
                            Double[] gcLngLat = CoordinateTransform.bd09togcj02(Double.valueOf(lngLat[0]),Double.valueOf(lngLat[1]));
                            Double[] gpsLngLat = CoordinateTransform.gcj02towgs84(gcLngLat[0],gcLngLat[1]);
                            String gpsLngLatS = gpsLngLat[0]+","+gpsLngLat[1];
                            gpsBoundaryList.add(gpsLngLatS);
                        }
                        String gpsBoundary = StringUtils.join(gpsBoundaryList,";");
                        //操作大数据类型查询数据库
                        Clob gpsBoundaryClob = new SerialClob(gpsBoundary.toCharArray());
                        queryRunner.update(conn,updateBoundarySql,gpsBoundary,id);
                    }
                }
            }
            System.out.println("当前已处理为："+start+","+"end");
            //继续分页遍历
            start += 100L;
            end += 100L;
        }
    }

    /*六级地址分类转换*/
    public static void convertTypes() throws SQLException, IOException {
        String countSql = "select count(1) from t_recap_zone t";
        long count = Long.parseLong(queryRunner.query(conn,countSql,new ScalarHandler<BigDecimal>()).toString());
        long start = 0L;
        long end = 100L;
        String querySql = "select * from ( select a.*,rownum pageNum from t_recap_zone a) t " +
                "where t.pageNum > ? and t.pageNum <= ?";
        String updateSql = "update t_recap_zone t set t.cover_scene = ? where t.int_id = ?";
        //100条批量处理
        while(end < count){
            List<Map<String, Object>> mapList = queryRunner.query(conn,querySql,new MapListHandler(),start,end);
            for (Map<String, Object> map:mapList){
                if (map != null && map.get("INT_ID") != null){
                    String id = map.get("INT_ID").toString();
                    if (map.get("COVER_SCENE") != null){
                        String coverScene = map.get("COVER_SCENE").toString();
                        String passCover = pattern.getTypeRel(coverScene);
                        if ("".equals(passCover)){
                            passCover = "其他";
                        }
                        //保存到数据库中
                        queryRunner.update(conn,updateSql,passCover,id);
                    }
                }
            }
            System.out.println("当前已处理为："+start+","+"end");
            //继续分页遍历
            start += 100L;
            end += 100L;
        }
    }

    /*程序入库GT_GEOMETRY库*/
    public static void covert2Geometry() throws SQLException, IOException {
        //用最原生的办法插入Clob
        Statement statement = conn.createStatement();
        String querySql = "select id," +
                "           related_id," +
                "           zh_label," +
                "           types," +
                "           longitude," +
                "           latitude," +
                "           city_name," +
                "           county_name," +
                "           addr_name," +
                "           bmap_uid," +
                "           bmap_addr," +
                "           bmap_prov," +
                "           bmap_city," +
                "           bmap_county," +
                "           bmap_road," +
                "           bmap_roadnum," +
                "           bmap_othertext," +
                "           bmap_zhlabel," +
                "           bmap_types," +
                "           bmap_town," +
                "           prov_name," +
                "           gps_longitude," +
                "           gps_latitude," +
                "           gps_boundary" +
                "      from jiakegis.t_amap_addr_temp_test" +
                "     where gps_boundary is not null";
        String insertSql = "insert into jiakegis.t_amap_poi" +
        "        (objectid," +
                "         id," +
                "         related_id," +
                "         zh_label," +
                "         types," +
                "         longitude," +
                "         latitude," +
                "         city_name," +
                "         county_name," +
                "         addr_name," +
                "         bmap_uid," +
                "         bmap_addr," +
                "         bmap_prov," +
                "         bmap_city," +
                "         bmap_county," +
                "         bmap_road," +
                "         bmap_roadnum," +
                "         bmap_othertext," +
                "         bmap_zhlabel," +
                "         bmap_types," +
                "         bmap_town," +
                "         prov_name," +
                "         gps_longitude," +
                "         gps_latitude," +
                "         shape)" +
                "      values" +
                "        (JIAKEGIS.VERSION_USER_DDL.NEXT_ROW_ID('JIAKEGIS', 62)," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         ?," +
                "         sde.st_polyfromtext(?, 4326))";
        List<Map<String, Object>> mapList = queryRunner.query(conn,querySql,new MapListHandler());
        for (Map<String, Object> map:mapList){
            String id = "";
            String related_id = "";
            String zh_label = "";
            String types = "";
            String longitude = "";
            String latitude = "";
            String city_name = "";
            String county_name = "";
            String addr_name = "";
            String bmap_uid = "";
            String bmap_addr = "";
            String bmap_prov = "";
            String bmap_city = "";
            String bmap_county = "";
            String bmap_road = "";
            String bmap_roadnum = "";
            String bmap_othertext = "";
            String bmap_zhlabel = "";
            String bmap_types = "";
            String bmap_town = "";
            String prov_name = "";
            String gps_longitude = "";
            String gps_latitude = "";
            String gps_boundary = "";
            if (map != null && map.get("ID") != null) {
                id = map.get("ID").toString();
            }
            if (map != null && map.get("RELATED_ID") != null) {
                related_id = map.get("RELATED_ID").toString();
            }
            if (map != null && map.get("ZH_LABEL") != null) {
                zh_label = map.get("ZH_LABEL").toString();
            }
            if (map != null && map.get("TYPES") != null) {
                types = map.get("TYPES").toString();
            }
            if (map != null && map.get("LONGITUDE") != null) {
                longitude = map.get("LONGITUDE").toString();
            }
            if (map != null && map.get("LATITUDE") != null) {
                latitude = map.get("LATITUDE").toString();
            }
            if (map != null && map.get("CITY_NAME") != null) {
                city_name = map.get("CITY_NAME").toString();
            }
            if (map != null && map.get("COUNTY_NAME") != null) {
                county_name = map.get("COUNTY_NAME").toString();
            }
            if (map != null && map.get("ADDR_NAME") != null) {
                addr_name = map.get("ADDR_NAME").toString();
            }
            if (map != null && map.get("BMAP_UID") != null) {
                bmap_uid = map.get("BMAP_UID").toString();
            }
            if (map != null && map.get("BMAP_ADDR") != null) {
                bmap_addr = map.get("BMAP_ADDR").toString();
            }
            if (map != null && map.get("BMAP_PROV") != null) {
                bmap_prov = map.get("BMAP_PROV").toString();
            }
            if (map != null && map.get("BMAP_CITY") != null) {
                bmap_city = map.get("BMAP_CITY").toString();
            }
            if (map != null && map.get("BMAP_COUNTY") != null) {
                bmap_county = map.get("BMAP_COUNTY").toString();
            }
            if (map != null && map.get("BMAP_ROAD") != null) {
                bmap_road = map.get("BMAP_ROAD").toString();
            }
            if (map != null && map.get("BMAP_ROADNUM") != null) {
                bmap_roadnum = map.get("BMAP_ROADNUM").toString();
            }
            if (map != null && map.get("BMAP_OTHERTEXT") != null) {
                bmap_othertext = map.get("BMAP_OTHERTEXT").toString();
            }
            if (map != null && map.get("BMAP_ZHLABEL") != null) {
                bmap_zhlabel = map.get("BMAP_ZHLABEL").toString();
            }
            if (map != null && map.get("BMAP_TYPES") != null) {
                bmap_types = map.get("BMAP_TYPES").toString();
            }
            if (map != null && map.get("BMAP_TOWN") != null) {
                bmap_town = map.get("BMAP_TOWN").toString();
            }
            if (map != null && map.get("PROV_NAME") != null) {
                prov_name = map.get("PROV_NAME").toString();
            }
            if (map != null && map.get("GPS_LONGITUDE") != null) {
                gps_longitude = map.get("GPS_LONGITUDE").toString();
            }
            if (map != null && map.get("GPS_LATITUDE") != null) {
                gps_latitude = map.get("GPS_LATITUDE").toString();
            }
            if (map != null && map.get("GPS_BOUNDARY") != null) {
                gps_boundary = ClobToString((Clob)map.get("GPS_BOUNDARY"));
            }
            if (gps_boundary != null && gps_boundary.length() < 4000 && isCircle(gps_boundary)){
                int x = queryRunner.update(conn,insertSql,id,related_id,zh_label,types,longitude,latitude,city_name,county_name
                        ,addr_name,bmap_uid,bmap_addr,bmap_prov,bmap_city,bmap_county,bmap_road,bmap_roadnum,bmap_othertext
                        ,bmap_zhlabel,bmap_types,bmap_town,prov_name,gps_longitude,gps_latitude);
            }else {
                int x = queryRunner.update(conn,insertSql,id,related_id,zh_label,types,longitude,latitude,city_name,county_name
                        ,addr_name,bmap_uid,bmap_addr,bmap_prov,bmap_city,bmap_county,bmap_road,bmap_roadnum,bmap_othertext
                        ,bmap_zhlabel,bmap_types,bmap_town,prov_name,gps_longitude,gps_latitude,gps_boundary);
            }
        }
    }

    //判断是否闭合
    public static boolean isCircle(String polygon){
        boolean isCircle = false;
        String reg = "polygon \\(\\(";
        String reg1 = "\\)\\)";
        Pattern xp = Pattern.compile(reg);
        String[] polygonx = xp.split(polygon);
        xp = Pattern.compile(reg1);
        String[] polygony = xp.split(polygonx[1]);
        String polygons[] = polygony[0].split(",");
        if (polygons[0].equals(polygons[polygons.length - 1])){
            return true;
        }else {
            return  false;
        }
    }

    // 将字Clob转成String类型
    public static String ClobToString(Clob sc) throws SQLException, IOException {
        String reString = "";
        Reader is = sc.getCharacterStream();// 得到流
        BufferedReader br = new BufferedReader(is);
        String s = br.readLine();
        StringBuffer sb = new StringBuffer();
        while (s != null) {// 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
            sb.append(s);
            s = br.readLine();
        }
        reString = sb.toString();
        return reString;
    }

    public static void main(String[] args) throws SQLException, IOException {
        //convertLoc();
        covert2Geometry();
        //convertTypes();
    }
}
