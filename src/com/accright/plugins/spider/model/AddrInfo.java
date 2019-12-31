package com.accright.plugins.spider.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;

public class AddrInfo extends BaseRowModel {
    @ExcelProperty(value ={"longitude"},index = 0)
    private String longitude;

    @ExcelProperty(value ={"latitude"},index =1)
    private String latitude;

    @ExcelProperty(value ={"index"},index =2)
    private String index;

    @ExcelProperty(value ={"zhlabel"},index =3)
    private String zhlabel;

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getZhlabel() {
        return zhlabel;
    }

    public void setZhlabel(String zhlabel) {
        this.zhlabel = zhlabel;
    }
}
