package com.accright.plugins.spider.bmap;

import com.accright.plugins.spider.utils.DBFactory;
import com.accright.plugins.spider.utils.HttpClientUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.*;

//用于分割网格
public class SpiderUtils{
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

    private static final String url = "http://api.map.baidu.com/place/v2/search";
    private static final String baiduUrl = "https://api.map.baidu.com/";
    private static final String patternUrl = "https://restapi.amap.com/v3/place/text";


    /**
     * 获取所有的纬度分割
     * @param latNw
     * @param latSe
     * @return
     */
    public static List<Double> getLatAll(Double latNw, Double latSe){
        List<Double> allLat = new ArrayList<Double>();
        for (int i = 0;i < (latNw - latSe + 0.000001) / 0.1;i++){
            double lat = latSe + 0.1 * i;
            allLat.add((double)Math.round(lat * 100000)/100000);
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
        for (int i = 0;i < (lngSe - lngNw + 0.0001) / 0.1;i++){
            double lng = lngNw + 0.1 * i;
            allLng.add((double)Math.round(lng * 100000)/100000);
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

    public static void insertData(String query,String bounds,String apiKey){
        int times = 3;
        for (int i = 0;i < 10;i++){//最大数量为1000 所以只查询1000条
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("ak",apiKey));
            params.add(new BasicNameValuePair("scope","1"));
            params.add(new BasicNameValuePair("query",query));
            params.add(new BasicNameValuePair("bounds",bounds));
            params.add(new BasicNameValuePair("page_size","20"));
            params.add(new BasicNameValuePair("output","json"));
            params.add(new BasicNameValuePair("page_num",i+""));
            try{
                String responseStrGeoCode = HttpClientUtil.sendGetRequest(url,params);
                if (StringUtils.isEmpty(responseStrGeoCode)){
                    break;
                }else {
                    JSONObject jsonObject = JSONObject.fromObject(responseStrGeoCode);
                    if (!"0".equals(jsonObject.getString("status"))){
                        System.out.println("百度地图-请求结果出错 其结果为："+responseStrGeoCode);
                        break;
                    }
                    JSONArray poiJsonArray = jsonObject.getJSONArray("results");
                    String countNum = jsonObject.getString("total");//当前的数量 保证一个分类每个区域数据不超过200
                    System.out.println("百度地图-当前线程"+Thread.currentThread().getName()+",当前查询的数量为： "+jsonObject.getString("total")
                            +" , 查询的边界为 :"+bounds+" , 当前页数为： "+i+" ,当前列表的数量为："+poiJsonArray.size());
                    if (Integer.valueOf(countNum) > 200){
                        SpiderUtils.dividePylogon(bounds,query,apiKey);
                        break;
                    }
                    for (int x = 0;x < poiJsonArray.size();x++){
                        JSONObject json = poiJsonArray.getJSONObject(x);
                        String id = json.getString("uid");
                        //String relatedId = json.getString("parent");
                        String relatedId = "";
                        String zhLabel = json.getString("name");
                        String type = query;//json.getString("type");
                        JSONObject location = json.getJSONObject("location");
                        String address = json.getString("address");
                        String longitude = "";
                        String latitude = "";
                        if (location != null){
                            longitude = location.getString("lng");
                            latitude = location.getString("lat");
                        }
                        String pname = json.getString("province");
                        String cityName = json.getString("city");
                        String countyName = json.getString("area");
                        //String id = json.getString("id");
                        if (cityName != null && "贵阳市".equals(cityName)){
                            Map map = getBaiduAddr(zhLabel,"");
                            if (map.get("uid") != null && map.get("addr") != null){
                                if (map.get("poiboundary") != null && !"".equals(map.get("poiboundary"))){
                                    String sql = "insert into t_bmap_addr_temp (id,related_id,zh_label,types," +
                                            "longitude,latitude,city_name,county_name,addr_name,bmap_uid,bmap_addr,bmap_boundary) " +
                                            "values(?,?,?,?,?,?,?,?,?,?,?,?)";
                                    queryRunner.update(conn,sql,id,relatedId,zhLabel,type,longitude,latitude,cityName,countyName,
                                            address,map.get("uid"),map.get("addr"),map.get("poiboundary"));
                                }else {
                                    String sql = "insert into t_bmap_cus_temp (id,related_id,zh_label,types," +
                                            "longitude,latitude,city_name,county_name,addr_name,bmap_uid,bmap_addr,bmap_boundary) " +
                                            "values(?,?,?,?,?,?,?,?,?,?,?,?)";
                                    queryRunner.update(conn,sql,id,relatedId,zhLabel,type,longitude,latitude,cityName,countyName,
                                            address,map.get("uid"),map.get("addr"),"");
                                }
                            }else {
                                String sql = "insert into t_bmap_cus_temp (id,related_id,zh_label,types," +
                                        "longitude,latitude,city_name,county_name,addr_name,bmap_uid,bmap_addr,bmap_boundary) " +
                                        "values(?,?,?,?,?,?,?,?,?,?,?,?)";
                                queryRunner.update(conn,sql,id,relatedId,zhLabel,type,longitude,latitude,cityName,countyName,
                                        address,"","","");
                            }
                        }
                    }
                    if (poiJsonArray.size() < 20){
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                if (times < 0){
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

    public static Map getBaiduAddr(String name,String ak){
        Map map = new HashMap();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("qt","s"));
        params.add(new BasicNameValuePair("c","146"));
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
                map.put("uid",uid);
                map.put("addr",addr);
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

    /**
     * 区块的划分
     * @param polygon
     */
    public static void dividePylogon(String polygon,String query,String apikey){
        System.out.println("百度地图-当前线程"+Thread.currentThread().getName()+",当前的区域："+polygon+" 进入递归");
        String[] points = polygon.split(",");
        //String[] lngLat1 = points[0].split(",");
        //String[] lngLat2 = points[1].split(",");
        String lng1 = points[1];//左下角经度
        String lng2 = points[3];//右上角经度
        String lat1 = points[0];//左下角纬度
        String lat2 = points[2];//右上角纬度
        //保留小数点后6位
        double tempLng = (Double.valueOf(lng1) + Double.valueOf(lng2)) / 2;
        double tempLat = (Double.valueOf(lat1) + Double.valueOf(lat2)) / 2;
        BigDecimal  tempLngD = new BigDecimal(tempLng);
        BigDecimal  tempLatD = new BigDecimal(tempLat);
        String lng3 = tempLngD.setScale(6,RoundingMode.HALF_UP).doubleValue() + "";//中心点经度
        String lat3 = tempLatD.setScale(6,RoundingMode.HALF_UP).doubleValue() + "";//中心点纬度
        //递归查询
        String bounds1 = lat1+","+lng1+","+lat3+","+lng3;
        String bounds2 = lat3+","+lng1+","+lat2+","+lng3;
        String bounds3 = lat1+","+lng3+","+lat3+","+lng2;
        String bounds4 = lat3+","+lng3+","+lat2+","+lng2;
        SpiderUtils.insertData(query,bounds1,apikey);
        SpiderUtils.insertData(query,bounds2,apikey);
        SpiderUtils.insertData(query,bounds3,apikey);
        SpiderUtils.insertData(query,bounds4,apikey);
    }
}
