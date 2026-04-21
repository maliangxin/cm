package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.iuap.yms.lock.YmsScopeLockManager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetTransferAccountManagerService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.intelligentapproval.CmpIntelligentAudit;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.SystemType;
import com.yonyoucloud.fi.cmp.stwb.StwbBillCheckService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.transferaccount.service.ITransferAccountPushService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component("taSubmitRule")
@Slf4j
public class TaSubmitRule extends AbstractCommonRule {

    @Resource
    private StwbBillCheckService stwbBillCheckService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private CheckStatusService checkStatusService;

    @Autowired
    private CmpBudgetTransferAccountManagerService cmpBudgetTransferAccountManagerService;

    @Autowired
    private CmpIntelligentAudit cmpIntelligentAudit;

    @Autowired
    private TransTypeQueryService transTypeQueryService;

    @Autowired
    @Qualifier("ymsGlobalScopeLockManager")
    protected YmsScopeLockManager ymsScopeLockManager;

    @Autowired
    private ITransferAccountPushService iTransferAccountPushService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BizObject bizObject = getBills(billContext, map).get(0);
        BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
        if (null == currentBill) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100000"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802EB","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
        Short auditStatus = Short.parseShort(currentBill.get("auditstatus").toString());
        if (auditStatus != null && auditStatus.equals(AuditStatus.Complete.getValue())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100889"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F5C6805C0003E","单据[%s]已审批") /* "单据[%s]已审批" */,currentBill.get(ICmpConstant.CODE).toString()));
        }
        //对于rpc传入的暂存数据进行校验
        checkForDraftRpc(bizObject);
        // 增加并发锁
        //针对虚拟户上锁，直到事务结束，避免并发
        if (!ymsScopeLockManager.tryTxScopeLock(bizObject.getId().toString())) {
            throw new CtmException(new CtmErrorCode("033-502-100652"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180747",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2E85405780011", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */) /* "该数据正在处理，请稍后重试！" */);
        }

        Map<String, Object> autoConfigMap = cmCommonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
        if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
            //走影像
            BillBiz.executeRule("shareSubmit", billContext, map);
        }
        TransferAccount transferAccount = new TransferAccount();
        transferAccount.init(currentBill);

        //冗余数据调用结算校验
        String checkBillNo = null;
        if (transferAccount.getCheckid() != null) {
            CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());
            //支票编号
            checkBillNo = checkStock.getCheckBillNo();
        }

        //判断是否为支票业务
        int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
        if(!transferAccount.getSettlestatus().equals(SettleStatus.SettledRep)) {
            //iTransferAccountPushService.pushBill(getBills(billContext, map), true, settlemode, checkBillNo);//推送资金结算
        }

        //TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId());
        if (null == currentBill.get(ICmpConstant.IS_WFCONTROLLED) || !currentBill.getBoolean(ICmpConstant.IS_WFCONTROLLED) ) {
            //不影响原有逻辑，提交时结算检查
            this.GJCheck(billContext, map, currentBill);
            if (transferAccount == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
            }

            //int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
            if (settlemode == 0 && transferAccount.getCheckid() != null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100890"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1894FDA005B80001","结算方式为银行业务，支票编号只能为空，请重新编辑后提交") /* "结算方式为银行业务，支票编号只能为空，请重新编辑后提交" */);
            }
            // 未启动审批流，单据直接审批通过
            cmpIntelligentAudit.auditStart(currentBill, IBillNumConstant.TRANSFERACCOUNT, ICmpConstant.CM_CMP_TRANSFERACCOUNT, BusinessPart.submit.getValue());
            boolean budget = cmpBudgetTransferAccountManagerService.budget(transferAccount);
            if (budget) {
                TransferAccount tA = (TransferAccount) bizObject;
                tA.setIsOccupyBudget(OccupyBudget.PreSuccess.getValue());
                cmpBudgetTransferAccountManagerService.updateOccupyBudget(tA, OccupyBudget.PreSuccess.getValue());
            }
            RuleExecuteResult resultAudit = BillBiz.executeRule(ICmpConstant.AUDIT, billContext, map);
            //更新结算状态
            if (transferAccount.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue()) {
                TransferAccount finalTransferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, transferAccount.getId());
                finalTransferAccount.setSettlestatus(SettleStatus.SettleProssing);
                finalTransferAccount.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, finalTransferAccount);
            }
            resultAudit.setCancel(true);
            return resultAudit;
        }else {
            short verifyState = ValueUtils.isNotEmptyObj(currentBill.get("verifystate")) ? Short.parseShort(currentBill.get("verifystate").toString()) : (short) -1;
            if (VerifyState.TERMINATED.getValue() == verifyState) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100104"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AA", "流程已终止，不允许提交单据！") /* "流程已终止，不允许提交单据！" */);
            }
            if (currentBill.get("checkid") != null) {
                Long checkId = currentBill.get("checkid");
                CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
                if (checkStock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100891"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804AB", "支票不存在，请重新选择支票") /* "支票不存在，请重新选择支票" */);
                }
                //1。查询现金参数 是否推送结算 是
                Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
                if (!checkFundTransfer) {
                    // 更新支票状态为“在开票”
                    checkStock.setIsLock(CmpLock.YES.getValue());
                    checkStock.setCheckBillStatus(CmpCheckStatus.Billing.getValue());
                    checkStock.setInputBillNo(currentBill.get("code"));
                    checkStock.setSysNo(String.valueOf(SystemType.CashManager.getIndex()));
                    checkStock.setEntityStatus(EntityStatus.Update);
                    checkStatusService.recordCheckStatusByCheckId(checkStock.getId(),checkStock.getCheckBillStatus());
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }

                if (transferAccount == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100062"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041804CE","当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }

                //int settlemode = cmCommonService.getServiceAttr(transferAccount.getSettlemode());
                if (settlemode == 0 && transferAccount.getCheckid() != null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100890"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1894FDA005B80001","结算方式为银行业务，支票编号只能为空，请重新编辑后提交") /* "结算方式为银行业务，支票编号只能为空，请重新编辑后提交" */);
                }
            }

            //国际相关，新增结算检查,列表提交时
            if ("cm_transfer_account_list".equals(billContext.getBillnum())){
                //需要进行结算检查
                if (bizObject != null && bizObject.get("billCheckFlag") != null && bizObject.getBoolean("billCheckFlag")){
                    CtmJSONObject billCheckResult = stwbBillCheckService.transferSubmitBillCheck(transferAccount);
                    if ("1".equals(billCheckResult.getString("checkFlag"))){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100892"),billCheckResult.getString("checkMsg"));
                    }
                }
            }
            cmpIntelligentAudit.auditStart(currentBill, IBillNumConstant.TRANSFERACCOUNT, ICmpConstant.CM_CMP_TRANSFERACCOUNT, BusinessPart.submit.getValue());
            return new RuleExecuteResult();
        }
    }

    /**
     * 不影响原有逻辑，提交时结算检查
     * @param billContext
     * @param map
     * @param currentBill
     * @throws Exception
     */
    private void GJCheck(BillContext billContext, Map<String, Object> map, BizObject currentBill) throws Exception{
        //国际相关，新增结算检查,列表提交时
        if ("cm_transfer_account_list".equals(billContext.getBillnum())){
            //需要进行结算检查
            if (currentBill!=null && currentBill.get("billCheckFlag") !=null && currentBill.getBoolean("billCheckFlag")){
                TransferAccount transferAccount = new TransferAccount();
                transferAccount.init(currentBill);
                CtmJSONObject billCheckResult = stwbBillCheckService.transferSubmitBillCheck(transferAccount);
                if ("1".equals(billCheckResult.getString("checkFlag"))){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100893"),billCheckResult.getString("checkMsg"));
                }
            }
        }
    }

    //通过交易类型校验不同的参数
    private void checkForDraftRpc(BizObject bizObject) throws Exception {
        //查询交易类型code
        String tradeTypeCode = bizObject.get("tradetype_code"); //交易类型编码
        if (null == tradeTypeCode &&  null != bizObject.get("tradetype")) {
            BdTransType tradeType = transTypeQueryService.findById(bizObject.get("tradetype"));
            if (tradeType == null) {
                return;
            }
            tradeTypeCode = tradeType.getCode();
        }
        StringBuilder errorMsgBuilder  = new StringBuilder();
        errorMsgBuilder.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400633", "提交失败:单据[单据编号]") /* "提交失败:单据[单据编号]" */ + bizObject.get("code")+",");
        String errorMsg = "";
        //对应code 做响应的校验
        switch (tradeTypeCode) {
            case "BT":  //交易类型为银行转账
                if (bizObject.get("payBankAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180123", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400636", "交易类型为银行转账，付款银行账户为必输项") /* "交易类型为银行转账，付款银行账户为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180124", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400637", "交易类型为银行转账，收款银行账户为必输项") /* "交易类型为银行转账，收款银行账户为必输项" */);
                }
                if (bizObject.get("payBankAccount") != null && bizObject.get("payBankAccount") != null &&
                        bizObject.get("payBankAccount").equals(bizObject.get("recBankAccount"))) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180127", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400639", "付款与收款账号不能为同一个") /* "付款与收款账号不能为同一个" */);
                }
                break;
            case "SC":
                if (bizObject.get("payCashAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062D", "交易类型为缴存现金，付款现金账户编码为必输项") /* "交易类型为缴存现金，付款现金账户编码为必输项" */);
                }
                if (bizObject.get("recBankAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418012E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062F", "交易类型为缴存现金，收款银行账户为必输项") /* "交易类型为缴存现金，收款银行账户为必输项" */);
                }
                break;
            case "EC":
                if (bizObject.get("payBankAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180132", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400631", "交易类型为提取现金，付款银行账户为必输项") /* "交易类型为提取现金，付款银行账户为必输项" */);
                }
                if (bizObject.get("recCashAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180135", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400632", "交易类型为提取现金，收款现金账户编码为必输项") /* "交易类型为提取现金，收款现金账户编码为必输项" */);
                }
                break;
            case "CT":
                if (bizObject.get("payCashAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180137", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400634", "交易类型为现金互转，付款现金账户为必输项") /* "交易类型为现金互转，付款现金账户为必输项" */);
                }
                if (bizObject.get("recCashAccount") == null) {
                    errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400635", "交易类型为现金互转，收款现金账户编码为必输项") /* "交易类型为现金互转，收款现金账户编码为必输项" */);
                }
                break;
            case "TPT":
                if (bizObject.get(ICmpConstant.VIRTUALBANK) != null) {
                    if (bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.BankToVirtual.getValue())) {
                        if (!ValueUtils.isNotEmptyObj(ICmpConstant.PAYBANKACCOUNT)) {
                            errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418013D", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400638", "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账户为必输项") /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，付款银行账户为必输项" */);
                        }
                        if (!ValueUtils.isNotEmptyObj(ICmpConstant.COLLVIRTUALACCOUNT)) {
                            errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180140", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540063A", "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，收款虚拟账户为必输项") /* "交易类型为第三方转账，三方转账类型为银行账户转虚拟账户，收款虚拟账户为必输项" */);
                        }
                    } else if (bizObject.get(ICmpConstant.VIRTUALBANK).equals(VirtualBank.VirtualToBank.getValue())) {
                        if (!ValueUtils.isNotEmptyObj(ICmpConstant.RECBANKACCOUNT)) {
                            errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180143", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062E", "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账户为必输项") /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，收款银行账户为必输项" */);
                        }
                        if (!ValueUtils.isNotEmptyObj(ICmpConstant.PAYVIRTUALACCOUNT)) {
                            errorMsg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180145", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400630", "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，付款虚拟账户为必输项") /* "交易类型为第三方转账，三方转账类型为虚拟账户转银行账户，付款虚拟账户为必输项" */);
                        }
                    }
                }
                break;

            default:
                break;
        }
        if(!StringUtils.isEmpty(errorMsg)){
            errorMsgBuilder.append(errorMsg);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100894"),errorMsgBuilder.toString());
        }
    }
}
