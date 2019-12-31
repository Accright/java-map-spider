package com.accright.plugins.spider.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HttpClientSSLUtil {
    public static String doPost(String url, Map<String,String> map, String charset,Map<String,String> headers){
        HttpClient httpClient = null;
        HttpPost httpPost = null;
        String result = null;
        try{
            httpClient = new SSLClient();
            httpPost = new HttpPost(url);
            InputStream is = null;
            //设置参数
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator iterator = map.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String,String> elem = (Map.Entry<String, String>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(),elem.getValue()));
            }
            if(list.size() > 0){
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list,charset);
                httpPost.setEntity(entity);
            }
            HttpResponse response = httpClient.execute(httpPost);
            if(response != null){
                HttpEntity entity = response.getEntity();
                if(entity != null){
                    //result = EntityUtils.toString(resEntity,charset);
                    GZIPInputStream gZIPInputStream = null;
                    BufferedReader br = null;
                    is = entity.getContent();
                    if ("gzip".equals(entity.getContentEncoding())){
                        gZIPInputStream = new GZIPInputStream(is);
                        InputStreamReader inputStreamReader = new InputStreamReader(gZIPInputStream, Consts.UTF_8);
                        br = new BufferedReader(inputStreamReader);
                    }else {
                        //转换为字节输入流
                        br = new BufferedReader(new InputStreamReader(is, Consts.UTF_8));
                    }
                    String body = null;
                    while((body=br.readLine()) != null){
                        //System.out.println(body);
                        result=body;
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }

    public static String doGet(String url, Map<String,String> map, String charset,Map<String,String> headers){
        HttpClient httpClient = null;
        HttpGet httpGet = null;
        String result = null;
        try{
            httpClient = new SSLClient();
            //设置参数
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator iterator = map.entrySet().iterator();
            InputStream is = null;
            while(iterator.hasNext()){
                Map.Entry<String,String> elem = (Map.Entry<String, String>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(),elem.getValue()));
            }
            if(list.size() > 0){
                //UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list,charset);
                //httpGet.setEntity(entity);
                String qparm = EntityUtils.toString(new UrlEncodedFormEntity(list, Consts.UTF_8));
                System.out.println("高德地图详情信息URL is"+url+"?"+qparm);
                httpGet = new HttpGet(url+"?"+qparm);
            }else {
                httpGet = new HttpGet(url);
            }
            //设置header属性，模拟浏览器调用
            for(String header: headers.keySet())
            {
                httpGet.setHeader(header, headers.get(header));
            }
            HttpResponse response = httpClient.execute(httpGet);
            if(response != null){
                HttpEntity entity = response.getEntity();
                if(entity != null){
                    GZIPInputStream gZIPInputStream = null;
                    BufferedReader br = null;
                    is = entity.getContent();
                    String xxx = entity.getContentEncoding().getValue();
                    if ("gzip".equals(xxx)){
                        gZIPInputStream = new GZIPInputStream(is);
                        InputStreamReader inputStreamReader = new InputStreamReader(gZIPInputStream, Consts.UTF_8);
                        br = new BufferedReader(inputStreamReader);
                    }else {
                        //转换为字节输入流
                        br = new BufferedReader(new InputStreamReader(is, Consts.UTF_8));
                    }
                    String body = null;
                    while((body=br.readLine()) != null){
                        //System.out.println(body);
                        result=body;
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }
}
