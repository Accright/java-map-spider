package com.accright.plugins.spider.handler;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.dbutils.handlers.AbstractListHandler;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

public class MapListHandler extends AbstractListHandler<Map<String, Object>> {

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
        Map<String, Object> result = new CaseInsensitiveMap();
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            //通过ResultSetMetaData类，可判断该列数据类型
            if(rsmd.getColumnTypeName(i).equals("BLOB")){
                Blob bb = rs.getBlob(i);
                if(bb!=null) {
                    byte[] b = bb.getBytes(1, (int)bb.length());
                    String blobStr = "";
                    try {
                        //blobStr = new String(ZipUtil.deflateUncompress(b), "UTF-8");
                        blobStr = new String(b);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    //将结果放到Map中
                    result.put(rsmd.getColumnName(i), blobStr);
                }
            }else {
                //如果不是BLOB类型，则直接放进Map
                result.put(rsmd.getColumnName(i), rs.getObject(i));
            }
        }
        return result;
    }
}