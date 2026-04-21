package com.yonyoucloud.fi.cmp.balanceadjust.service.impl;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoVO;

import java.util.Map;

/**
 * @description: 自动生成月末余额调节表,专用接口
 * @author: wanxbo@yonyou.com
 * @date: 2025/10/30 10:05
 */

public interface BalanceAdjustAutoGenerateService {

    /**
     * 调度任务：自动生成月末余额调节表，具体执行接口
     * @param param 调度任务参数，具体参照 CM34_自动生成月末余额调节表；
     * @return 执行状态
     * @throws Exception
     */
    boolean generateMonthEndBalanceAdjust(CtmJSONObject param) throws Exception;


    /**
     * 按照对账截止日期生成余额调节表，具体实现
     * 可以生成的条件：1不包含未达项；2余额已平* 余额调节表默认自动提交
     * @param bankAccountInfoVO 银行账户信息
     * @param checkEndDate 月末日期，对账截止日期
     * @param logParams 业务日志参数
     * @return 是否生成成功
     * @throws Exception
     */
    boolean generateBalanceAdjust(BankAccountInfoVO bankAccountInfoVO, String checkEndDate, CtmJSONObject logParams) throws Exception;

    /**
     * 预警任务：预警未生成余额调节表，具体执行接口
     * @param param 预警任务参数，具体参照 未生成月末余额调节表；
     * @return 预警的具体数据，参照预警任务的结果集
     * @throws Exception
     */
    Map<String, Object> warningMonthEndUngenerated(CtmJSONObject param) throws Exception;
}
