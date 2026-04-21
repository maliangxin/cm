package com.yonyoucloud.fi.cmp.common.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("reFillMainPKRule")
/**
 * @author: qihaoc
 * @Date: 2026年02月23日 10:32
 * @Description:用于进行风险检测后的临时id回填，临时id回填到主表和子表关联字段中，只在新增保存时进行
 */
public class ReFillMainPKRule extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (CollectionUtils.isNotEmpty(bills)) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102093"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800D9", "传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        assert bills != null;
        for (BizObject bizObject : bills) {
            if(StringUtils.isNotEmpty(bizObject.get("tmpRiskCheckBillId"))){
                //只在新增保存时进行
                if(!bizObject.getEntityStatus().equals(EntityStatus.Insert)){
                    continue;
                }
                //处理主表id
                if (IBillNumConstant.FUND_PAYMENT.equals(billnum) ){
                    bizObject.set("id","" + bizObject.get("tmpRiskCheckBillId"));
                }else if(IBillNumConstant.TRANSFERACCOUNT.equals(billnum) || IBillNumConstant.CMP_FOREIGNPAYMENT.equals(billnum)){
                    bizObject.set("id",Long.valueOf(bizObject.get("tmpRiskCheckBillId").toString()));
                }
                //处理子表关联id
                if (IBillNumConstant.FUND_PAYMENT.equals(billnum) ){
                    List<FundPayment_b> fundPaymentBList = bizObject.get("FundPayment_b");
                    for (FundPayment_b fundPayment_b : fundPaymentBList) {
                        fundPayment_b.setMainid(bizObject.get("tmpRiskCheckBillId").toString());
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }
}
