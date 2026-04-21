package com.yonyoucloud.fi.cmp.util;

import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.util.*;


/**
 * 疑似重复设置
 */
@Component
public class CtmDealDetailCheckMayRepeatUtils {
    public static final Short  MAY_REPEAT = 1; // 疑似重复
    // 疑重变量获取和初始化
    public  static boolean isRepeatCheck;

    // 是否使用布隆过滤器进行疑重
    @Value("${cmp.bankDetail.CheckRepeat.isUseCuckoo:false}")
    public static boolean isUseCuckoo;

    @Autowired
    @Value("${cmp.bankDetail.isRepeatCheck:false}")
    public boolean isRepeatCheckInstance;

    public  static Integer selectDayCount;
    @Autowired
    @Value("${cmp.bankdetail.selectDayCount:35}")
    private Integer selectDayCountInstance;

    // 四元素外增加的判断疑重规则
    public static String repeatAddFactors;
    @Value("${cmp.bankDetail.repeatAddFactors:bankaccount,tran_date,tran_amt,dc_flag}")
    public String repeatAddFactorsInstance;

    public static List<String> repeatFactors = new ArrayList<>();
    // 疑重要素数量
    public static  int repeatFactorCount;

    @PostConstruct
    public void  init () {
        CtmDealDetailCheckMayRepeatUtils.selectDayCount = selectDayCountInstance;
        CtmDealDetailCheckMayRepeatUtils.isRepeatCheck = isRepeatCheckInstance;
        CtmDealDetailCheckMayRepeatUtils.repeatAddFactors = repeatAddFactorsInstance;

        // 根据“,”分隔后期疑重规则配置内容项
        // 分隔增加疑重要素字段
        String[] factors = repeatAddFactors.split(",");
        repeatFactors.addAll(Arrays.asList(factors));
        // 设定疑重要素规则
        if (repeatAddFactors.equals(BankDealDetailConst.REPEATFACTIRS)) {
            repeatFactorCount = BankDealDetailConst.REPEAT_FACTIRS_4;
        } else {
            repeatFactorCount = BankDealDetailConst.REPEAT_FACTIRS_DEFINE;
        }
    }

}
