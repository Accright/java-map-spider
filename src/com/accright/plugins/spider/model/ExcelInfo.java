package com.accright.plugins.spider.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;

public class ExcelInfo extends BaseRowModel {
    @ExcelProperty(value = "高德ID" ,index = 0)
    private String ID;

    @ExcelProperty(value = "地址名称",index = 1)
    private String ZH_LABEL;

    @ExcelProperty(value = "高德地址详情",index = 2)
    private String ADDR_NAME;

    @ExcelProperty(value = "百度地址详情",index = 3)
    private String BMAP_ADDR;

    /*@ExcelProperty(value = "地址边界",index = 3)
    private String BMAP_BOUNDARY;*/

    @ExcelProperty(value = "高德地址类型",index = 4)
    private String TYPES;

    @ExcelProperty(value = "百度地址类型",index = 5)
    private String BMAP_TYPES;

    public ExcelInfo(){

    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getZH_LABEL() {
        return ZH_LABEL;
    }

    public void setZH_LABEL(String ZH_LABEL) {
        this.ZH_LABEL = ZH_LABEL;
    }

    /*public String getBMAP_BOUNDARY() {
        return BMAP_BOUNDARY;
    }

    public void setBMAP_BOUNDARY(String BMAP_BOUNDARY) {
        this.BMAP_BOUNDARY = BMAP_BOUNDARY;
    }*/

    public String getTYPES() {
        return TYPES;
    }

    public void setTYPES(String TYPES) {
        this.TYPES = TYPES;
    }

    public String getBMAP_TYPES() {
        return BMAP_TYPES;
    }

    public void setBMAP_TYPES(String BMAP_TYPES) {
        this.BMAP_TYPES = BMAP_TYPES;
    }

    public String getADDR_NAME() {
        return ADDR_NAME;
    }

    public void setADDR_NAME(String ADDR_NAME) {
        this.ADDR_NAME = ADDR_NAME;
    }

    public String getBMAP_ADDR() {
        return BMAP_ADDR;
    }

    public void setBMAP_ADDR(String BMAP_ADDR) {
        this.BMAP_ADDR = BMAP_ADDR;
    }
}
