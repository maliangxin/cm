package com.yonyoucloud.fi.cmp.common.rule;


import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.util.basedoc.BillTypeQueryService;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.common.CtmException;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_COLLECTION;
import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_FUND_PAYMENT;

/**
 * @Author: mal
 * @Description: 本类用于过滤交易类型的启停用和删除标识
 * @Date: Created in  2022/11/08 10:05
 * @Version v1.0
 */
@Component
public class TransTypeReferFilterRule extends AbstractCommonRule {

    /**
     *  需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();
    @Autowired
    private BillTypeQueryService billTypeQueryService;
    public TransTypeReferFilterRule() {
        BILLNUM_MAP.add(IBillNumConstant.RECEIVE_BILL);
        BILLNUM_MAP.add(IBillNumConstant.PAYMENT);
        BILLNUM_MAP.add(IBillNumConstant.PAYMENT);
        BILLNUM_MAP.add(IBillNumConstant.FUND_COLLECTION);
        BILLNUM_MAP.add(IBillNumConstant.FUND_PAYMENT);
    }

    /**
     * 对交易类型进行过滤，过滤删除标识为未删除，启用标识为已启用的交易类型
     *
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        RefEntity refentity = billDataDto.getRefEntity();
        if(BILLNUM_MAP.contains(billnum) && (IRefCodeConstant.TRANSTYPE_BD_BILLTYPERE_LOCAL.equals(refentity.code) || IRefCodeConstant.TRANSTYPE_BD_BILLTYPEREF.equals(refentity.code))){
            if (refentity.domain == null || IDomainConstant.MDD_DOMAIN_TRANSTYPE.equals(refentity.domain) || IDomainConstant.MDD_DOMAIN_UCFBASEDOC.equals(refentity.domain)) {
                if(billDataDto.getCondition() == null){
                    FilterVO conditon = new FilterVO();
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", "eq", "0"));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", "eq", "1"));
                    billDataDto.setCondition(conditon);
                }else{
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", "eq", "0"));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", "eq", "1"));
                }
                if(IBillNumConstant.PAYMENT.equals(billnum) ){
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", "eq", "FICM2"));
                }else if(IBillNumConstant.RECEIVE_BILL.equals(billnum)){
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", "eq", "FICM1"));
                }
                BdBillType billType = null;
                if(IBillNumConstant.FUND_COLLECTION.equals(billContext.getBillnum())){
                    billType = billTypeQueryService.queryBillTypeId(CM_CMP_FUND_COLLECTION);
                    if (!ValueUtils.isNotEmptyObj(billType)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101914"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180394","查询资金收付款单交易类型失败！请检查数据。") /* "查询资金收付款单交易类型失败！请检查数据。" */);
                    }
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "cmp_fundcollection_delegation"));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billType.getId()));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                    // 资金付款单
                }else if(IBillNumConstant.FUND_PAYMENT.equals(billContext.getBillnum())){
                    billType = billTypeQueryService.queryBillTypeId(CM_CMP_FUND_PAYMENT);
                    if (!ValueUtils.isNotEmptyObj(billType)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101914"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180394","查询资金收付款单交易类型失败！请检查数据。") /* "查询资金收付款单交易类型失败！请检查数据。" */);
                    }
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("code", ICmpConstant.QUERY_NEQ, "cmp_fund_payment_delegation"));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billType.getId()));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                }
            }
        }
        return new RuleExecuteResult();
    }
}
