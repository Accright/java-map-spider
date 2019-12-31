package com.accright.plugins.spider.amap.main;

import com.accright.plugins.spider.runable.Worker;
import com.accright.plugins.spider.utils.CommonUtils;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Jeff.Wang
 * @Title: ExcelRowSX
 * @version V1.0
 * @date 2019/6/21 14:37
 **/
public class AMapSpider {
    public static void main(String args[]){
        //为了避免并发超限 每次获取5个大分类左右
        //String[] types = {"010000","020000","030000","040000","050000"};
        //String[] types = {"060100","060200","060300","060400","060500","060600","060700",
                        //"060800", "060900","061000","061100","061200","061300","061400"};
        //String[] types = {"070000","080000","090000","100000","110000","120000","130000"};
        //String[] types = {"070000","140000","150000","160000","170000","180000"};
        //String[] types = {"190000","200000","970000","990000"};
        //String[] types = {"190000"};
        String[] types = {"120200","190400"};
        String amapkey = CommonUtils.amapKey;//30W请求数
        //String polygon = "106.126088,27.289100|107.266844,26.169422";//这是贵阳市的区域范围
        //String polygon = "103.446056,29.196169|109.774697,24.562465";//这是贵州省的区域范围
        //String polygon = "110.180859,40.809866|114.706602,34.483116";//这是山西省的区域范围
        String polygon = "115.408278,41.075208|117.537765,39.41886";//这是北京市的区域范围
        String cityNamePre = "北京市";
        List<String> temp = new ArrayList<String>();
        for (String type : types){
            ExecutorService executorService = Executors.newFixedThreadPool(types.length);
            Worker spiderWorker = new Worker(type,polygon,amapkey,cityNamePre,temp);
            executorService.execute(spiderWorker);
        }
    }
}

