package com.accright.plugins.spider.amap.main;

import com.accright.plugins.spider.pattern.PatternAddress;
import com.accright.plugins.spider.utils.CoordinateTransform;
import com.accright.plugins.spider.utils.DBFactory;
import com.accright.plugins.spider.utils.HttpClientUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.*;

/**
 * @author Jeff.Wang
 * @Title: ExcelRowSX
 * @version V1.0
 * @desc 用于分割网格
 * @date 2019/6/21 14:37
 **/
public class SpiderUtils{
    private static QueryRunner queryRunner = new QueryRunner();
    private static PatternAddress patternAddress = PatternAddress.getInstance();
    private static Map<String,String> cityRelMap;

    //获取百度地图地市关联关系
    static {
        try {
            cityRelMap = patternAddress.getCityRelMap();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取当前线程数据库连接
    private static ThreadLocal<Connection> connHolder = new ThreadLocal<Connection>(){
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

    private static Connection conn;
    static {
        try {
            //conn = DBFactory.getConnection();
            conn = connHolder.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String amapUrl = "http://api.map.baidu.com/place/v2/search";
    private static final String baiduUrl = "https://api.map.baidu.com/";
    private static final String baiduGeoUrl  = "http://api.map.baidu.com/geocoder/v2/";
    private static final String patternUrl = "https://restapi.amap.com/v3/place/text";

    /**
     * 获取所有的纬度分割
     * @param latNw
     * @param latSe
     * @return
     */
    public static List<Double> getLatAll(Double latNw, Double latSe){
        List<Double> allLat = new ArrayList<Double>();
        for (int i = 0;i < (latNw - latSe + 0.000001) / 0.5;i++){
            double lat = latSe + 0.5 * i;
            allLat.add((double)Math.round(lat * 1000000)/1000000);
        }
        allLat.add(latNw);
        Collections.reverse(allLat);//反转纬度List
        return allLat;
    }

    /**
     * 获取所有的经度分割
     * @param lngNw
     * @param lngSe
     * @return
     */
    public static List<Double> getLngAll(Double lngNw,Double lngSe){
        List<Double> allLng = new ArrayList<Double>();
        for (int i = 0;i < (lngSe - lngNw + 0.000001) / 0.5;i++){
            double lng = lngNw + 0.5 * i;
            allLng.add((double)Math.round(lng * 1000000)/1000000);
        }
        allLng.add(lngSe);
        //Collections.reverse(allLng);//反转经度List
        return allLng;
    }

    /**
     * 获取边角的点
     * @param latNw
     * @param latSe
     * @param lngNw
     * @param lngSe
     * @return
     */
    public static List<String> getNetCom(Double latNw, Double latSe,Double lngNw,Double lngSe){
        List<Double> allLat = SpiderUtils.getLatAll(latNw,latSe);
        List<Double> allLng = SpiderUtils.getLngAll(lngNw,lngSe);
        List<String> pointList = new ArrayList<String>();
        for (int i = 0;i < allLat.size();i++){
            double lat = allLat.get(i);
            for (int j = 0;j < allLng.size();j++){
                double lng = allLng.get(j);
                String point = lng+","+lat;
                pointList.add(point);
            }
        }
        return pointList;
    }

    public static List<String> getNetList(Double latNw, Double latSe,Double lngNw,Double lngSe){
        List<Double> allLat = SpiderUtils.getLatAll(latNw,latSe);
        List<Double> allLng = SpiderUtils.getLngAll(lngNw,lngSe);
        List<String> pointList = SpiderUtils.getNetCom(latNw,latSe,lngNw,lngSe);
        List<String> netList = new ArrayList<String>();
        for (int i = 0;i < allLat.size() - 1;i++){
            for (int j = 0 + allLat.size() * i; j < allLng.size() + allLng.size() * i -1; j++){
                String point1 = pointList.get(j);
                String point2 = pointList.get(j + allLng.size() + 1);
                String net = point1 + "|" + point2;
                netList.add(net);
            }
        }
        return netList;
    }

    public static void insertData(String types,String polygon,String apiKey,String cityNamePre){
        int times = 3;
        for (int i = 1;i <= 10;i++){//最大数量为1000 但是200条左右就会报错 所以只查询200条
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("key",apiKey));
            params.add(new BasicNameValuePair("extensions","base"));
            params.add(new BasicNameValuePair("types",types));
            params.add(new BasicNameValuePair("polygon",polygon));
            params.add(new BasicNameValuePair("offset","20"));
            params.add(new BasicNameValuePair("output","json"));
            params.add(new BasicNameValuePair("page",i+""));
            try{
                String responseStrGeoCode = HttpClientUtil.sendGetRequest(amapUrl,params);
                if (StringUtils.isEmpty(responseStrGeoCode)){
                    break;
                }else {
                    JSONObject jsonObject = JSONObject.fromObject(responseStrGeoCode);
                    if (!"1".equals(jsonObject.getString("status"))){
                        System.out.println("请求结果出错 其结果为："+responseStrGeoCode);
                        break;
                    }
                    JSONArray poiJsonArray = jsonObject.getJSONArray("pois");
                    String countNum = jsonObject.getString("count");//当前的数量 保证一个分类每个区域数据不超过100
                    System.out.println("当前线程"+Thread.currentThread().getName()+",当前查询的数量为： "+jsonObject.getString("count")
                            +" , 查询的边界为 :"+polygon+" , 当前页数为： "+i+" ,当前列表的数量为："+poiJsonArray.size()+",当前查询的分类为"+types);
                    if (Integer.valueOf(countNum) > 100){
                        SpiderUtils.dividePylogon(polygon,types,apiKey,cityNamePre);
                        break;
                    }
                    for (int x = 0;x < poiJsonArray.size();x++){
                        JSONObject json = poiJsonArray.getJSONObject(x);
                        String id = json.getString("id");
                        String relatedId = json.getString("parent");
                        String zhLabel = json.getString("name");
                        String type = json.getString("type");
                        String location = json.getString("location");
                        String address = json.getString("address");
                        String tel = json.getString("tel");
                        String longitude = "";
                        String latitude = "";
                        if (!StringUtils.isEmpty(location)){
                            String[] locations = location.split(",");
                            longitude = locations[0];
                            latitude = locations[1];
                        }
                        String pname = json.getString("pname");
                        String cityName = json.getString("cityname");
                        String countyName = json.getString("adname");
                        //转换后的经纬度坐标
                        Double[] gpsPoints = CoordinateTransform.gcj02towgs84(Double.parseDouble(longitude),Double.parseDouble(latitude));
                        String gpsLng = gpsPoints[0]+"";
                        String gpsLat = gpsPoints[1]+"";
                        if (cityName != null && cityNamePre.equals(pname)){// && cityNamePre.equals(cityName)//如果只查询一个地市的地址数据 则使用代码进行限制
                            String cityCode = cityRelMap.get(cityName);
                            Map map = getBaiduAddr(zhLabel,"vkie9s9hRY9xlbMxDuXtXBPajHPhNBuU",cityCode);
                            if (map.get("uid") != null && map.get("addr") != null){
                                //百度范围数据
                                String bmapBoundary = "";
                                if (map.get("poiboundary") != null){
                                    bmapBoundary = map.get("poiboundary").toString();
                                }
                                //处理百度地址
                                String bMapAddr = map.get("addr").toString();
                                Map bAddrMap = patternAddress.getPatterMap(bMapAddr);
                                String bmapProv = "";
                                String bmapCity = "";
                                String bmapCounty = "";
                                String bmapTown = "";
                                String bmapRoad = "";
                                String bmapRoadNum = "";
                                String bmapOtherText = "";
                                if (bAddrMap.get("prov") != null && !"".equals(bAddrMap.get("prov").toString())){
                                    bmapProv = bAddrMap.get("prov").toString();
                                }
                                if(bAddrMap.get("city") != null && !"".equals(bAddrMap.get("city").toString())){
                                    bmapCity = bAddrMap.get("city").toString();
                                }
                                if(bAddrMap.get("county") != null && !"".equals(bAddrMap.get("county").toString())){
                                    bmapCounty = bAddrMap.get("county").toString();
                                }
                                if(bAddrMap.get("road") != null && !"".equals(bAddrMap.get("road").toString())){
                                    bmapRoad = bAddrMap.get("road").toString();
                                }
                                if(bAddrMap.get("roadNum") != null && !"".equals(bAddrMap.get("roadNum").toString())){
                                    bmapRoadNum = bAddrMap.get("roadNum").toString();
                                }
                                if(bAddrMap.get("otherText") != null && !"".equals(bAddrMap.get("otherText").toString())){
                                    bmapOtherText = bAddrMap.get("otherText").toString();
                                }
                                if (!"".equals(bmapRoad) && bmapRoad.contains("[")){
                                    //如果包含[ 说明其区县和路之间有其他层级 需要重新处理该路的split格式

                                    String[] roadP = bmapRoad.split("\\[");
                                    bmapRoad = roadP[1];
                                    //处理可能出现的乡镇信息或街道信息
                                    if (!"".equals(roadP[0])
                                            && (roadP[0].matches(".*街道")
                                            || roadP[0].matches(".*镇")
                                            || roadP[0].matches(".*乡"))){
                                        bmapTown = roadP[0];
                                    }else {
                                        bmapOtherText = roadP[0] + bmapOtherText;
                                    }
                                }
                                //边界信息的坐标转换需要通过JS处理
                                String sql = "insert into t_amap_addr_bj_all (id,related_id,zh_label,types," +
                                        "longitude,latitude,PROV_NAME,city_name,county_name,addr_name,bmap_uid,bmap_zhlabel," +
                                        "bmap_types,bmap_addr,bmap_boundary,BMAP_PROV,BMAP_CITY,BMAP_COUNTY,BMAP_TOWN," +
                                        "BMAP_ROAD,BMAP_ROADNUM,BMAP_OTHERTEXT,gps_longitude,gps_latitude,customer_tel) " +
                                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                                queryRunner.update(conn,sql,id,relatedId,zhLabel,type,longitude,latitude,pname,cityName,countyName,
                                        address,map.get("uid"),map.get("name"),map.get("std_tag"),map.get("addr"),bmapBoundary,
                                        bmapProv,bmapCity,bmapCounty,bmapTown,bmapRoad,bmapRoadNum,bmapOtherText,gpsLng,gpsLat,tel);
                            }else {
                                //如果使用百度的接口查询不出信息 直接使用另一个方法根据经纬度去获取
                                Map geoMap = SpiderUtils.getBaiduGeo(gpsLng,gpsLat,"vkie9s9hRY9xlbMxDuXtXBPajHPhNBuU");
                                String bmapBoundary = "";
                                String bmapProv = geoMap.get("province")+"";
                                String bmapCity = geoMap.get("city")+"";
                                String bmapCounty = geoMap.get("county")+"";
                                String bmapTown = geoMap.get("town")+"";
                                String bmapRoad = geoMap.get("road")+"";
                                String bmapRoadNum = "";
                                String uid = geoMap.get("uid")+"";
                                String name = geoMap.get("name")+"";
                                String tag = geoMap.get("tag")+"";
                                String addr = geoMap.get("address")+"";
                                String bmapOtherText = geoMap.get("otherText")+"";
                                String sql = "insert into t_amap_addr_bj_all (id,related_id,zh_label,types," +
                                        "longitude,latitude,PROV_NAME,city_name,county_name,addr_name,bmap_uid,bmap_zhlabel," +
                                        "bmap_types,bmap_addr,bmap_boundary,BMAP_PROV,BMAP_CITY,BMAP_COUNTY,BMAP_TOWN," +
                                        "BMAP_ROAD,BMAP_ROADNUM,BMAP_OTHERTEXT,gps_longitude,gps_latitude,customer_tel) " +
                                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                                queryRunner.update(conn,sql,id,relatedId,zhLabel,type,longitude,latitude,pname,cityName,countyName,
                                        address,uid,name,tag,addr,bmapBoundary,
                                        bmapProv,bmapCity,bmapCounty,bmapTown,bmapRoad,bmapRoadNum,bmapOtherText,gpsLng,gpsLat,tel);
                            }
                        }
                    }
                    if (poiJsonArray.size() < 20){
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (times <= 0){
                    times = 3;
                    continue;
                }else {
                    i--;
                    times--;
                    continue;
                }
            }
        }
    }

    public static Map getBaiduAddr(String name,String ak,String cityCode){
        Map map = new HashMap();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("qt","s"));
        params.add(new BasicNameValuePair("c",cityCode));
        params.add(new BasicNameValuePair("wd",name));
        params.add(new BasicNameValuePair("rn","2"));
        params.add(new BasicNameValuePair("ie","utf-8"));
        params.add(new BasicNameValuePair("oue","1"));
        params.add(new BasicNameValuePair("fromproduct","jsapi"));
        params.add(new BasicNameValuePair("res","api"));
        params.add(new BasicNameValuePair("callback",""));
        params.add(new BasicNameValuePair("ak",ak));
        String responseStrGeoCode = HttpClientUtil.sendGetRequest(baiduUrl,params);
        JSONObject jsonObject = JSONObject.fromObject(responseStrGeoCode);
        if (jsonObject.get("content") != null && jsonObject.get("content") instanceof JSONArray){
            JSONObject json = jsonObject.getJSONArray("content").getJSONObject(0);
            if (json != null && json.get("address_norm") != null){
                String uid = json.getString("uid");
                String addr = json.getString("address_norm");
                String bName = json.getString("name");
                String tags = json.getString("std_tag");
                map.put("uid",uid);
                map.put("addr",addr);
                map.put("name",bName);
                map.put("std_tag",tags);
                if (json.get("ext") != null && json.get("ext") instanceof JSONObject
                        && json.getJSONObject("ext").get("detail_info") != null && json.getJSONObject("ext").get("detail_info") instanceof JSONObject
                        && json.getJSONObject("ext").getJSONObject("detail_info").get("guoke_geo") != null && json.getJSONObject("ext").getJSONObject("detail_info").get("guoke_geo") instanceof JSONObject
                        && json.getJSONObject("ext").getJSONObject("detail_info").getJSONObject("guoke_geo").get("geo") != null){
                    String poiboundary = json.getJSONObject("ext").getJSONObject("detail_info").getJSONObject("guoke_geo").getString("geo");
                    map.put("poiboundary",poiboundary);
                    //System.out.println("该名称《"+name+"》下可以查询出边界数据为："+poiboundary);
                }
            }
        }
        return map;
    }

    public static Map getBaiduGeo(String lng,String lat,String ak){
        Map map = new HashMap();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("coordtype", "wgs84ll"));
        params.add(new BasicNameValuePair("location", lat + "," + lng));
        params.add(new BasicNameValuePair("pois", "1"));
        params.add(new BasicNameValuePair("latest_admin", "1"));
        params.add(new BasicNameValuePair("output", "json"));
        params.add(new BasicNameValuePair("ak", ak));
        String responseStr = HttpClientUtil.sendGetRequest(baiduGeoUrl, params);
        if (StringUtils.isEmpty(responseStr)) {
            return null;
        }
        JSONObject jsonObj = JSONObject.fromObject(responseStr);
        JSONObject resultJson = jsonObj.getJSONObject("result");
        String formatted_address = resultJson.getString("formatted_address");//格式化后的地址
        String sematic_description = resultJson.getString("sematic_description");//地址附加描述
        JSONObject addressComponent = resultJson.getJSONObject("addressComponent");
        String province = "山西省"; // 1级-省份
        String city = addressComponent.getString("city"); // 2级-地市
        String county = addressComponent.getString("district"); // 3级-区县
        String town = addressComponent.getString("town"); // 4级-乡镇街道
        String street = addressComponent.getString("street"); // 5级-道路
        String name = "";
        String tag = "";
        String uid = "";
        JSONArray jsonPOIs = resultJson.getJSONArray("pois");
        if (jsonPOIs.size() > 0) {
            JSONObject jsonPoi = jsonPOIs.getJSONObject(0);
            name = jsonPoi.getString("name"); // 6级-区域
            tag = jsonPoi.getString("tag"); //百度的分类
            uid = jsonPoi.getString("uid");//百度的uid
        }
        map.put("province",province);
        map.put("city",city);
        map.put("county",county);
        map.put("town",town);
        map.put("road",street);
        map.put("name",name);
        map.put("tag",tag);
        map.put("address",formatted_address);
        map.put("otherText",sematic_description);
        map.put("uid",uid);
        return map;
    }

    /**
     * 区块的划分
     * @param polygon
     */
    public static void dividePylogon(String polygon,String types,String apikey,String cityNamePre){
        System.out.println("当前线程"+Thread.currentThread().getName()+",当前的区域："+polygon+" 进入递归");
        String[] points = polygon.split("\\|");
        String[] lngLat1 = points[0].split(",");
        String[] lngLat2 = points[1].split(",");
        String lng1 = lngLat1[0];//左上角经度
        String lng2 = lngLat2[0];//右下角经度
        String lat1 = lngLat1[1];//左上角纬度
        String lat2 = lngLat2[1];//右下角纬度
        //保留小数点后6位
        double tempLng = (Double.valueOf(lng1) + Double.valueOf(lng2)) / 2;
        double tempLat = (Double.valueOf(lat1) + Double.valueOf(lat2)) / 2;
        BigDecimal  tempLngD = new BigDecimal(tempLng);
        BigDecimal  tempLatD = new BigDecimal(tempLat);
        String lng3 = tempLngD.setScale(6,RoundingMode.HALF_UP).doubleValue() + "";//中心点经度
        String lat3 = tempLatD.setScale(6,RoundingMode.HALF_UP).doubleValue() + "";//中心点纬度
        //递归查询
        String polygon1 = lng1+","+lat1+"|"+lng3+","+lat3;
        String polygon2 = lng3+","+lat1+"|"+lng2+","+lat3;
        String polygon3 = lng1+","+lat3+"|"+lng3+","+lat2;
        String polygon4 = lng3+","+lat3+"|"+lng2+","+lat2;
        SpiderUtils.insertData(types,polygon1,apikey,cityNamePre);
        SpiderUtils.insertData(types,polygon2,apikey,cityNamePre);
        SpiderUtils.insertData(types,polygon3,apikey,cityNamePre);
        SpiderUtils.insertData(types,polygon4,apikey,cityNamePre);
    }

    /**
     * 根据地址解析高德INT
     * @param zhLabel
     * @param key
     * @return
     */
    public static Map<String,String> getNetId(String zhLabel,String key){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key",key));
        params.add(new BasicNameValuePair("keywords",zhLabel));
        params.add(new BasicNameValuePair("city","北京"));
        params.add(new BasicNameValuePair("citylimit","true"));
        Map<String,String> map = new HashMap<>(2);
        try {
            String responseStrGeoCode = HttpClientUtil.sendGetRequest(patternUrl, params);
            if (StringUtils.isEmpty(responseStrGeoCode)) {
                return null;
            } else {
                JSONObject jsonObject = JSONObject.fromObject(responseStrGeoCode);
                if (!"1".equals(jsonObject.getString("status"))){
                    System.out.println("请求结果出错 其结果为："+responseStrGeoCode);
                    return null;
                }
                JSONArray poiJsonArray = jsonObject.getJSONArray("pois");
                if (poiJsonArray != null && poiJsonArray.size() > 0){
                    JSONObject poiJson = poiJsonArray.getJSONObject(0);
                    map.put("netId",poiJson.getString("id"));
                    map.put("zhLabel",poiJson.getString("name"));
                    map.put("netAddress",poiJson.getString("address"));
                    return map;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
