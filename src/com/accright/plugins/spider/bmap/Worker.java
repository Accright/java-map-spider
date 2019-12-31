package com.accright.plugins.spider.bmap;

public class Worker implements Runnable {

    private String types;//分类
    private String cycleNet;//矩形区域
    private String apikey;//百度的APIkey

    public Worker(String types,String polygon,String apikey){
        this.apikey = apikey;
        this.cycleNet = polygon;
        this.types = types;
    }

    @Override
    public void run() {
        SpiderUtils.insertData(types,cycleNet,apikey);
        System.out.println("该分类下的所有数据已执行完毕："+types+",执行的线程为："+Thread.currentThread().getName());
    }
}
