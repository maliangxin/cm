package com.yonyoucloud.fi.cmp.bankreconciliation.rule;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.IBankrecRuleEngineService;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailaccess.model.BankDealDetailODSModel;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetaildao.IBankDealDetailAccessDao;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailRuleResult;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailcontext.BankDealDetailWrapper;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.BankDealDetailManageFacade;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.process.BankDealDetailProcessChainImpl;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.model.DealDetailRuleExecRecord;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.utils.DealDetailUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
* @Author: wangjipeng
* @Description: openAPI新增数据的后置操作
* @DateTime: 2022/10/10 16:03
*/
@Slf4j
@Component("bankreconciliationAfterSaveRule")
@RequiredArgsConstructor
public class BankreconciliationAfterSaveRule extends AbstractCommonRule {

    private final IBankrecRuleEngineService iBankrecRuleEngineService;

    private final IBankDealDetailAccessDao bankDealDetailAccessDao;

    private final BankDealDetailManageFacade bankDealDetailManageAccess;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        if(CollectionUtils.isEmpty(bills)){
            return new RuleExecuteResult();
        }
        BankReconciliation bankReconciliation = (BankReconciliation) bills.get(0);
//        //智能流水开关
//        if(DealDetailUtils.isOpenIntelligentDealDetail()){
//            //如果存在事务
//            if(TransactionSynchronizationManager.isActualTransactionActive()){
//                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//                    @Override
//                    public void afterCommit() {
//                        dealDetailProcess(bankReconciliation);
//                    }
//                });
//            }
//        }
        return new RuleExecuteResult();
    }

    /**
     * 流水后置处理
     * */
    public void dealDetailProcess(BankReconciliation bankReconciliation){
        try{
            //step1:查询银行流水
            Long id = bankReconciliation.getId();
            List<BankReconciliation> bankReconciliationList = bankDealDetailAccessDao.getBankReconciliationById(id);
            if(CollectionUtils.isEmpty(bankReconciliationList)){
                return;
            }
            List<BankDealDetailODSModel> bankDealDetailODSModelList = bankDealDetailAccessDao.queryODSByMainid(id);
            if(CollectionUtils.isEmpty(bankDealDetailODSModelList)){
                return;
            }
            BankReconciliation bankReconciliationFromDB = bankReconciliationList.get(0);
            String osdId = bankDealDetailODSModelList.get(0).getId();
            bankReconciliationFromDB.put(DealDetailEnumConst.ODSID,osdId);
            //step2:根据状态判断走哪个规则
            Short processStatus = bankReconciliationFromDB.getProcessstatus();
            Integer index = null;
            if(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_RECEIVEPAY_SUCC.getStatus().equals(processStatus)||
                    DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_SUCC.getStatus().equals(processStatus)){
                //流程处理从单据关联开始执行
                index = 1;
            }
            if(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_GENERATE_SUCC.getStatus().equals(processStatus)){
                //流程处理从生单流程开始执行，依次执行生单、挂账生单、发布认领
                index=3;
            }
            if(index == null){
                return;
            }
            if(index!=null){
                //step3:构建上下文，调用流水处理器
                List<BankDealDetailWrapper> bankDealDetailWrappers = DealDetailUtils.convertBankReconciliationToWrapper(Arrays.asList(bankReconciliationFromDB));
                if(!CollectionUtils.isEmpty(bankDealDetailWrappers)){
                    BankDealDetailWrapper bankDealDetailWrapper = bankDealDetailWrappers.get(0);
                    //step3:查询过程表
                    List<Object> ids = new ArrayList<>();
                    ids.add(bankReconciliation.getId());
                    List<DealDetailRuleExecRecord> dealDetailRuleExecRecords = bankDealDetailAccessDao.queryDealDetailRuleExecRecordByMainid(ids);
                    DealDetailRuleExecRecord dealDetailRuleExecRecord = null;
                    if(!CollectionUtils.isEmpty(dealDetailRuleExecRecords)){
                        dealDetailRuleExecRecord = dealDetailRuleExecRecords.get(0);
                        CtmJSONArray ctmJSONArray = CtmJSONArray.parseArray(dealDetailRuleExecRecord.getExerules());
                        List<BankDealDetailRuleResult> bankDealDetailRuleResults = new ArrayList<>();
                        try{
                            if(!CollectionUtils.isEmpty(ctmJSONArray)){
                                for(Object object : ctmJSONArray){
                                    BankDealDetailRuleResult bankDealDetailRuleResult = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(object), BankDealDetailRuleResult.class);
                                    bankDealDetailRuleResults.add(bankDealDetailRuleResult);
                                }
                            }
                        }catch (Exception e){
                            log.error("过程表json解析错误",e);
                        }
                        bankDealDetailWrapper.setRuleList(bankDealDetailRuleResults);
                        bankDealDetailWrapper.setDealDetailRuleExecRecord(dealDetailRuleExecRecord);
                        bankDealDetailManageAccess.bankDealDetailManageAccessByProcess(bankDealDetailWrappers,Arrays.asList(bankReconciliationFromDB), BankDealDetailProcessChainImpl.get().code(null),index,dealDetailRuleExecRecord.getTraceid(),dealDetailRuleExecRecord.getRequestseqno());
                    }

                }
            }
        }catch (Exception e){
            log.error("【智能流水】导入后置规则，执行流程失败",e);
        }
    }
    /**
     * 导入数据根据筛选条件过滤
     *
     * @param journal
     * @param map
     */
    private void checkImportDate(BankReconciliation journal, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) map.get("param");
        Map<String, Object> map1 = billDataDto.getMapCondition();
        if (ValueUtils.isNotEmpty(map1)) {
            String accentity = (String) map1.get(IBussinessConstant.ACCENTITY);
            String bankaccount = (String) map1.get("bankaccount");
            String currency = (String) map1.get("currency");
            String bankreconciliationscheme = map1.get("bankreconciliationscheme").toString();
            if (!bankreconciliationscheme.equals(journal.get("bankreconciliationscheme").toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102228"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A2","导入对账方案与当前对账方案不匹配") /* "导入对账方案与当前对账方案不匹配" */);
            }
            if (!accentity.equals(journal.getAccentity())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102229"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A3","导入的会计主体与当前会计主体不一致!") /* "导入的会计主体与当前会计主体不一致!" */);
            }
            if (!bankaccount.equals(journal.getBankaccount())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102230"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A0","导入的银行账户与当前银行账户不一致!") /* "导入的银行账户与当前银行账户不一致!" */);
            }
            if (!currency.equals(journal.getCurrency())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102231"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807A1","导入的币种与当前币种不一致!") /* "导入的币种与当前币种不一致!" */);
            }
        }
    }
}
