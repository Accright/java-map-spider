/**
 * @author Jeff.Wang
 * @Title: ExcelRowBJ
 * @Package: com.accright.plugins.common.spider.utils.easyexcel
 * @version V1.0
 * @date 2019/6/21 11:34
 **/
package com.accright.plugins.spider.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;

public class ExcelRowBJ extends BaseRowModel {
    @ExcelProperty(index = 0)
    private String id;

    @ExcelProperty(index =1)
    private String resourceId;

    @ExcelProperty(index =2)
    private String buildingId;

    @ExcelProperty(index =3)
    private String zhlabel;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
    }

    public String getZhlabel() {
        return zhlabel;
    }

    public void setZhlabel(String zhlabel) {
        this.zhlabel = zhlabel;
    }
}
