/**
 * @author Jeff.Wang
 * @Title: CommonUtils
 * @Package: com.accright.plugins.common.spider.utils
 * @version V1.0
 * @date 2019/6/21 14:54
 **/
package com.accright.plugins.spider.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {

    private static Pattern numberPattern = Pattern.compile(".*\\d+.*");

    public static String amapKey = "TEST";

    /**
     * 判断是否包含数字
     * @param content
     * @return
     */
    public static boolean hasDigit(String content) {
        boolean flag = false;
        Matcher m = numberPattern.matcher(content);
        if (m.matches()) {
            flag = true;
        }
        return flag;
    }

    /**
     * 判断一个字符是否都为数字
     * @param strNum
     * @return
     */
    public static boolean isDigit(String strNum){
        return strNum.matches("[0-9]{1,}");
    }

    /**
     * 判断是否包含大写字母
     * @param content
     * @return
     */
    public static boolean hasUpperChar(String content){
        return content.matches(".*[A-Z]+.*");
    }


}
