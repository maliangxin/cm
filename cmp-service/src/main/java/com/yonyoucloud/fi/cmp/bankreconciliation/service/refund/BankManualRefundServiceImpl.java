package com.yonyoucloud.fi.cmp.bankreconciliation.service.refund;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.reqvo.DataSettledBatchDetailReqVO;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankManualRefundService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.RefundStatus;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @description: 银行对账单手工退票具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2023/10/26 20:41
 */

@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class BankManualRefundServiceImpl implements BankManualRefundService {

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    BankReconciliationReFundService bankReconciliationReFundService;

    /**
     * 手工退票接口
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public JsonNode manualRefund(Map<String, Object> paramMap) throws Exception {
        ObjectNode result = JSONBuilderUtils.createJson();
        //action=stwbManualRefund 结算手工退票
        if("stwbManualRefund".equals(paramMap.get("action") != null ? paramMap.get("action").toString() : null)){
            //银行对账单数据
            Map<String, Object> bankdata = paramMap.get("bankdata") != null ? (Map<String, Object>) paramMap.get("bankdata") :null;
            //资金结算代发明细数据
            List<Map<String, Object>> manualRefundData = paramMap.get("manualRefundData") != null ? (List<Map<String, Object>>) paramMap.get("manualRefundData") :null;
            if (bankdata == null || manualRefundData==null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102030"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008D", "手工退票数据缺失！") /* "手工退票数据缺失！" */);
            }
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,bankdata.get("id"));
            if (bankReconciliation == null){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102031"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008F", "银行对账单数据不存在！") /* "银行对账单数据不存在！" */);
            }
//            String errMessage = BankreconciliationUtils.checkDataLegal(bankReconciliation, BankreconciliationActionEnum.MANUALREFUND);
//            if (Strings.isNotEmpty(errMessage)) {
//                throw new CtmException(errMessage);
//            }

            if (bankReconciliation.getRefundstatus() !=null && (bankReconciliation.getRefundstatus() == RefundStatus.Refunded.getValue()
                    || bankReconciliation.getRefundstatus() == RefundStatus.MaybeRefund.getValue())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102032"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080090", "银行对账单【%s】退票状态为疑似退票/退票，不可发起手工退票！") /* "银行对账单【%s】退票状态为疑似退票/退票，不可发起手工退票！" */,bankReconciliation.getBank_seq_no()));
            }

            try {
                List<DataSettledBatchDetailReqVO> dataSettledBatchDetailReqVOList = new ArrayList<>();
                for (Map<String, Object> m : manualRefundData){
                    DataSettledBatchDetailReqVO dataSettledBatchDetailReqVO = new DataSettledBatchDetailReqVO();
                    dataSettledBatchDetailReqVO.setAmount(new BigDecimal(m.get("amount").toString()));
                    dataSettledBatchDetailReqVO.setId(Long.parseLong(m.get("dataSettledBatchId").toString()));
                    // 退票确认，传递流水的交易日期
                    if (bankReconciliation.getTran_time() != null) {
                        dataSettledBatchDetailReqVO.setTransactionDate(bankReconciliation.getTran_time());
                    } else {
                        dataSettledBatchDetailReqVO.setTransactionDate(bankReconciliation.getTran_date());
                    }
                    dataSettledBatchDetailReqVOList.add(dataSettledBatchDetailReqVO);
                }
                CtmJSONObject logparam = new CtmJSONObject();
                logparam.put("bankinfo",bankReconciliation);
                logparam.put("settleinfo",dataSettledBatchDetailReqVOList);
                ctmcmpBusinessLogService.saveBusinessLog(logparam, "", "单条手工退票", IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.RECONCILIATE, "手工退票");//@notranslate
                RemoteDubbo.get(IOpenApiService.class, IDomainConstant.MDD_DOMAIN_STWB).updateDataSettleByRedund(dataSettledBatchDetailReqVOList);
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102033"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080091", "调用资金结算手工退票接口异常，异常信息：") /* "调用资金结算手工退票接口异常，异常信息：" */ + e.getMessage());
            }

            //资金结算退票成功后，修改银行对账单状态为退票
            bankReconciliation.setRefundstatus(RefundStatus.Refunded.getValue());
            EntityTool.setUpdateStatus(bankReconciliation);
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
        }

        //action=cmpManualRefund 现金手工退票
        if("cmpManualRefund".equals(paramMap.get("action") != null ? paramMap.get("action").toString() : null)){
            List<Map<String, Object>> bankDataList = paramMap.get("bankdatalist") != null ? (List<Map<String, Object>>) paramMap.get("bankdatalist") :null;
            List<Object> ids = new ArrayList<>();
            for(Map<String, Object> bankdata : bankDataList){
                ids.add(bankdata.get("id"));
            }
            List<BankReconciliation> bankReconciliations;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
            querySchema.addCondition(group);
            bankReconciliations =  MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
            //修改银行对账单
            for (int i = 0; i < bankReconciliations.size();i++) {
                bankReconciliations.get(i).setEntityStatus(EntityStatus.Update);
                bankReconciliations.get(i).setRefundstatus(RefundStatus.Refunded.getValue());
                //退票确认人赋值
                bankReconciliations.get(i).setRefundconfirmstaff(AppContext.getCurrentUser().getName());
            }
            //给退票数据赋值
            if(bankReconciliations.size() == 2){
                bankReconciliations.get(0).setRefundrelationid(bankReconciliations.get(1).getId().toString());
                bankReconciliations.get(1).setRefundrelationid(bankReconciliations.get(0).getId().toString());
            }
            //发布到事件中心
            try {
                CtmJSONObject logparam = new CtmJSONObject();
                logparam.put("bankinfo",bankReconciliations);
                ctmcmpBusinessLogService.saveBusinessLog(logparam, "", "两条手工退票", IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.RECONCILIATE, "手工退票");//@notranslate
                bankReconciliationReFundService.refundSettlebench(bankReconciliations);
            }catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102034"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508008E", "广播退票单据信息异常，异常信息：") /* "广播退票单据信息异常，异常信息：" */ + e.getMessage());
            }
            CommonSaveUtils.updateBankReconciliation(bankReconciliations);
        }

        return result;
    }

    @Override
    public void clearRefundStatusById(CtmJSONObject params) throws Exception {
        LinkedHashMap<String,String> data = (LinkedHashMap<String, String>) params.get("data");
        String id = data.get("id");
        String bank_seq_no = data.get("bankSerialNumber");
        if (StringUtils.isEmpty(id) && StringUtils.isEmpty(bank_seq_no)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221410EE05000010", "请输入银行流水id或银行交易流水号") /* "请输入银行流水id或银行交易流水号" */);
        }
        List<BankReconciliation> bankReconciliations;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup(ConditionOperator.and);
        if (StringUtils.isNotEmpty(id)){
            group.addCondition(QueryCondition.name("id").eq(id));
        }else {
            group.addCondition(QueryCondition.name("bank_seq_no").eq(bank_seq_no));
        }
        group.appendCondition(QueryCondition.name("refundstatus").eq(RefundStatus.Refunded.getValue()));
        querySchema.addCondition(group);
        bankReconciliations =  MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME,querySchema,null);
        if (bankReconciliations == null || bankReconciliations.size() == 0){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221410EE0500000E", "根据条件未查询到已退票的银行流水") /* "根据条件未查询到已退票的银行流水" */);
        }
        if (bankReconciliations.size() > 1){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221410EE0500000F", "根据银行交易流水号查询出多条流水，请按照银行流水id精确查询！") /* "根据银行交易流水号查询出多条流水，请按照银行流水id精确查询！" */);
        }
        if (bankReconciliations.get(0).getRefundrelationid() != null){
            BankReconciliation otherBankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME,bankReconciliations.get(0).getRefundrelationid());
            if (otherBankReconciliation != null){
                bankReconciliations.add(otherBankReconciliation);
            }
        }
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("bankinfo",CtmJSONObject.toJSONString(bankReconciliations));
        ctmcmpBusinessLogService.saveBusinessLog(logparam, StringUtils.isNotEmpty(id) ? id : bank_seq_no, "运营工具-清除银行流水退票状态", IServicecodeConstant.CMPBANKRECONCILIATION, IMsgConstant.RECONCILIATE, "清除退票状态");//@notranslate
        //清除退票状态
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            bankReconciliation.setEntityStatus(EntityStatus.Update);
            bankReconciliation.setRefundstatus(null);
            bankReconciliation.setRefundrelationid(null);
            //智能到账，退票自动辨识自动设置为false
            bankReconciliation.setRefundauto(null);
        }
        CommonSaveUtils.updateBankReconciliation(bankReconciliations);
    }
}