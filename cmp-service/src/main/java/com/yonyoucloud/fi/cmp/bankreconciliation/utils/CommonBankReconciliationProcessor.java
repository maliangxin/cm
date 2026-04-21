package com.yonyoucloud.fi.cmp.bankreconciliation.utils;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.param.ResultSetProcessor;
import com.yonyou.iuap.yms.param.SQLParameter;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.ReFundType;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.BankDealDetailConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CommonBankReconciliationProcessor {
    public static void batchReconciliationBeforeUpdate(List<BankReconciliation> bankReconciliationList) {
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return;
            }
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if (executeBankReconciliationStatus(bankReconciliation) && bankReconciliation.getProcessstatus() !=null && bankReconciliation.getProcessstatus() == (short) 25) {
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
                }
            }
        } catch (Exception e) {
            log.error("批量处理银行对账数据异常", e);
        }
    }

    public static void batchReconciliationBeforeUpdate(List<BankReconciliation> bankReconciliationList, String confirmOrReject) {
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return;
            }
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                if (executeBankReconciliationStatus(bankReconciliation) && bankReconciliation.getProcessstatus() !=null && bankReconciliation.getProcessstatus() == (short) 25) {
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
                } else if (StringUtils.isNotEmpty(confirmOrReject) && "1".equals(confirmOrReject)) {
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus());
                } else if (StringUtils.isNotEmpty(confirmOrReject) && "2".equals(confirmOrReject)) {
                    bankReconciliation.setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
                }
            }
        } catch (Exception e) {
            log.error("批量处理银行对账数据异常", e);
        }
    }

    /**
     *
     * @param bankReconciliationList
     * @param confirmOrReject  人工确认：拒绝:2 流程状态设置为1  确认:1 流程状态设置为25流程完成
     */
    public static void batchBizObjectBeforeUpdate(List<BizObject> bankReconciliationList, String confirmOrReject)  {
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return;
            }
            for (BizObject bankReconciliation : bankReconciliationList) {
                if (executeBizObjectStatus(bankReconciliation) && bankReconciliation.getShort("processstatus") !=null && bankReconciliation.getShort("processstatus") == (short) 25) {
                    bankReconciliation.set("processstatus", (short) 1);
                } else if (StringUtils.isNotEmpty(confirmOrReject) && "1".equals(confirmOrReject)) {
                    bankReconciliation.set("processstatus", (short) 25);
                } else if (StringUtils.isNotEmpty(confirmOrReject) && "2".equals(confirmOrReject)) {
                    bankReconciliation.set("processstatus", (short) 1);
                }
            }
        } catch (Exception e) {
            log.error("批量处理业务对象数据异常", e);
        }
    }

    public static void batchBizObjectBeforeUpdate(List<BizObject> bankReconciliationList)  {
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return;
            }
            for (BizObject bankReconciliation : bankReconciliationList) {
                if (executeBizObjectStatus(bankReconciliation) && bankReconciliation.getShort("processstatus") !=null && bankReconciliation.getShort("processstatus") == (short) 25) {
                    bankReconciliation.set("processstatus", (short) 1);
                }
            }
        } catch (Exception e) {
            log.error("批量处理业务对象数据异常", e);
        }
    }


    public static void batchReconciliationBeforeDelete(List<BankReconciliation> bankReconciliationList, IYmsJdbcApi ymsJdbcApi) {
        try {
            if (CollectionUtils.isEmpty(bankReconciliationList)) {
                return;
            }
            String paramsQuery = "";
            for (int i = 0; i < bankReconciliationList.size() - 1; i++) {
                paramsQuery += "'" + bankReconciliationList.get(i).getId() + "',";
            }
            if (!CollectionUtils.isEmpty(bankReconciliationList)) {
                paramsQuery += "'" + bankReconciliationList.get(bankReconciliationList.size() - 1).getId() + "'";
            }
            //组装sql
            String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID = "select id from cmp_bankdealdetail_ods where ytenant_id=? and mainid in (" + paramsQuery + ")";
            SQLParameter sqlParameter_ods = new SQLParameter();
            sqlParameter_ods.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
            List<BankDealDetailODSModel> list_ods = ymsJdbcApi.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, sqlParameter_ods, new ResultSetProcessor() {
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
            if (!CollectionUtils.isEmpty(list_ods)) {
                ymsJdbcApi.remove(list_ods);
            }
        } catch (Exception e) {
            log.error("批量删除银行对账单数据异常", e);
        }
    }

    public static void HandlerReconciliationBeforeDeleteById(Long id, IYmsJdbcApi ymsJdbcApi) {
        try {
            if (id == null) {
                return;
            }
            //组装sql
            String UPDATE_ODS_BYPROCESSSTATUSANDTRACEID = "select id from cmp_bankdealdetail_ods where ytenant_id=? and mainid =" + id;
            SQLParameter sqlParameter_ods = new SQLParameter();
            sqlParameter_ods.addParamWithType(InvocationInfoProxy.getTenantid(), Types.VARCHAR);
            List<BankDealDetailODSModel> list_ods = ymsJdbcApi.queryForList(UPDATE_ODS_BYPROCESSSTATUSANDTRACEID, sqlParameter_ods, new ResultSetProcessor() {
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
            if (!CollectionUtils.isEmpty(list_ods)) {
                ymsJdbcApi.remove(list_ods);
            }
        } catch (Exception e) {
            log.error("根据银行流水ID银行对账单数据异常", e);
        }
    }

    private static boolean executeBankReconciliationStatus(BankReconciliation bankReconciliation) {

        //关联关系，已关联则返回false，未关联则返回true
        if (bankReconciliation.getAssociationstatus() != null && bankReconciliation.getAssociationstatus() == AssociationStatus.Associated.getValue()) {
            return Boolean.FALSE;
        }
        //发布状态，已发布则返回false，未发布则返回true
        if (bankReconciliation.getIspublish() != null && bankReconciliation.getIspublish()) {
            return Boolean.FALSE;
        }
        //总账是否勾兑，已勾兑则返回false，未勾兑则返回true
        if (bankReconciliation.getOther_checkflag() != null && bankReconciliation.getOther_checkflag()) {
            return Boolean.FALSE;
        }
        //日记账是否勾兑，已勾兑则返回false，未勾兑则返回true
        if (bankReconciliation.getCheckflag() != null && bankReconciliation.getCheckflag()) {
            return Boolean.FALSE;
        }
        //疑重判定逻辑，疑似重复，疑重确认重复
        if (bankReconciliation.getIsRepeat() != null && (bankReconciliation.getIsRepeat() == (short) BankDealDetailConst.REPEAT_DOUBT || bankReconciliation.getIsRepeat() == (short) BankDealDetailConst.REPEAT_CONFIRM)) {
            return Boolean.FALSE;
        }
        //退票，疑似退票返回false，非退票返回true
        if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus() == ReFundType.SUSPECTEDREFUND.getValue()) {
            return Boolean.FALSE;
        }
        //流水处理完结状态，已完结则返回false，未完结则返回true
        if (bankReconciliation.getSerialdealendstate() != null && bankReconciliation.getSerialdealendstate() == SerialdealendState.END.getValue()) {
            return Boolean.FALSE;
        }
        //生单状态,已自动关联返回false，未自动关联返回true
        if (bankReconciliation.getIsautocreatebill() != null && bankReconciliation.getIsautocreatebill() == Boolean.TRUE) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private static boolean executeBizObjectStatus(BizObject bankReconciliation) {

        //发布状态，已发布则返回false，未发布则返回true
        if (bankReconciliation.getBoolean("ispublish") != null && bankReconciliation.getBoolean("ispublish")) {
            return Boolean.FALSE;
        }
        //总账是否勾兑，已勾兑则返回false，未勾兑则返回true
        if (bankReconciliation.getBoolean("other_checkflag") != null && bankReconciliation.getBoolean("other_checkflag")) {
            return Boolean.FALSE;
        }
        //日记账是否勾兑，已勾兑则返回false，未勾兑则返回true
        if (bankReconciliation.getBoolean("checkflag") != null && bankReconciliation.getBoolean("checkflag")) {
            return Boolean.FALSE;
        }
        //疑重判定逻辑，疑似重复，疑重确认重复
        if (bankReconciliation.getShort("isrepeat") != null && (bankReconciliation.getShort("isrepeat") == (short) BankDealDetailConst.REPEAT_DOUBT || bankReconciliation.getShort("isrepeat") == (short) BankDealDetailConst.REPEAT_CONFIRM)) {
            return Boolean.FALSE;
        }
        //退票，疑似退票返回false，非退票返回true
        if (bankReconciliation.getShort("refundstatus") != null && bankReconciliation.getShort("refundstatus") == ReFundType.SUSPECTEDREFUND.getValue()) {
            return Boolean.FALSE;
        }
        //流水处理完结状态，已完结则返回false，未完结则返回true
        if (bankReconciliation.getShort("serialdealendstate") != null && bankReconciliation.getShort("serialdealendstate") == SerialdealendState.END.getValue()) {
            return Boolean.FALSE;
        }
        //生单状态,已自动关联返回false，未自动关联返回true
        if (bankReconciliation.getBoolean("isautocreatebill") != null && bankReconciliation.getBoolean("isautocreatebill") == Boolean.TRUE) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
