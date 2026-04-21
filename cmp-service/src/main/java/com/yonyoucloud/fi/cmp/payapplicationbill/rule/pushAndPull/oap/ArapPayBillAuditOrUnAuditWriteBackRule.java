package com.yonyoucloud.fi.cmp.payapplicationbill.rule.pushAndPull.oap;

import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.payapplicationbill.PayApplicationBill;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * <h1>应付付款单弃审和审核时修改回写付款申请表单状态</h1>
 *
 * @author GuoCai Sun
 * @version 1.0
 * @since 2021/4/16 8:32
 */
@Slf4j
@Component("arapPayBillAuditOrUnAuditWriteBackRule")
public class ArapPayBillAuditOrUnAuditWriteBackRule extends AbstractCommonRule implements ISagaRule {
    @Autowired
    CmCommonService cmCommonService;

    @Autowired
    BaseRefRpcService baseRefRpcService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        log.error("pay bill audit or unAudit back write data input parameter, billContext = {}, paramMap = {}"
                , JsonUtils.toJson(billContext), paramMap);
        BillDataDto item = (BillDataDto) this.getParam(paramMap);
        List<BizObject> bills = BillInfoUtils.decodeBills(billContext, item.getData());
        List<PayApplicationBill> payApplicationBills = new ArrayList<>(16);
        for (Map bizobject : bills) {
            String action = billContext.getAction();
            if (!ValueUtils.isNotEmptyObj(action)) {
                log.info("pay bill audit or unAudit back write data error!");
            }
            Object srcFlag = bizobject.get("srcflag");
            if (!"cmppayapplication".equals(srcFlag)) {
                log.error("pay bill audit or unAudit back write data input parameter ! tenant_id = {}, id = {}, srcFlag = {}",
                        InvocationInfoProxy.getTenantid(), bizobject.get("id"), srcFlag);
                return new RuleExecuteResult();
            }
            Object settleMode = bizobject.get(SETTLE_MODE);
            if (ValueUtils.isNotEmptyObj(settleMode)) {
                SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                settleMethodQueryParam.setId((Long) settleMode);
                settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                if(dataList != null && dataList.size() > CONSTANT_ZERO) {
                    Object serviceAttr = dataList.get(CONSTANT_ZERO).getServiceAttr();
                    bizobject.put("serviceAttr", serviceAttr);
                    if (ValueUtils.isNotEmptyObj(serviceAttr) && !CONSTANT_ZERO.equals(serviceAttr) && !CONSTANT_ONE.equals(serviceAttr)) {
                        log.error("pay bill audit or unAudit back write data input parameter ! settleMode not exist, tenant_id = {}, id = {}, serviceAttr = {}",
                                InvocationInfoProxy.getTenantid(), bizobject.get("id"), serviceAttr);
                        return new RuleExecuteResult();
                    }
                }
            }

            billContext.setFullname("arap.paybill.PayBillb");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_FIARAP);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("srcbillitemid");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("mainid").eq(bizobject.get("id")));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(billContext, schema);
            Set<Object> srcbillitemid = new HashSet<>(16);
            if (CollectionUtils.isNotEmpty(list)) {
                for (Map<String, Object> stringObjectMap : list) {
                    srcbillitemid.add(stringObjectMap.get("srcbillitemid"));
                }
            }
            payApplicationBills.addAll(cmCommonService.updateStatePayApplyBill(action, srcbillitemid));
        }
        Map<String, Object> oldData = new HashMap<>(CONSTANT_EIGHT);
        oldData.put(PAY_APPLY_BILL, payApplicationBills);
        paramMap.put(OLD_INFO, oldData);
        return new RuleExecuteResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("pay bill audit or unAudit back write data cancel, billContext = {}", JsonUtils.toJson(billContext));
        }
        Map<String,Object> data = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(paramMap.get(OLD_INFO)));
        List<PayApplicationBill> payApplyBillList = (List<PayApplicationBill>) data.get(PAY_APPLY_BILL);
        EntityTool.setUpdateStatus(payApplyBillList);
        MetaDaoHelper.update(PayApplicationBill.ENTITY_NAME, payApplyBillList);
        return new RuleExecuteResult();
    }
}
