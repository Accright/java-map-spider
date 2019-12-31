/**
 * @author Jeff.Wang
 * @Title: CitySpideer
 * @Package: com.accright.plugins.common.util.spider.amap
 * @version V1.0
 * @date 2019/6/15 14:40
 **/
package com.accright.plugins.spider.amap.main;

import com.accright.plugins.spider.utils.DBFactory;
import com.accright.plugins.spider.utils.HttpClientUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Jeff.Wang
 * @Title: ExcelRowSX
 * @version V1.0
 * @date 2019/6/21 14:37
 **/
public class CitySpider {
    //爬取高德地图的地市区县乡镇
    public static final String amapUrl = "https://restapi.amap.com/v3/config/district";

    public static final String apiKey = "";

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

    public void spiderCity(String page){
        List<NameValuePair> param = new ArrayList<>();
        param.add(new BasicNameValuePair("keywords","010"));
        param.add(new BasicNameValuePair("subdistrict","3"));
        param.add(new BasicNameValuePair("extensions","all"));
        param.add(new BasicNameValuePair("filter","110000"));
        param.add(new BasicNameValuePair("key",apiKey));
        param.add(new BasicNameValuePair("page",page));
        String responseStr = HttpClientUtil.sendGetRequest(amapUrl,param);
        try{
            JSONObject jsonObject = JSONObject.fromObject(responseStr);
            if (jsonObject.has("status") && "1".equals(jsonObject.getString("status"))){
                JSONArray districts = jsonObject.getJSONArray("districts");
                JSONArray jsonArray = new JSONArray();
                jsonArray.add(districts.get(0));
                this.processJson(jsonArray);
                if (districts.size() >= 20){
                    this.spiderCity(String.valueOf(Integer.parseInt(page) + 1));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void processJson(JSONArray districts) throws SQLException {
        if (districts != null && districts.size() > 0){
            //循环里的SQL应该都是相同的
            String tableName = "t_recap_city";//默认为市级表
            Object[][] param = new Object[districts.size()][7];
            for (int i = 0;i < districts.size();i++){
                JSONObject json = districts.getJSONObject(i);
                if (json != null){
                    String citycode = json.has("citycode")?json.getString("citycode"):"";
                    String adcode = json.has("adcode")?json.getString("adcode"):"";
                    String name = json.has("name")?json.getString("name"):"";
                    String polyline = json.has("polyline")?json.getString("polyline"):"";
                    String center = json.has("center")?json.getString("center"):"";
                    String longitude = center == null?center.split(",")[0]:"";
                    String latitude = center == null?center.split(",")[1]:"";
                    if ("province".equals(json.getString("level"))){
                        tableName = "t_recap_skip";
                    }else if ("city".equals(json.getString("level"))){
                        tableName = "t_recap_city";
                        param[i] = new Object[]{citycode,citycode,adcode,name,0,longitude,latitude};
                    }else if ("district".equals(json.getString("level"))){
                        tableName = "t_recap_county";
                        param[i] = new Object[]{adcode,citycode,adcode,name,0,longitude,latitude};
                    }else if ("street".equals(json.getString("level"))){
                        tableName = "t_recap_town";
                        param[i] = new Object[]{UUID.randomUUID().toString().replaceAll("-", ""),citycode,adcode,name,0,longitude,latitude};
                    }
                    if (json.has("districts") && json.getJSONArray("districts") != null && json.getJSONArray("districts").size() > 0){
                        processJson(json.getJSONArray("districts"));
                    }
                }
            }
            if (!"t_recap_skip".equals(tableName)){
                String sql = "insert into "+tableName+" (int_id,city_id,county_id,zh_label,stateflag,longitude,latitude) values (?,?,?,?,?,?,?)";
                int[] row = queryRunner.batch(conn,sql,param);
                System.out.println("The table is "+tableName+",the num is"+Arrays.toString(row));
            }
        }
    }

    public static void main(String[] args){
        CitySpider spideer = new CitySpider();
        spideer.spiderCity("1");
    }

}
