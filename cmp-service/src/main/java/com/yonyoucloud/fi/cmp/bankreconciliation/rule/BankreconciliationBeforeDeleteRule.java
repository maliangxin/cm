package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.cmpentity.TripleSynchronStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by xudya on 2019/06/14 1115.
 */
@Slf4j
public class BankreconciliationBeforeDeleteRule extends AbstractCommonRule {

    @Autowired
    ICmpSendEventService cmpSendEventService;

    @Autowired
    IYmsJdbcApi busiBaseDAO;
    public void setYmsJdbcApi(IYmsJdbcApi busiBaseDAO) {
        this.busiBaseDAO = busiBaseDAO;
    }
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            BankReconciliation bankReconciliationReq = (BankReconciliation) bills.get(0);

            QueryConditionGroup repeatGroup = new QueryConditionGroup(ConditionOperator.or);
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_null());
            repeatGroup.addCondition(QueryCondition.name("isrepeat").is_not_null());

            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM).appendQueryCondition(
                    QueryCondition.name("id").eq(bankReconciliationReq.getId())
            );
            querySchema.appendQueryCondition(repeatGroup);

            List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema,null);
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED74204780025", "根据id【%s】未找到对应银行流水") /* "根据id【%s】未找到对应银行流水" */, bankReconciliationReq.getId()));
           }
            BankReconciliation bankReconciliation = bankReconciliationList.get(0);
            if ((ObjectUtils.isNotEmpty(bankReconciliation.getCheckflag()) && bankReconciliation.getCheckflag()) || (ObjectUtils.isNotEmpty(bankReconciliation.getOther_checkflag()) && bankReconciliation.getOther_checkflag())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102597"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D7","该单据已勾对完成，不能删除单据！") /* "该单据已勾对完成，不能删除单据！" */);
            }
            if (DateOrigin.DownFromYQL.getValue() == bankReconciliation.getDataOrigin().getValue()) {
                List<BankDealDetail> bankDealDetail = MetaDaoHelper.queryById(BankDealDetail.ENTITY_NAME, "id", bankReconciliation.getLong("id"));
                if (CollectionUtils.isEmpty(bankDealDetail)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_203ED74204780024", "银行交易流水号【%s】未找到对应账户交易明细，删除失败，请联系系统管理员处理！") /* "银行交易流水号【%s】未找到对应账户交易明细，删除失败，请联系系统管理员处理！" */, bankReconciliation.getBank_seq_no()));
                }
                //fix CZFW-383041 确认重复的银企联数据可以删除
                if(bankReconciliation.getIsRepeat() == BankDealDetailConst.REPEAT_CONFIRM){
                    MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME, bankReconciliation.getLong("id"));
                }else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102598"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D9", "银企联生成的单据，不能删除！") /* "银企联生成的单据，不能删除！" */);
                }
            }
            JedisLockUtils.isexistDzLock(bankReconciliation.getAccentity()+bankReconciliation.getBankaccount());

            if("cmp_bankreconciliationwdlist".equals(billContext.getBillnum())){
                JedisLockUtils.isexistRjLock(bankReconciliation.get(IBussinessConstant.ACCENTITY));
                //表示是银行期初银行未达列表中的数据
                //已日结后不能修改或删除期初数据
            }
            if (ObjectUtils.isNotEmpty(bankReconciliation.getAutobill()) && bankReconciliation.getAutobill()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102599"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D6","已生单单据不允许删除") /* "已生单单据不允许删除" */);
            }
            if (null != bankReconciliation.getAssociationstatus() && bankReconciliation.getAssociationstatus() != 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102600"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D5","银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D8","已关联不能删除！") /* "已关联不能删除！" */);
            }
            if (bankReconciliation.getIspublish()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102600"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D5","银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DA","已发布，不能删除！") /* "已发布，不能删除！" */);
            }
            //已同步至三方对账 不能删除
            if (bankReconciliation.getTripleSynchronStatus() == TripleSynchronStatus.AlreadyManual.getValue() || bankReconciliation.getTripleSynchronStatus() == TripleSynchronStatus.AlreadyAuto.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102601"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802DB","银行交易流水号[%s]已同步至三方对账，无法删除！") /* "银行交易流水号[%s]已同步至三方对账，无法删除！" */, (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no())));
            }
            //退票状态银行对账单不可删除
            if (bankReconciliation.getRefundstatus() != null &&
                    (bankReconciliation.getRefundstatus() == RefundStatus.Refunded.getValue() || bankReconciliation.getRefundstatus() == RefundStatus.MaybeRefund.getValue())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102600"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D5","银行交易流水号：") /* "银行交易流水号：" */ + (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no()) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D4","退票的对账单不可删除") /* "退票的对账单不可删除" */);
            }
            if (null != bankReconciliation.getReceiptassociation() && bankReconciliation.getReceiptassociation() != 4) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102602"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7F1C0490056A","银行交易流水号：[%s]已关联银行交易回单不能删除！") /* "银行交易流水号：[%s]已关联银行交易回单不能删除！" */, (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no())));
            }
            //是否触发归集=‘是’的单据不能删除
            if (ObjectUtils.isNotEmpty(bankReconciliation.getIsimputation()) && bankReconciliation.getIsimputation()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102603"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7F1C0490056B","银行交易流水号：[%s]已触发归集处理，不能删除！") /* "银行交易流水号：[%s]已触发归集处理，不能删除！" */,(bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no())));
            }
            if (null != bankReconciliation.getEliminateStatus() ) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102604"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A0D7F1C0490056C","银行交易流水号：[%s]已进行剔除处理,不能删除！") /* "银行交易流水号：[%s]已进行剔除处理,不能删除！" */, (bankReconciliation.getBank_seq_no()==null?"[]":bankReconciliation.getBank_seq_no())));
            }

            deleteOds(bankReconciliationReq);
            // 银行流水支持发送事件消息
            List<BankReconciliation> bankReconciliations = new ArrayList<>();
            bankReconciliations.add(bankReconciliation);
            cmpSendEventService.sendEventByBankClaimBatch(bankReconciliations, EntityStatus.Delete.name());
        }
        return new RuleExecuteResult();
    }

    private void deleteOds(BankReconciliation bankReconciliationReq) {
        try {
            String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID="select id from cmp_bankdealdetail_ods where ytenant_id=? and mainid = ?";
            SQLParameter sqlParameter_ods = new SQLParameter();
            sqlParameter_ods.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
            sqlParameter_ods.addParamWithType(bankReconciliationReq.getId(),Types.VARCHAR);
            List<BankDealDetailODSModel> list_ods = busiBaseDAO.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, sqlParameter_ods, new ResultSetProcessor() {
                @Override
                public Object handleResultSet(ResultSet rs) throws SQLException {
                    List<BankDealDetailODSModel> list = new ArrayList<>();
                    while (rs.next()) {
                        String id = rs.getString(1);
                        BankDealDetailODSModel bankDealDetailODSModel = new BankDealDetailODSModel();
                        bankDealDetailODSModel.setId(id);
                        list.add(bankDealDetailODSModel);
                    }
                    return list;
                }
                @Override
                public Object handleStreamingResultSet(ResultSet rs, PreparedStatement preparedStatement, Connection con) throws SQLException {
                    return null;
                }
            });
            busiBaseDAO.remove(list_ods);
        } catch (Exception e) {
            log.error("删除ods数据异常",e);
        }

    }

}
