package com.yonyoucloud.fi.cmp.paymentbill.service;



import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.paymentbill.service.redis.BankBalanceRedisUtil;
import com.yonyoucloud.fi.cmp.paymentbill.service.redis.ProcessVo;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * desc:银行账户实时余额在线查询异步处理计算进度百分比接口
 * author:wangqiangac
 * date:2023/7/10 19:44
 */
@Component
public class CalcProcessServiceImpl implements CalcProcessService {
    @Autowired
    BankBalanceRedisUtil bankBalanceRedisUtil;
    @Override
    public String getProcess(String uid) {
        ProcessVo vo = bankBalanceRedisUtil.getProcess(uid);
        return CtmJSONObject.toJSONString(vo);
    }

    public String process(String uid) {
        return ProcessUtil.getProcess(uid);
    }


    @Override
    public void testProcess(String uid) throws InterruptedException {

        bankBalanceRedisUtil.initProcess(uid,29);
        for (int i = 0; i < 29; i++) {

//            Thread.sleep(300);
            if( i % 2 == 1){
                bankBalanceRedisUtil.updateProcess(uid,100,false,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400797", "第") /* "第" */+i+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400798", "个失败") /* "个失败" */);
            }else{
                bankBalanceRedisUtil.updateProcess(uid,100,true,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400797", "第") /* "第" */+i+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400799", "个成功") /* "个成功" */);
            }
        }
    }
}
