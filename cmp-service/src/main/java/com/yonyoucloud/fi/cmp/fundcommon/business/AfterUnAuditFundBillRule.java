package com.yonyoucloud.fi.cmp.fundcommon.business;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单弃审的后置规则：通知第三方单据状态</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-04-06 14:43
 */
@Slf4j
@Component("afterUnAuditFundBillRule")
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AfterUnAuditFundBillRule extends AbstractCommonRule {
    final private IFundCommonService fundCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        String billnum = billContext.getBillnum();
        for (BizObject bizobject : bills) {
            BizObject currentBill;
            if (IBillNumConstant.FUND_PAYMENT.equals(billnum) || IBillNumConstant.FUND_PAYMENTLIST.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101247"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029F","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                boolean isWfControlled = ValueUtils.isNotEmptyObj(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED);
                short verifystate = Short.parseShort(currentBill.get("verifystate").toString());
                if (isWfControlled && verifystate != VerifyState.INIT_NEW_OPEN.getValue()) {
                    // 开启审批流，弃审后，单据不是初始开立状态，赋值auditTime为new Date()，适配附件删除功能
                    currentBill.set("auditTime", new Date());
                    currentBill.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FundPayment.ENTITY_NAME, currentBill);
                }

                //20231030 弃审删除关联协同生成的资金收，资金收款单审批流状态为初始开立/驳回到制单时，否则不允许撤回，给出提示：“协同生成的资金收款单已处理，不允许撤回”
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bizobject.getId()));
                querySchema.addCondition(group);
                List<FundPayment_b> list = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
                List<FundPayment_b> updateList = new ArrayList<>();
                List<FundCollection> deleteList = new ArrayList<>();
                for (FundPayment_b payment_b : list){
                  //关联协同资金收款单编码不为空，则有协同生单数据
                  if (!StringUtils.isEmpty(payment_b.getSynergybillno())){
                      FundCollection fundCollection = MetaDaoHelper.findById(FundCollection.ENTITY_NAME,payment_b.getSynergybillid());
                      if (fundCollection != null){
                          //单审批流状态为初始开立/驳回到制单
                          if (VerifyState.INIT_NEW_OPEN.getValue() == fundCollection.getVerifystate() || VerifyState.REJECTED_TO_MAKEBILL.getValue() == fundCollection.getVerifystate() ){
                              //清空付款单明细协同生单关联信息
                              payment_b.setIssynergy(false);
                              payment_b.setSynergybillitemno(null);
                              payment_b.setSynergybillid(null);
                              payment_b.setSynergybillno(null);
                              EntityTool.setUpdateStatus(payment_b);
                              updateList.add(payment_b);

                              //删除协同生成的资金收款单
                              deleteList.add(fundCollection);
                          }else {
                              throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101248"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1982586404D0000A","协同生成的资金收款单[%s]已处理，不允许撤回") /* "协同生成的资金收款单[%s]已处理，不允许撤回" */,fundCollection.getCode()));
                          }
                      }
                  }
                }
                if (CollectionUtils.isNotEmpty(updateList)){
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME,updateList);
                }
                if (CollectionUtils.isNotEmpty(deleteList)){
                    MetaDaoHelper.delete(FundCollection.ENTITY_NAME,deleteList);
                }

            }

            if (IBillNumConstant.FUND_COLLECTION.equals(billnum) || IBillNumConstant.FUND_COLLECTIONLIST.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101247"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029F","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
                boolean isWfControlled = ValueUtils.isNotEmptyObj(currentBill.get(ICmpConstant.IS_WFCONTROLLED)) && currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED);
                short verifystate = Short.parseShort(currentBill.get("verifystate").toString());
                if (isWfControlled && verifystate != VerifyState.INIT_NEW_OPEN.getValue()) {
                    // 开启审批流，弃审后，单据不是初始开立状态，赋值auditTime为new Date()，适配附件删除功能
                    currentBill.set("auditTime", new Date());
                    currentBill.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(FundCollection.ENTITY_NAME, currentBill);
                }
            }

            if (IBillNumConstant.TRANSFERACCOUNTLIST.equals(billnum) || IBillNumConstant.TRANSFERACCOUNT.equals(billnum)) {
                currentBill = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizobject.getId());
                if (currentBill == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101247"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029F","单据不存在 id:") /* "单据不存在 id:" */ + bizobject.getId());
                }
            }
        }
        return new RuleExecuteResult();
    }
}
