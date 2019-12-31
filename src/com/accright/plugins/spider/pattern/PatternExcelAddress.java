/**
 * @author Jeff.Wang
 * @Title: PatternExcelAddress
 * @Description: 根据Excel的内容反爬信息
 * @version V1.0
 * @date 2019/6/21 11:26
 **/
package com.accright.plugins.spider.pattern;

import com.accright.plugins.spider.amap.main.SpiderUtils;
import com.accright.plugins.spider.model.ExcelRowBJ;
import com.accright.plugins.spider.utils.DBFactory;
import com.accright.plugins.spider.utils.CommonUtils;
import com.accright.plugins.spider.utils.easyexcel.ExcelUtil;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class PatternExcelAddress {

    public static QueryRunner queryRunner = new QueryRunner();
    public static ThreadLocal<Connection> connHolder = new ThreadLocal<Connection>(){
        @Override
        protected Connection initialValue() {
            try {
                return DBFactory.getConnection();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    };
    public static Connection conn;
    static {
        try {
            conn = connHolder.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String,String[]> reMap = null;
    public static List<Map<String,String>> filterMapL = null;

    /**
     * 根据Excel信息反爬高德地图的INT_ID数据
     * @param filePath
     */
    public void patternExcel(String filePath){
        //读取Excel
        File excelFile = new File(filePath);
        try {
            List<ExcelRowBJ> resultList = ExcelUtil.readExcel(excelFile,new ExcelRowBJ());
            String insertSql = " INSERT INTO　T_RECAP_PATTERN_EXCEL_BJ(RESOURCE_ID,NET_ID,BUILDING_ID,ZH_LABEL,REMARK,NET_ZHLABEL,SEARCH_TEXT,NET_ADDRESS) VALUES (?,?,?,?,?,?,?,?)";
            if (resultList != null && resultList.size() > 0){
                for (int index = 0;index < resultList.size();index ++ ){
                    ExcelRowBJ excelRowBJ = resultList.get(index);
                    String zhLable = excelRowBJ.getZhlabel();
                    String resultValue = "";
                    //开始规则的判断
                    if (!StringUtils.isEmpty(zhLable)){
                        String[] patternZhLable = zhLable.split("-");
                        int possibleIndex = patternZhLable.length - 1;
                        String possibleValue = patternZhLable[possibleIndex];
                        while ((CommonUtils.hasDigit(possibleValue) || CommonUtils.hasUpperChar(possibleValue))&& possibleValue.length() < 5){
                            if (possibleIndex > 0){
                                possibleIndex--;
                            }else {
                                break;
                            }
                            possibleValue = patternZhLable[possibleIndex];
                        }
                        if (possibleValue.contains("（") || possibleValue.contains("(")){
                            String[] resultValues = possibleValue.split("（");
                            if (resultValues == null || resultValues.length == 0){
                                resultValues = possibleValue.split("\\(");
                            }
                            possibleValue = resultValues[0];
                        }
                        resultValue = possibleValue;
                        //根据resultValue进行反爬取
                        Map<String,String> resultMap =  SpiderUtils.getNetId(resultValue,CommonUtils.amapKey);
                        String netId = resultMap == null ? "":resultMap.get("netId");
                        String zhLabel = resultMap == null ? "":resultMap.get("zhLabel");
                        String netAddress = resultMap == null ? "":resultMap.get("netAddress");
                        try{
                            queryRunner.update(conn,insertSql,excelRowBJ.getResourceId(),netId,excelRowBJ.getBuildingId(),excelRowBJ.getZhlabel(),"",zhLabel,resultValue,netAddress);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }else {
                System.out.println("Excel 无数据!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        PatternExcelAddress patternExcelAddress = new PatternExcelAddress();
        patternExcelAddress.patternExcel("D:\\楼宇数据-pattern.xlsx");
    }

}
