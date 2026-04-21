package com.yonyoucloud.fi.cmp.rule;

import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: liaojbo
 * @Date: 2024年08月31日 20:53
 * @Description:
 */
@Component("fillSubAccRawValueRule")
public class FillSubAccRawValueRule extends AbstractCommonRule {
    private static final Logger log = LoggerFactory.getLogger(FillSubAccRawValueRule.class);
    @Autowired
    FundsOrgQueryServiceComponent fundsOrgQueryService;

    private static HashMap<String, String> hashMap;

    static {
        hashMap = new HashMap<>();

        hashMap.put("cmp.bankreconciliationsetting.BankReconciliationSetting","bankReconciliationSetting_b");
        hashMap.put("cmp.billclaim.BillClaim","items");
        hashMap.put("cmp.fundcollection.FundCollection","FundCollection_b");
        hashMap.put("cmp.fundpayment.FundPayment","FundPayment_b");
        hashMap.put("cmp.initdata.InitData","InitDatab");
    }

    public FillSubAccRawValueRule() {
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> param) throws Exception {
        String accentityKey = "accentity";
        String accentityRawKey = "accentityRaw";
        String config = this.getConfig();
        String fullName = billContext.getFullname();;
        if (StringUtils.isNotEmpty(config)) {
            CtmJSONObject ctmJSONObject = CtmJSONObject.parseObject(config);
            fullName = ctmJSONObject.getString("accentity");
            if (StringUtils.isNotEmpty(fullName)) {
                accentityKey = fullName;
            }

            String accentityRawConfig = ctmJSONObject.getString("accentityRaw");
            if (StringUtils.isNotEmpty(accentityRawConfig)) {
                accentityRawKey = accentityRawConfig;
            }
        }

        List<BizObject> mainBills = this.getBills(billContext, param);
        Map<String,String> fundorgsMap = new HashMap<>();
        if (!StringUtils.isEmpty(fullName) && !CollectionUtils.isEmpty(mainBills) && hashMap.keySet().contains(fullName)) {
            for (BizObject bill : mainBills) {
                String relationKey = hashMap.get(fullName);
                if (CollectionUtils.isNotEmpty(bill.get(relationKey))) {
                    //对账设置子表的核算会计主体的校验和赋值，是要根据明细行上的账户使用组织确定的，下边逻辑错误，注释掉！
//                    FundsOrgDTO fundorgs = this.fundsOrgQueryService.getById(bill.getString("accentity"));
                    List<BizObject> bills = bill.get(relationKey);
                    if (CollectionUtils.isNotEmpty(bills)) {
                        for (BizObject subBill : bills) {
                            String accentity = null;
                            //原来的会计组织，现在代表资金组织
                            if (BankReconciliationSetting.ENTITY_NAME.equals(fullName)) {
                                //特殊命名
                                accentity = subBill.get("useorg");
                            }else {
                                accentity = subBill.get("accentity");
                            }
                            //新增的会计组织字段
                            String accentityRaw = subBill.get("accentityRaw");
                            if (accentity == null) {
                                return new RuleExecuteResult();
                            }
                            //todo 增加栈内缓存
                            String fundorgsFinorgid = null;
                            if(fundorgsMap.get(accentity) != null){
                                fundorgsFinorgid = fundorgsMap.get(accentity);
                            }else {
                                FundsOrgDTO fundorgs = this.fundsOrgQueryService.getById(accentity);
                                fundorgsFinorgid = fundorgs.getFinorgid();
                                fundorgsMap.put(accentity,fundorgsFinorgid);
                            }
                            if (StringUtils.isEmpty(fundorgsFinorgid)) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100383"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050026", "资金组织对应的会计组织不存在") /* "资金组织对应的会计组织不存在" */);
                            }
                            log.info("根据资金组织查询到的会计组织:{}", fundorgsFinorgid);
                            if (accentityRaw != null) {
                                log.info("校验资金组织和会计组织的关系");
                                //根据资金组织查询对应的会计组织
                                if (!accentityRaw.equals(fundorgsFinorgid)) {
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100384"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050025", "资金组织与会计组织不匹配") /* "资金组织与会计组织不匹配" */);
                                }
                            } else if (accentityRaw == null) {
                                log.info("根据资金组织查询会计组织并赋值");
                                //根据资金组织查询会计组织并赋值
                                subBill.set("accentityRaw", fundorgsFinorgid);
                                log.info("1");
                            }
                        }
                    }
                }
            }
            return new RuleExecuteResult();
        } else {
            log.error("Rule parameters do not meet the subsequent logic, return directly! fullName: {}, bills: {}", fullName, mainBills);
            return new RuleExecuteResult();
        }
    }
}
