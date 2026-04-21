package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.ProtocolCallLogs;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 资金收付款单删除后规则
 *
 * @author mal
 * @version 1.0
 * @since 2022-02-15 16:38
 */
@Slf4j
@Component("afterDeleteFundBillRule")
@RequiredArgsConstructor
public class AfterDeleteFundBillRule extends AbstractCommonRule {

    private final IFundCommonService fundCommonService;

    private final CmCommonService commonService;

    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            if (StringUtils.isEmpty(billnum)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100380"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804C0","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
            }
        }
        assert bills != null;
        for (BizObject bizObject : bills) {
            Map<String, Object> autoConfigMap = commonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;

            // 资金付款单
            boolean isFundPaymentBillDelete = IBillNumConstant.FUND_PAYMENT.equals(billnum)
                    || IBillNumConstant.FUND_PAYMENTLIST.equals(billnum);
            if (isFundPaymentBillDelete) {
                if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
                    //走影像
                    BillBiz.executeRule("shareDelete", billContext, paramMap);
                    CtmJSONObject param = new CtmJSONObject();
                    param.put("data", bizObject);
                    param.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540044C", "删除资金付款单，走影像") /* "删除资金付款单，走影像" */);
                    ctmcmpBusinessLogService.saveBusinessLog(param, bizObject.get("code"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540044D", "删除资金付款单影像") /* "删除资金付款单影像" */, IServicecodeConstant.FUNDPAYMENT, IMsgConstant.FUND_PAYMENT, IMsgConstant.DELETE);
                    log.error("删除资金付款单，走影像");
                }
            }
            // 资金收款单
            if (IBillNumConstant.FUND_COLLECTION.equals(billnum) || IBillNumConstant.FUND_COLLECTIONLIST.equals(billnum)) {
                if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
                    //走影像
                    BillBiz.executeRule("shareDelete", billContext, paramMap);
                    CtmJSONObject param = new CtmJSONObject();
                    param.put("data", bizObject);
                    param.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540044A", "删除资金收款单，走影像") /* "删除资金收款单，走影像" */);
                    ctmcmpBusinessLogService.saveBusinessLog(param, bizObject.get("code"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540044B", "删除资金收款单影像") /* "删除资金收款单影像" */, IServicecodeConstant.FUNDCOLLECTION, IMsgConstant.FUND_COLLECTION, IMsgConstant.DELETE);
                    log.error("删除资金收款单，走影像");
                }
            }

            // 删除资金付款单，来源于内转协议生单的单据需删除内转协议调用日志
            boolean isInternalTransferProtocol = ValueUtils.isNotEmptyObj(bizObject.getShort(ICmpConstant.BILLTYPE))
                    && bizObject.getShort(ICmpConstant.BILLTYPE) == EventType.InternalTransferProtocol.getValue();
            if (isFundPaymentBillDelete && isInternalTransferProtocol){
                QuerySchema schema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(
                        QueryCondition.name(ICmpConstant.GENERATED_DOCUMENT_ID).eq(bizObject.getId())
                );
                schema.appendQueryCondition(queryConditionGroup);
                List<Map<String, Object>> list = MetaDaoHelper.query(ProtocolCallLogs.ENTITY_NAME, schema);
                if (!list.isEmpty()) {
                    List<ProtocolCallLogs> protocolCallLogsList = new ArrayList<>();
                    for (Map<String, Object> map : list) {
                        ProtocolCallLogs protocolCallLogs = new ProtocolCallLogs();
                        protocolCallLogs.init(map);
                        protocolCallLogsList.add(protocolCallLogs);
                    }
                    MetaDaoHelper.delete(ProtocolCallLogs.ENTITY_NAME, protocolCallLogsList);
                }
            }

            // 如果删除的是退票重付的资金付款单据，要解除源单据的关联关系
            if (isFundPaymentBillDelete) {
                QuerySchema schema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(
                        QueryCondition.name("refundAssociatedPaymentId").eq(bizObject.getId())
                );
                schema.appendQueryCondition(queryConditionGroup);
                List<Map<String, Object>> bizObjects = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME, schema);
                if (!bizObjects.isEmpty()) {
                    List<BizObject> fundPaymentSubUpdateList = new ArrayList<>();
                    for (Map<String, Object> map : bizObjects) {
                        BizObject bizObj = new BizObject(map);
                        bizObj.set("whetherRefundAndRepayment", 0);
                        bizObj.set("refundAssociatedPaymentId", null);
                        fundPaymentSubUpdateList.add(bizObj);
                    }
                    EntityTool.setUpdateStatus(fundPaymentSubUpdateList);
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentSubUpdateList);
                }
            }
        }
        return new RuleExecuteResult();
    }
}
