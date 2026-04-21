package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.IBankrecRuleEngineService;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.AbstractCmpRuleEngine;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleEngineA;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleEngineB;
import com.yonyoucloud.fi.cmp.bankreconciliation.ruleengine.CmpRuleEngineC;
import com.yonyoucloud.fi.cmp.constant.CmpRuleEngineTypeConstant;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description: 银行对账单规则执行实现类
 * @Author: gengrong
 * @createTime: 2022/9/29
 * @version: 1.0
 */
@Service
public class BankrecRuleEngineServiceImpl implements IBankrecRuleEngineService, ApplicationContextAware {

    private AbstractCmpRuleEngine cmpRuleEngine;

    private ApplicationContext applicationContext;

    @Override
    public void executeRuleEngine(List<BankReconciliation> bankrecList, String ruleType,
                                  boolean isReturnMsg) throws Exception {
        // ruleType ：cmp_identification-辨识规则，cmp_freeze-冻结规则，cmp_generate-生单规则
        if (ruleType != null) {
            // 设置类型
            switch (ruleType) {
                case CmpRuleEngineTypeConstant.cmp_identification:
                    cmpRuleEngine = applicationContext.getBean(CmpRuleEngineA.class);
                    break;
                case CmpRuleEngineTypeConstant.cmp_freeze:
                    cmpRuleEngine = applicationContext.getBean(CmpRuleEngineB.class);
                    break;
                case CmpRuleEngineTypeConstant.cmp_generate:
                    cmpRuleEngine = applicationContext.getBean(CmpRuleEngineC.class);
                    break;
                default:
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100444"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049B","规则类型ruleType参数有误，请检查！") /* "规则类型ruleType参数有误，请检查！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100444"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418049B","规则类型ruleType参数有误，请检查！") /* "规则类型ruleType参数有误，请检查！" */);
        }

        cmpRuleEngine.loadRule(isReturnMsg);
        cmpRuleEngine.executeRule(bankrecList,isReturnMsg);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
