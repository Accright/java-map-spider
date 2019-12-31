package com.accright.plugins.spider.utils.easyexcel;


import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @Author yuanhaoyue swithaoy@gmail.com
 * @Description 监听类，可以自定义
 * @Date 2018-06-05
 * @Time 16:58
 */
public class ExcelListener<T> extends AnalysisEventListener {

    //自定义用于暂时存储data。
    //可以通过实例获取该值
    private List<T> datas = new ArrayList<T>();

    /**
     * 通过 AnalysisContext 对象还可以获取当前 sheet，当前行等数据
     */
    @Override
    public void invoke(Object object, AnalysisContext context) {
        //根据业务自行 do something
        doSomething(object);
    }

    /**
     * 根据业务自行实现该方法
     */
    private void doSomething(Object object) {
        //数据存储到list，供批量处理，或后续自己业务逻辑处理。
        datas.add((T)object);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        //datas.clear();
    }

    public List<T> getDatas() {
        return datas;
    }

    public void setDatas(List<T> datas) {
        this.datas = datas;
    }
}