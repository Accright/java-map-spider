package com.accright.plugins.spider.pattern;

import com.accright.plugins.spider.model.ExcelInfo;
import com.accright.plugins.spider.model.ExcelRowSX;
import com.accright.plugins.spider.model.TAmapCusTemp;
import com.accright.plugins.spider.utils.easyexcel.ExcelUtil;
import com.accright.plugins.spider.utils.DBFactory;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 根据爬取的地址进行正则表达式分割 分别如标准地址级别库
 */
public class PatternAddress {

    public static volatile PatternAddress patternAddress = null;

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

    public static Map<String,String[]> reMap = null;
    public static List<Map<String,String>> filterMapL = null;

    static {
        try {
            Connection connection = DBFactory.getConnection();
            reMap = getRelMap(queryRunner,connection);
            filterMapL = getFilterMap(queryRunner,connection);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String []args){
        //String testStr = "[贵州省(520000)|PROV|0|][贵阳市(520100)|CITY|1|][南明区(520102)|AREA|1|][瑞金南路()|ROAD|1|25号$]";
        //String testStr = "[贵州省(520000)|PROV|0|][贵阳市(520100)|CITY|0|][云岩区(520103)|AREA|0|]新添大道南段289号";
        //String testStr = "[贵州省(520000)|PROV|1|][贵阳市(520100)|CITY|1|][乌当区(520112)|AREA|1|][航天路()|ROAD|0|]航天大道中段172号";
        String testStr = "[贵州省(520000)|PROV|1|][贵阳市(520100)|CITY|1|][花溪区(520111)|AREA|1|]聚缘宾馆([孟关大道()|ROAD|1|]店)";
        //String testStr = "[贵州省(520000)|PROV|1|][贵阳市(520100)|CITY|1|][花溪区(520111)|AREA|1|][花溪大道南段()|ROAD|1|][贵州民族大学(1106223252510683125)|POI_PARENT|1|]";

        String xareg = "\\|\\d*\\|\\]";//截取到]
        //String xareg = "\\|\\d*\\|\\][\\u4E00-\\u9FA5]{0,1000}\\(?\\[?";//截取到[

        Pattern xp = Pattern.compile(xareg);

        String[] a = xp.split(testStr);

        String prov = "";
        String city = "";
        String county = "";
        String road = "";
        String roadNum = "";
        String otherText = "";
        //处理所有的省市区分割字符
        for (String item : a){
            System.out.println("item "+item);
            String[] segmentation = item.split("\\|");
            //System.out.println("segmentation"+segmentation);
            if (segmentation.length > 1 && segmentation[1] != null && "PROV".equals(segmentation[1])){
                String pr = "\\(\\d+\\)";
                Pattern pp = Pattern.compile(pr);
                String temp[] = pp.split(segmentation[0]);
                prov = temp[0].substring(1,temp[0].length());
                System.out.println("prov is"+prov);
            }else if (segmentation.length > 1 && segmentation[1] != null && "CITY".equals(segmentation[1])){
                String cr = "\\(\\d+\\)";
                Pattern cp = Pattern.compile(cr);
                String temp[] = cp.split(segmentation[0]);
                city = temp[0].substring(1,temp[0].length());
                System.out.println("city is"+city);
            }else if (segmentation.length > 1 && segmentation[1] != null && "AREA".equals(segmentation[1])){
                String ar = "\\(\\d+\\)";
                Pattern ap = Pattern.compile(ar);
                String temp[] = ap.split(segmentation[0]);
                county = temp[0].substring(1,temp[0].length());
                System.out.println("county is"+county);
            }else if (segmentation.length > 1 && segmentation[1] != null && "ROAD".equals(segmentation[1])){
                String rr = "\\(\\d+\\)";
                Pattern rp = Pattern.compile(rr);
                String temp[] = rp.split(segmentation[0]);
                road = temp[0].substring(1,temp[0].length()-2);//去掉后面的()括号
                System.out.println("road is"+road);
                //取门牌号
                for (String tempx : segmentation){
                    if (tempx.contains("$")){
                        String roadNums[] = tempx.split("\\$");
                        roadNum = roadNums[0];
                    }
                }
            }else if (segmentation.length == 1){
                otherText = segmentation[0];
                System.out.println("otherText is"+otherText);
            }
        }
        System.out.println("prov is"+prov+" city is"+city+" county is"+county
                +" road is"+road+" roadNum is"+roadNum+" otherText is"+otherText);
    }


    /**
     * 循环处理所有的地址分割数据
     */
    public void dealPattern(List<TAmapCusTemp> list) {
        for (TAmapCusTemp tAmapAddrTemp : list){
            if (tAmapAddrTemp.getBmapAddr() != null){
                String bMapAddr = tAmapAddrTemp.getBmapAddr();
                Map map = this.getPatterMap(bMapAddr);
                if (map.get("prov") != null && !"".equals(map.get("prov").toString())){
                    String prov = map.get("prov").toString();
                    tAmapAddrTemp.setBmapProv(prov);
                }
                if(map.get("city") != null && !"".equals(map.get("city").toString())){
                    String city = map.get("city").toString();
                    tAmapAddrTemp.setBmapCity(city);
                }
                if(map.get("county") != null && !"".equals(map.get("county").toString())){
                    String county = map.get("county").toString();
                    tAmapAddrTemp.setBmapCounty(county);
                }
                if(map.get("road") != null && !"".equals(map.get("road").toString())){
                    String road = map.get("road").toString();
                    tAmapAddrTemp.setBmapRoad(road);
                }
                if(map.get("roadNum") != null && !"".equals(map.get("roadNum").toString())){
                    String roadNum = map.get("roadNum").toString();
                    tAmapAddrTemp.setBmapRoadnum(roadNum);
                }
                if(map.get("otherText") != null && !"".equals(map.get("otherText").toString())){
                    String otherText = map.get("otherText").toString();
                    tAmapAddrTemp.setBmapOthertext(otherText);
                }
                if (tAmapAddrTemp.getBmapOthertext() != null &&
                        !"".equals(tAmapAddrTemp.getBmapOthertext()) &&
                        tAmapAddrTemp.getBmapOthertext().contains("'")){
                    tAmapAddrTemp.setBmapOthertext("");
                }
                String sql = " update T_AMAP_CUS_TEMP a set a.BMAP_PROV='"+tAmapAddrTemp.getBmapProv()+"',a.BMAP_CITY='"
                        +tAmapAddrTemp.getBmapCity()+"',a.BMAP_COUNTY='"+tAmapAddrTemp.getBmapCounty()+"',"
                        +"a.BMAP_ROAD='"+tAmapAddrTemp.getBmapRoad()+"',a.BMAP_ROADNUM='"+tAmapAddrTemp.getBmapRoadnum()+"',"
                        +"a.BMAP_OTHERTEXT='"+tAmapAddrTemp.getBmapOthertext()+"' where a.id = '"+tAmapAddrTemp.getId()+"'";
                //tamapaddrtempDao.update(tAmapAddrTemp);//更新tAmapAddrTemp
                //int x = jdbcTemplate.update(sql);
                //System.out.println("x is "+x);
            }
        }
    }

    /**
     * 处理不符合格式的路的数据
     * @param list
     */
    public void dealPatternRoad(List<TAmapCusTemp> list) {
        for (TAmapCusTemp tAmapAddrTemp : list){
            if (tAmapAddrTemp.getBmapRoad() != null && tAmapAddrTemp.getBmapRoad().contains("[")){
                String[] roadP = tAmapAddrTemp.getBmapRoad().split("\\[");
                String road = roadP[1];
                String sql = " update T_AMAP_CUS_TEMP a set a.BMAP_ROAD='"+road+"' where a.id = '"+tAmapAddrTemp.getId()+"'";
                //tamapaddrtempDao.update(tAmapAddrTemp);//更新tAmapAddrTemp
                //int x = jdbcTemplate.update(sql);
                //System.out.println("x is "+x);
            }
        }
    }

    /**
     * 返回地址的字符串分割
     * @param str
     * @return
     */
    public Map getPatterMap(String str){
        Map map = new HashMap();
        String xareg = "\\|\\d*\\|\\]";//截取到]
        //String xareg = "\\|\\d*\\|\\][\\u4E00-\\u9FA5]{0,1000}\\(?\\[?";//截取到[

        Pattern xp = Pattern.compile(xareg);

        String[] a = xp.split(str);

        String prov = "";
        String city = "";
        String county = "";
        String road = "";
        String roadNum = "";
        String otherText = "";
        //处理所有的省市区分割字符
        for (String item : a){
            //System.out.println("item "+item);
            String[] segmentation = item.split("\\|");
            //System.out.println("segmentation"+segmentation);
            if (segmentation.length > 1 && segmentation[1] != null && "PROV".equals(segmentation[1])){
                String pr = "\\(\\d+\\)";
                Pattern pp = Pattern.compile(pr);
                String temp[] = pp.split(segmentation[0]);
                prov = temp[0].substring(1,temp[0].length());
                //System.out.println("prov is"+prov);
            }else if (segmentation.length > 1 && segmentation[1] != null && "CITY".equals(segmentation[1])){
                String cr = "\\(\\d+\\)";
                Pattern cp = Pattern.compile(cr);
                String temp[] = cp.split(segmentation[0]);
                city = temp[0].substring(1,temp[0].length());
                //System.out.println("city is"+city);
            }else if (segmentation.length > 1 && segmentation[1] != null && "AREA".equals(segmentation[1])){
                String ar = "\\(\\d+\\)";
                Pattern ap = Pattern.compile(ar);
                String temp[] = ap.split(segmentation[0]);
                county = temp[0].substring(1,temp[0].length());
                //System.out.println("county is"+county);
            }else if (segmentation.length > 1 && segmentation[1] != null && "ROAD".equals(segmentation[1])){
                String rr = "\\(\\d+\\)";
                Pattern rp = Pattern.compile(rr);
                String temp[] = rp.split(segmentation[0]);

                road = temp[0].substring(1,temp[0].length()-2);//去掉后面的()括号
                /**
                 * 对于区县和路之间有其他地址的数据 重新处理该地址获得完整的路信息
                 */
                if (road != null && road.contains("[")){
                    road = temp[0].substring(0,temp[0].length()-2);
                }
                //System.out.println("road is"+road);
                //取门牌号
                for (String tempx : segmentation){
                    if (tempx.contains("$")){
                        String roadNums[] = tempx.split("\\$");
                        roadNum = roadNums[0];
                    }
                }
            }else if (segmentation.length == 1){
                otherText = segmentation[0];
                //System.out.println("otherText is"+otherText);
            }
        }

        //返回map
        map.put("prov",prov);
        map.put("city",city);
        map.put("county",county);
        map.put("road",road);
        map.put("roadNum",roadNum);
        map.put("otherText",otherText);

        return map;
    }

    /**
     * 返回百度地图地市的对应map
     */
    public Map<String,String> getCityRelMap() throws IOException {
        Map<String,String> cityRelMap = new HashMap<String, String>();
        //使用文件流读取文件并存入数据库
        File file = new File("D:\\BaiduMap_cityCode_1102.txt");
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        try{
            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                //System.out.println("line is "+line);
                String lineArr[] = line.split(",");
                String areaId = lineArr[0];
                String name = lineArr[1];
                cityRelMap.put(name,areaId);
            }
        }finally {
            bufferedReader.close();
        }
        return cityRelMap;
    }

    /*
    * 获取类型映射Map
    * */
    public String getTypeRel(String types) throws IOException, SQLException {
        String reKey = "";
        Iterator<String> iterator = reMap.keySet().iterator();
        //遍历iterator map 获取类型
        while (iterator.hasNext()){
            String key = iterator.next();
            String[] value = reMap.get(key);
            for (String x : value){
                //x = x.replace("\n","");
                if (types.contains("|")){
                    boolean allHas = true;
                    String typeSeg[] = types.split("\\|");
                    for (String type : typeSeg){
                        if (!type.contains(x)){
                            allHas = false;
                            break;
                        }
                    }
                    if (allHas){
                        reKey = key;
                    }
                }else {
                    if (types.contains(x)){
                        reKey = key;
                        break;
                    }
                }
            }
        }
        return reKey;
    }

    //筛选关键字 返回true则证明需要过滤掉
    public boolean filterKeywords(Map map, ExcelInfo excelInfo) throws ClassNotFoundException, NoSuchFieldException,
            IllegalAccessException {
        boolean isF = false;
        for (Map<String,String> x : filterMapL){
            String column = x.get("column");
            String keywrods = x.get("keywords");
            String rules = x.get("rules");
            /*String value = map.get(column).toString();*/
            //使用反射获取javabean的字段值
            Class<?> clazz = Class.forName("com.accright.plugins.spider.model.ExcelInfo");
            Field field = clazz.getDeclaredField(column);
            field.setAccessible(true);
            Object object = field.get(excelInfo);
            String value = object.toString();
            if (!"".equals(value) && !"".equals(rules)){
                if ("end".equals(rules)){
                    if (value.endsWith(keywrods)){
                        isF = true;
                    }
                }else if ("start".equals(rules)){
                    if (value.startsWith(keywrods)){
                        isF = true;
                    }
                }else if ("contains".equals(rules)){
                    if (value.contains(keywrods)){
                        isF = true;
                    }
                }
            }
        }
        return isF;
    }

    /*
    getRelMap
     */
    //执行SQL
    public static Map<String,String[]> getRelMap(QueryRunner queryRunner, Connection conn) throws SQLException {
        List<Map<String,Object>> mapList = new ArrayList<Map<String, Object>>();
        Map<String,String[]> reMap = new HashMap<String, String[]>();
        String sql = "select t.* from t_amap_type_relation t ";
        mapList = queryRunner.query(conn,sql,new MapListHandler());
        for (Map<String,Object> map:mapList){
            if (map.get("REL_TYPE") != null && map.get("AMAP_TYPES") != null){
                String relType = map.get("REL_TYPE").toString();
                String amapType = map.get("AMAP_TYPES").toString();
                String[] xAmapType = amapType.split(",");
                reMap.put(relType,xAmapType);
            }
        }
        return reMap;
    }

    //获取筛选的方式
    //执行SQL
    public static List<Map<String,String>> getFilterMap(QueryRunner queryRunner, Connection conn) throws SQLException {
        List<Map<String,Object>> mapList = new ArrayList<Map<String, Object>>();
        List<Map<String,String>> filterMapList = new ArrayList<Map<String, String>>();
        Map<String,String> reMap = new HashMap<String, String>();
        String sql = "select t.* from t_amap_type_filter t ";
        mapList = queryRunner.query(conn,sql,new MapListHandler());
        for (Map<String,Object> map:mapList){
            if (map.get("KEYWORDS") != null && map.get("RULESX") != null && map.get("COLUMN_NAME") != null
                    &&"0".equals(map.get("USED").toString())){
                String keywords = map.get("KEYWORDS").toString();
                String rules = map.get("RULESX").toString();
                String column = map.get("COLUMN_NAME").toString();
                reMap.put("keywords",keywords);
                reMap.put("rules",rules);
                reMap.put("column",column);
                filterMapList.add(reMap);
            }
        }
        return filterMapList;
    }

    /**
     * 构造函数为空
     */
    private PatternAddress(){

    }

    /**
     * 创造单例模式
     * @return
     */
    public static PatternAddress getInstance(){
        if (patternAddress == null){
            synchronized (PatternAddress.class){
                if (patternAddress == null){
                    patternAddress = new PatternAddress();
                }
            }
        }
        return patternAddress;
    }
}
