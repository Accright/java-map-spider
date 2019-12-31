package com.accright.plugins.spider.amap.manager;

import com.accright.plugins.spider.model.ExcelInfo;
import com.accright.plugins.spider.pattern.PatternAddress;
import com.accright.plugins.spider.utils.DBFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Font;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.metadata.Table;
import com.alibaba.excel.metadata.TableStyle;
import com.alibaba.excel.support.ExcelTypeEnum;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;


/**
 * @author Jeff.Wang
 * @Title: ExcelRowSX
 * @version V1.0
 * @desc 用于数据分割入库
 * @date 2019/6/21 14:37
 **/
public class AddressManager {
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
    //直接把 所有的数据存放到Map中
    private static Map<String,String> cityMap = new HashMap<String, String>();//name为 key uuid为value
    private static Map<String,String> countyMap = new HashMap<String, String>();//name为 key uuid为value
    private static Map<String,String> townMap = new HashMap<String, String>();//name为 key uuid为value
    private static Map<String,String> roadMap = new HashMap<String, String>();//name为 key uuid为value
    private static Map<String,String> roadnumMap = new HashMap<String, String>();//name为 key uuid为value
    private static PatternAddress patternAddress = PatternAddress.getInstance();

    /**
     * 插入地市数据
     */
    public static void insertCity() throws SQLException {
        String cusSql =  "select t.bmap_city from t_amap_addr_temp_test t  where t.bmap_prov = '北京市' group by t.bmap_city";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        Set<String> citySet = new HashSet();
        for (Map cusMap : cusList){
            if (cusMap.get("BMAP_CITY") != null){
                String cusCityName = cusMap.get("BMAP_CITY").toString();
                citySet.add(cusCityName);
            }
        }
        //使用HashSet入库
        for (String cityName : citySet){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String sql = " insert into t_recap_city (int_id,zh_label,alias,stateflag,city_id) values (?,?,?,?,?)";
            queryRunner.update(conn,sql,uuid,cityName,cityName,0,uuid);//执行插入的SQL
            cityMap.put(cityName,uuid);
        }
    }
    /**
     * 插入区县数据
     */
    public static void insertCounty() throws SQLException {
        String cusSql =  " select t.bmap_city,t.bmap_county from t_amap_addr_temp_test t group by t.bmap_county,t.bmap_city";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        Set<Map> countySet = new HashSet();
        for (Map cusMap : cusList){
            countySet.add(cusMap);
        }
        //使用HashSet入库
        for (Map countyNameMap : countySet){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            if (countyNameMap.get("BMAP_COUNTY") != null && countyNameMap.get("BMAP_CITY") != null){
                String countyName = countyNameMap.get("BMAP_COUNTY").toString();
                String cityName = countyNameMap.get("BMAP_CITY").toString();
                String cityUid = cityMap.get(cityName);
                //String
                String sql = " insert into t_recap_county (int_id,zh_label,alias,stateflag,city_id,county_id) values (?,?,?,?,?,?)";
                queryRunner.update(conn,sql,uuid,countyName,countyName,0,cityUid,uuid);//执行插入的SQL
                countyMap.put(countyName,uuid);
            }
        }
    }

    /**
     *插入城镇和街道数据
     */
    public static void insertTown() throws SQLException {
        String cusSql =  " select t.bmap_town,t.bmap_county,t.bmap_city from t_amap_addr_temp_test t group by t.bmap_town,t.bmap_county,t.bmap_city";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        Set<Map> townSet = new HashSet();
        for (Map cusMap : cusList){
            townSet.add(cusMap);
        }
        //使用HashSet入库
        for (Map townNameMap : townSet){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String cityName = "";
            String countyName = "";
            String townName = "";
            if (townNameMap.get("BMAP_CITY") != null && townNameMap.get("BMAP_COUNTY") != null) {
                cityName = townNameMap.get("BMAP_CITY").toString();
                countyName = townNameMap.get("BMAP_COUNTY").toString();
            }
            if (townNameMap.get("BMAP_TOWN") != null){
                townName = townNameMap.get("BMAP_TOWN").toString();
                String cityUid = cityMap.get(cityName);
                String countyUid = countyMap.get(countyName);
                String sql = " insert into t_recap_town (int_id,zh_label,addr_type,city_id,county_id) values (?,?,?,?,?)";
                if (townName != null && townName.contains("镇") && !"null".equals(townName)){
                    queryRunner.update(conn,sql,uuid,townName,"镇",cityUid,countyUid);//执行插入的SQL
                    townMap.put(townName,uuid);
                }else if (townName != null && townName.contains("乡") && !"null".equals(townName)){
                    queryRunner.update(conn,sql,uuid,townName,"乡",cityUid,countyUid);//执行插入的SQL
                    townMap.put(townName,uuid);
                }else if (townName != null && townName.contains("街道") && !"null".equals(townName)){
                    queryRunner.update(conn,sql,uuid,townName,"街道",cityUid,countyUid);//执行插入的SQL
                    townMap.put(townName,uuid);
                }else if (townName != null && !"null".equals(townName)){
                    queryRunner.update(conn,sql,uuid,townName,"未知",cityUid,countyUid);//执行插入的SQL
                    townMap.put(townName,uuid);
                }
            }
        }
    }


    /**
     * 插入路数据
     */
    public static void insertRoad() throws SQLException {
        String cusSql =  " select t.bmap_road,t.bmap_town,t.bmap_county,t.bmap_city from t_amap_addr_temp_test t group by t.bmap_road,t.bmap_town,t.bmap_county,t.bmap_city";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        Set<Map> roadSet = new HashSet();
        for (Map cusMap : cusList){
            roadSet.add(cusMap);
        }
        //使用HashSet入库
        for (Map roadNameMap : roadSet){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String cityName = "";
            String countyName = "";
            String townName = "";
            String roadName = "";
            if (roadNameMap.get("BMAP_CITY") != null && roadNameMap.get("BMAP_COUNTY") != null) {
                cityName = roadNameMap.get("BMAP_CITY").toString();
                countyName = roadNameMap.get("BMAP_COUNTY").toString();
            }
            if (roadNameMap.get("BMAP_TOWN") != null){
                townName = roadNameMap.get("BMAP_TOWN").toString();
            }
            if (roadNameMap.get("BMAP_ROAD") != null){
                roadName = roadNameMap.get("BMAP_ROAD").toString();
                String cityUid = cityMap.get(cityName);
                String countyUid = countyMap.get(countyName);
                String townUid = townMap.get(townName);
                String sql = " insert into t_recap_road (int_id,zh_label,addr_type,city_id,county_id,town_id) values (?,?,?,?,?,?)";
                if (roadName != null && roadName.contains("路") && !"null".equals(roadName)){
                    queryRunner.update(conn,sql,uuid,roadName,"路",cityUid,countyUid,townUid);//执行插入的SQL
                    roadMap.put(roadName,uuid);
                }else if (roadName != null && roadName.contains("巷") && !"null".equals(roadName)){
                    queryRunner.update(conn,sql,uuid,roadName,"巷",cityUid,countyUid,townUid);//执行插入的SQL
                    roadMap.put(roadName,uuid);
                }else if (roadName != null && roadName.contains("村") && !"null".equals(roadName)){
                    queryRunner.update(conn,sql,uuid,roadName,"村",cityUid,countyUid,townUid);//执行插入的SQL
                    roadMap.put(roadName,uuid);
                }else if (roadName != null && !"null".equals(roadName)){
                    queryRunner.update(conn,sql,uuid,roadName,"未知",cityUid,countyUid,townUid);//执行插入的SQL
                    roadMap.put(roadName,uuid);
                }
            }
        }
    }

    /**
     * 插入门牌号数据
     */
    public static void insertNumber() throws SQLException {
        String cusSql =  " select t.bmap_roadnum,t.bmap_road,t.bmap_town,t.bmap_county,t.bmap_city from t_amap_addr_temp_test t group by t.bmap_roadnum,t.bmap_road,t.bmap_town,t.bmap_county,t.bmap_city";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        Set<Map> numberSet = new HashSet();
        for (Map cusMap : cusList){
            numberSet.add(cusMap);
        }
        //使用HashSet入库
        for (Map numberMap : numberSet){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String cityName = "";
            String countyName = "";
            String townName = "";
            String roadName = "";
            String numberName = "";
            if (numberMap.get("BMAP_CITY") != null && numberMap.get("BMAP_COUNTY") != null){
                cityName = numberMap.get("BMAP_CITY").toString();
                countyName = numberMap.get("BMAP_COUNTY").toString();
            }
            if (numberMap.get("BMAP_TOWN") != null){
                townName = numberMap.get("BMAP_TOWN").toString();
            }
            if (numberMap.get("BMAP_ROAD") != null){
                roadName = numberMap.get("BMAP_ROAD").toString();
            }
            if (numberMap.get("BMAP_ROADNUM") != null){
                numberName = numberMap.get("BMAP_ROADNUM").toString();
                String cityUid = cityMap.get(cityName);
                String countyUid = countyMap.get(countyName);
                String roadUid = roadMap.get(roadName);
                String townUid = townMap.get(townName);
                String sql = " insert into t_recap_number (int_id,zh_label,city_id,county_id,town_id,road_id) values (?,?,?,?,?,?)";
                //如果路为空的话 直接舍弃这条地址
                if (roadName != null && !"null".equals(roadName) && numberName != null && !"null".equals(numberName)){
                    queryRunner.update(conn,sql,uuid,numberName,cityUid,countyUid,townUid,roadUid);//执行插入的SQL
                    roadnumMap.put(numberName,uuid);
                }
            }
        }
    }

    /**
     * 插入地址库
     */
    public static void insertZone(String[] classfication) throws SQLException, IOException {
        String cusSql =  " select t.* from t_amap_addr_temp_test t";//查询客户表中的地市
        //使用HashSet进行去重
        List<Map<String, Object>> cusList = queryRunner.query(conn,cusSql,new MapListHandler());
        //使用List入库   对于客户数据按照分类入库
        for (Map zoneMap : cusList){
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String cityName = "";
            String countyName = "";
            String townName = "";
            String roadName = "";
            String numberName = "";
            String zhLabel = "";
            if (zoneMap.get("BMAP_CITY") != null){
                cityName = zoneMap.get("BMAP_CITY").toString();
            }
            if (zoneMap.get("BMAP_COUNTY") != null){
                countyName = zoneMap.get("BMAP_COUNTY").toString();
            }
            if (zoneMap.get("BMAP_TOWN") != null){
                townName = zoneMap.get("BMAP_TOWN").toString();
            }
            if (zoneMap.get("BMAP_ROAD") != null){
                roadName = zoneMap.get("BMAP_ROAD").toString();
            }
            if (zoneMap.get("BMAP_ROADNUM") != null){
                numberName = zoneMap.get("BMAP_ROADNUM").toString();
            }
            if (zoneMap.get("ZH_LABEL") != null){
                zhLabel = zoneMap.get("ZH_LABEL").toString();
            }
            String types = zoneMap.get("TYPES").toString();
            String longitude = zoneMap.get("LONGITUDE").toString();
            String latitude = zoneMap.get("LATITUDE").toString();
            String subText = "";
            if (zoneMap.get("BMAP_OTHERTEXT") != null){
                subText = zoneMap.get("BMAP_OTHERTEXT").toString();
            }
            //String boundary = zoneMap.get("BOUNDARY").toString();
            Clob boundary = null;//处理大数据类型
            if (zoneMap.get("BMAP_BOUNDARY") != null){
                boundary = (Clob) zoneMap.get("BMAP_BOUNDARY");
            }
            String fullAddr = cityName+countyName;
            if (townName != null && !"".equals(townName) && !"null".equals(townName)){
                fullAddr += townName;
            }
            if (roadName != null && !"".equals(roadName) && !"null".equals(roadName)){
                fullAddr += roadName;
            }
            if (numberName != null && !"".equals(numberName) && !"null".equals(numberName)){
                fullAddr += numberName;
            }
            //是否包含在types中
            boolean isContained = AddressManager.getClassfication(classfication,types);
            //坐标转换为wg ---不转换
            String cityUid = cityMap.get(cityName);
            String countyUid = countyMap.get(countyName);
            String townUid = townMap.get(townName);
            String roadUid = roadMap.get(roadName);
            //String numberUid = roadnumMap.get(numberName);
            //分类值
            String relType = patternAddress.getTypeRel(types);
            if ((isContained || boundary != null) && roadName != null && !"".equals(roadName) && !"null".equals(roadName)){
                String sql = " insert into t_recap_zone (int_id,zh_label,city_id,county_id,town_id,road_id,longitude,latitude,cover_scene,full_addr,ADDITION,boundary) " +
                        " values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
                //根据分类
                queryRunner.update(conn,sql,uuid,zhLabel,cityUid,countyUid,townUid,roadUid,longitude,latitude,relType,fullAddr,subText,boundary);//执行插入的SQL
            }else {
                String sql = " insert into T_RECAP_CUSTOMER (int_id,zh_label,city_id,city_name,county_id,county_name," +
                        " customer_addr,customer_type,longitude,latitude) " +
                        " values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
        }
    }

    /**
     * 判断是否有包含在该分类中
     * @param classfication
     * @param types
     * @return
     */
    public static boolean getClassfication(String[] classfication,String types){
        boolean isContained = false;
        for (String type : classfication){
            if (types.contains(type)){
                isContained = true;
            }
        }
        return isContained;
    }

    //输出为csv报表
    public static void exportExcel() throws SQLException, IOException {
        String cusSql =  " select t.* from t_amap_addr_bj_all t where t.bmap_boundary is null";//查询所有的楼栋信息
        List<ExcelInfo> infoList = queryRunner.query(conn,cusSql,new BeanListHandler<ExcelInfo>(ExcelInfo.class));
        List<ExcelInfo> excelInfoList = new ArrayList<ExcelInfo>();

        for (ExcelInfo excelInfo : infoList){
            String types = excelInfo.getTYPES();
            String relType = patternAddress.getTypeRel(types);
            if (relType != null && !"".equals(relType)){
                excelInfoList.add(excelInfo);
            }
            try {
                if (patternAddress.filterKeywords(null,excelInfo)){
                    excelInfoList.remove(excelInfo);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        //是否包含在types中
        OutputStream outputStream=new FileOutputStream("D:\\标准地址分析-1.xlsx");
        //定义Excel正文背景颜色
        TableStyle tableStyle=new TableStyle();
        tableStyle.setTableContentBackGroundColor(IndexedColors.WHITE);

        //定义Excel正文字体大小
        Font font=new Font();
        font.setFontHeightInPoints((short) 10);
        tableStyle.setTableContentFont(font);

        Table table=new Table(0);
        table.setTableStyle(tableStyle);

        //这里指定需要表头，因为model通常包含表头信息
        ExcelWriter writer = new ExcelWriter(outputStream, ExcelTypeEnum.XLSX,true);
        //写第一个sheet, sheet1  数据全是List<String> 无模型映射关系
        Sheet sheet1 = new Sheet(1,0, ExcelInfo.class);
        writer.write(excelInfoList, sheet1, table);
        writer.finish();
    }

    public static void main(String args[]) throws SQLException, IOException {
        String classfications[] = {"商务住宅","科教文化服务;学校;高等院校","科教文化服务;学校;中学","科教文化服务;学校;小学",
                "科教文化服务;学校;幼儿园","购物服务;花鸟鱼虫市场","购物服务;家居建材市场","购物服务;综合市场","购物服务;特色商业街",
                "体育休闲服务;运动场馆","体育休闲服务;高尔夫相关","体育休闲服务;娱乐场所;夜总会","体育休闲服务;度假疗养场所",
                "体育休闲服务;休闲场所;游乐场","体育休闲服务;休闲场所;垂钓园","体育休闲服务;休闲场所;采摘园",
                "体育休闲服务;休闲场所;露营地","体育休闲服务;休闲场所;水上活动中心","医疗保健服务;综合医院",
                "医疗保健服务;专科医院","住宿服务;宾馆酒店","风景名胜","公司企业;知名企业;知名企业","购物服务;商场","购物服务;超级市场",
                "购物服务;超级市场","政府机构及社会团体","地名地址信息;普通地名;村庄级地名","地名地址信息;普通地名;村组级地名",
                "地名地址信息;自然地名","地名地址信息;交通地名","地名地址信息;标志性建筑物;标志性建筑物", "地名地址信息;热点地名;热点地名"
        };
        insertCity();//插入地市
        insertCounty();//插入区县
        insertTown();//插入乡镇和街道
        insertRoad();//插入路
        //insertNumber();//插入门牌号
        insertZone(classfications);//插入区域地址
        exportExcel();
    }
}
