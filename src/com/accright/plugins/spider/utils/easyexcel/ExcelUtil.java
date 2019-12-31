package com.accright.plugins.spider.utils.easyexcel;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.BaseRowModel;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.excel.support.ExcelTypeEnum;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description 工具类
 * @Date 2018-06-06
 * @Time 14:07
 */
public class ExcelUtil {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    /**
     * 读取 Excel(多个 sheet)
     *
     * @param excel    文件
     * @param rowModel 实体类映射，继承 BaseRowModel 类
     * @return Excel 数据 list
     */
    public static <T extends BaseRowModel> List<T> readExcel(File excel, T rowModel) {
        ExcelListener excelListener = new ExcelListener();
        ExcelReader reader = getReader(excel, excelListener);
        if (reader == null) {
            return null;
        }
        for (Sheet sheet : reader.getSheets()) {
            if (rowModel != null) {
                sheet.setClazz(rowModel.getClass());
            }
            reader.read(sheet);
        }
        return excelListener.getDatas();
    }

    /**
     * 读取某个 sheet 的 Excel
     *
     * @param excel    文件
     * @param rowModel 实体类映射，继承 BaseRowModel 类
     * @param sheetNo  sheet 的序号 从1开始
     * @return Excel 数据 list
     */
    public static List<Object> readExcel(File excel, BaseRowModel rowModel, int sheetNo) {
        return readExcel(excel, rowModel, sheetNo, 1);
    }

    /**
     * 读取某个 sheet 的 Excel
     *
     * @param excel       文件
     * @param rowModel    实体类映射，继承 BaseRowModel 类
     * @param sheetNo     sheet 的序号 从1开始
     * @param headLineNum 表头行数，默认为1
     * @return Excel 数据 list
     */
    public static List<Object> readExcel(File excel, BaseRowModel rowModel, int sheetNo,
                                         int headLineNum) {
        ExcelListener excelListener = new ExcelListener();
        ExcelReader reader = getReader(excel, excelListener);
        if (reader == null) {
            return null;
        }
        reader.read(new Sheet(sheetNo, headLineNum, rowModel.getClass()));
        return excelListener.getDatas();
    }

    /**
     * 导出一个sheet
     * @param list		数据
     * @param fileName
     * @param sheetName
     * @param head		表头
     */
    public static void writeExcel(List<List<Object>> list,
            String fileName, String sheetName, List<List<String>> head) {
		ExcelWriter writer = new ExcelWriter(getOutputStream(fileName), ExcelTypeEnum.XLSX);
		Sheet sheet = new Sheet(1, 0);
		sheet.setHead(head);
		sheet.setSheetName(sheetName);
		writer.write1(list, sheet);
		writer.finish();
	}
    
    /**
     * 导出 Excel ：一个 sheet，带表头
     *
     * @param list      数据 list，每个元素为一个 BaseRowModel
     * @param fileName  导出的文件名
     * @param sheetName 导入文件的 sheet 名
     * @param object    映射实体类，Excel 模型
     */
     public static void writeExcel(List<? extends BaseRowModel> list,
                                  String fileName, String sheetName, BaseRowModel object) {
        ExcelWriter writer = new ExcelWriter(getOutputStream(fileName), ExcelTypeEnum.XLSX);
        Sheet sheet = new Sheet(1, 0, object.getClass());
        sheet.setSheetName(sheetName);
        writer.write(list, sheet);
        writer.finish();
    }

    /**
     * 导出 Excel ：多个 sheet，带表头
     *
     * @param list      数据 list，每个元素为一个 BaseRowModel
     * @param fileName  导出的文件名
     * @param sheetName 导入文件的 sheet 名
     * @param object    映射实体类，Excel 模型
     */
    public static ExcelWriterFactroy writeExcelWithSheets(List<? extends BaseRowModel> list,
                                                          String fileName, String sheetName, BaseRowModel object) {
        ExcelWriterFactroy writer = new ExcelWriterFactroy(getOutputStream(fileName), ExcelTypeEnum.XLSX);
        Sheet sheet = new Sheet(1, 0, object.getClass());
        sheet.setSheetName(sheetName);
        writer.write(list, sheet);
        return writer;
    }
    
    /**
     * 多sheet页导出
     * @param fileName	导出的文件名
     * @param list		数据 list，每个元素为一个sheet页内数据
     * @param sheetNames	sheet 名
     * @param objects	映射实体类list
     * @return
     */
     public static void writeExcelWithSheets(String fileName,
                                            List<List<? extends BaseRowModel>> list, List<String> sheetNames, List<BaseRowModel> objects) {
		ExcelWriterFactroy writer = new ExcelWriterFactroy(getOutputStream(fileName), ExcelTypeEnum.XLSX);
		for(int i=0;i<list.size();i++) {
			Sheet sheet = new Sheet((i+1), 0, objects.get(i).getClass());
			sheet.setSheetName(sheetNames.get(i));
			writer.write(list.get(i), sheet);
		}
		writer.finish();
	 }

    /**
     * 导出文件时为Writer生成OutputStream
     */
    private static OutputStream getOutputStream(String fileName) {
        //创建本地文件
        String filePath = fileName + ".xlsx";
        File dbfFile = new File(filePath);
        try {
            if (!dbfFile.exists() || dbfFile.isDirectory()) {
                dbfFile.createNewFile();
            }
            return new FileOutputStream(dbfFile);
        } catch (IOException e) {
            throw new ExcelException("创建文件失败！");
        }
    }
    /**
     * 返回 ExcelReader
     *
     * @param file         需要解析的 Excel 文件
     * @param excelListener new ExcelListener()
     */
    private static ExcelReader getReader(File file,
                                         ExcelListener excelListener) {
        /*String filename = excel.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xls") && !filename.toLowerCase().endsWith(".xlsx"))) {
            throw new ExcelException("文件格式错误！");
        }*/
        InputStream inputStream;
        try {
            //inputStream = excel.getInputStream();
            inputStream = new FileInputStream(file);
            return new ExcelReader(new BufferedInputStream(inputStream), null, excelListener, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String formatDate(Date date) {
    	return sdf.format(date);
    }
}
