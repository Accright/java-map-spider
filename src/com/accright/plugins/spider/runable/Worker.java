package com.accright.plugins.spider.runable;

import com.accright.plugins.spider.amap.main.SpiderUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Worker implements Runnable {

    private String types;//分类
    private String cycleNet;//矩形区域
    private String apikey;//高德的APIkey
    private String cityNamePre;//预定城市名称
    private List<String> polygonList;//区域分块列表

    public Worker(String types,String polygon,String apikey,String cityNamePre,List<String> polygonList){
        this.apikey = apikey;
        this.cycleNet = polygon;
        this.types = types;
        this.cityNamePre = cityNamePre;
        this.polygonList = polygonList;
    }

    @Override
    public void run() {
        //先分割 再递归
        //List<String> temp = SpiderUtils.getNetList(29.196169,24.562465,103.446056,109.774697);
        /*for (int i = 0;i < polygonList.size(); i++){
            String polygon = polygonList.get(i);
            SpiderUtils.insertData(types,polygon,apikey,cityNamePre);
            System.out.println("该分类下的区域"+types+"的区域中当前已经处理完："+polygon+",其索引为："+i);
        }*/
        //直接递归查询
        SpiderUtils.insertData(types,cycleNet,apikey,cityNamePre);
        System.out.println("该分类下的所有数据已执行完毕："+types+",执行的线程为："+Thread.currentThread().getName()
                +",当前时间为："+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }
}
