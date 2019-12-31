package com.accright.plugins.spider.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {

	/**
	 * 模拟浏览器发送GET请求
	 * @param url,请求的url
	 * @param params,请求参数列表
	 * @return
	 */
	public static String  sendGetRequest(String url,List<NameValuePair> params ){
		if(url==null||"".equals(url)){
			return "";
		}
		String responseBody="";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		InputStream is = null;
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setSocketTimeout(5000)
				.setConnectTimeout(5000)
				.setConnectionRequestTimeout(5000)
				.build();
		try {
			 String qparm = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
			 HttpGet httpGet = new HttpGet(url+"?"+qparm);
			 //模拟浏览器调用
			 httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36");
			 httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7");
			 httpGet.setHeader("Accept-Encoding", "gzip, deflate");
			 httpGet.setHeader("Pragma", "no-cache");
			 httpGet.setConfig(defaultRequestConfig);
			 response = httpclient.execute(httpGet);  
			 //得到响应体  
	         HttpEntity entity = response.getEntity();
	         if(entity != null){  
	                is = entity.getContent();  
	                //转换为字节输入流  
	                BufferedReader br = new BufferedReader(new InputStreamReader(is, Consts.UTF_8));
	                String body = null;  
	                while((body=br.readLine()) != null){  
	                    //System.out.println(body);  
	                    responseBody += body;
	                }  
	            }   
		} catch (Exception e) {
			e.printStackTrace();
		}finally{  
            //关闭输入流，释放资源  
            if(is != null){  
                try {  
                    is.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
            //消耗实体内容  
            if(response != null){  
                try {  
                    response.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
            //关闭相应 丢弃http连接  
            if(httpclient != null){  
                try {  
                	httpclient.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
		return responseBody;
	}
	
	public static String  sendGetRequest(String url, List<NameValuePair> params, Map<String, String> headers){
		if(url==null||"".equals(url)){
			return "";
		}
		String responseBody="";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse response = null;
		InputStream is = null;
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setSocketTimeout(5000)
				.setConnectTimeout(5000)
				.setConnectionRequestTimeout(5000)
				.build();
		try {
			 String qparm = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
			 HttpGet httpGet = new HttpGet(url+"?"+qparm);
			httpGet.setConfig(defaultRequestConfig);
			 //设置header属性，模拟浏览器调用
			 for(String header: headers.keySet())
			 {
				 httpGet.setHeader(header, headers.get(header));
			 }
			 response = httpclient.execute(httpGet);  
			 //得到响应体  
	         HttpEntity entity = response.getEntity();
	         if(entity != null){  
	                is = entity.getContent();  
	                //转换为字节输入流  
	                BufferedReader br = new BufferedReader(new InputStreamReader(is, Consts.UTF_8));
	                String body = null;  
	                while((body=br.readLine()) != null){  
	                    //System.out.println(body);  
	                    responseBody=body;
	                }  
	            }   
		} catch (Exception e) {
			e.printStackTrace();
		}finally{  
            //关闭输入流，释放资源  
            if(is != null){  
                try {  
                    is.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
            //消耗实体内容  
            if(response != null){  
                try {  
                    response.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
            //关闭相应 丢弃http连接  
            if(httpclient != null){  
                try {  
                	httpclient.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
		return responseBody;
	}
}
