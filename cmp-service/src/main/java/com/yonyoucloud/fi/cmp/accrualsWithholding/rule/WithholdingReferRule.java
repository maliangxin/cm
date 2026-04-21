package com.yonyoucloud.fi.cmp.accrualsWithholding.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdBillType;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY;

/**
 * 预提，参照前规则 - 10
 */
@Slf4j
@Component("withholdingReferRule")
@RequiredArgsConstructor
public class WithholdingReferRule extends AbstractCommonRule {

    private final BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, map);
        if (bills.size() > 0) {
            bizObject = bills.get(0);
        }
        FilterVO filterVO  = billDataDto.getCondition();
        if (filterVO == null) {
            filterVO = new FilterVO();
        }

        if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
            try {
                BdBillType billType = baseRefRpcService.queryBillTypeByFormId(CM_CMP_CMP_ACCRUALSWITHHOLDINGQUERY);
                if (null == billType) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100419"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418065D","查询银行预提单单据类型失败！请检查数据。") /* "查询银行预提单单据类型失败！请检查数据。" */ );
                }
                String billtypeId = billType.getId();
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, billtypeId));
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", ICmpConstant.QUERY_EQ, 0));
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
                if (billDataDto.getCondition()==null){
                    billDataDto.setTreeCondition(filterVO);
                }
            } catch (Exception e) {
                log.error("WithholdingReferRule query transType fail! id={}, yTenantId = {}, e = {}",
                        bizObject.getId(), InvocationInfoProxy.getTenantid(), e.getMessage());
            }
        } else if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
            if (bizObject.get("currency") != null) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bizObject.get("currency")));
            }
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
            if (billDataDto.getCondition()==null){
                billDataDto.setTreeCondition(filterVO);
            }
        } else if ("ucfbasedoc.bd_currencytenantref".equals(billDataDto.getrefCode()) ) {
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.ENABLE, ICmpConstant.QUERY_EQ, "1"));
            if (billDataDto.getCondition()==null){
                billDataDto.setTreeCondition(filterVO);
            }
        }
        return new RuleExecuteResult();
    }
}
