package com.accright.plugins.spider.bmap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BMapSpider {
    public static void main(String args[]){
        String[] querys = {"购物","公司企业","政府机构","出入口","自然地物"};
        String bmapkey = "vkie9s9hRY9xlbMxDuXtXBPajHPhNBuU";
        //传入左下角和右上角
        String bounds = "26.158033,106.141859,27.35546,107.300889";
        ExecutorService executorService = Executors.newFixedThreadPool(querys.length);
        for (String query : querys){
            Worker spiderWorker = new Worker(query,bounds,bmapkey);
            executorService.execute(spiderWorker);
        }
    }
}

