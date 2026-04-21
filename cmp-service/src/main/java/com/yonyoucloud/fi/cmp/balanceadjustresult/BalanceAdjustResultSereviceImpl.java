package com.yonyoucloud.fi.cmp.balanceadjustresult;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.fileservice.sdk.module.pojo.CooperationFileInfo;
import com.yonyou.iuap.fileservice.sdk.service.CooperationFileManageService;
import com.yonyou.iuap.fileservice.sdk.service.CooperationFileQueryService;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.balanceadjust.service.impl.BalanceBatchCommonService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BalanceadjustBankreconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.enums.BalanceStateEnum;
import com.yonyoucloud.fi.cmp.journal.BalanceadjustJournal;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.vo.BalanceAdjustResultVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BalanceAdjustResultSereviceImpl implements BalanceAdjustResultSerevice {
    public static  String BANKRECONCILIATIONSCHEME = "bankreconciliationscheme";
    private static final String CURRENCY = "currency";
    private static final String BANK_ACCOUNT = "bankaccount";
    private static final String SETTLE_STATUS = "settlestatus";
    private static final String DZ_DATE = "dzdate";
    private static final String AUDIT_STATUS = "auditstatus";
    private static final String INIT_FLAG = "initflag";
    private static final int GETLOCK_FAIL = 0;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;
    @Autowired
    private BalanceBatchCommonService balanceBatchCommonService;
    @Autowired
    private FIBillService fiBillService;
    @Autowired
    private IBillCodeComponentService billCodeComponentService;
    @Autowired
    ProcessService processService;
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    CooperationFileManageService cooperationFileManageService;
    @Autowired
    CooperationFileQueryService cooperationFileQueryService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    CurrencyQueryService currencyQueryService;
    @Autowired
    CmpCheckService cmpCheckService;
    /**
     * 根据对账方案查询凭证日记账
     * @param balanceAdjustResult
     * @param filterArgs
     * @param ctmJson 页面过滤信息
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = RuntimeException.class)
    public CtmJSONObject add(BalanceAdjustResult balanceAdjustResult, String filterArgs,CtmJSONObject ctmJson) throws  Exception  {
        CtmJSONObject result = new CtmJSONObject();
        if(balanceAdjustResult != null){
            //单组织逻辑
            if(FIDubboUtils.isSingleOrg()){
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
                if(singleOrg!=null){
                    balanceAdjustResult.setAccentity(singleOrg.get("id"));
                }
            }
            if (StringUtils.isEmpty(balanceAdjustResult.getAccentity())) {//会计主体
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050024", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D87076E05080015", "资金组织不能为空！") /* "资金组织不能为空！" */) /* "资金组织不能为空！" */);
            }
            if (balanceAdjustResult.getBankreconciliationscheme() == null) {//银行账户
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C0",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F1", "对账方案不能为空！") /* "对账方案不能为空！" */) /* "对账方案不能为空！" */);
            }
            if (StringUtils.isEmpty(balanceAdjustResult.getBankaccount())) {//银行账户
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C1",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F2", "银行账户不能为空！") /* "银行账户不能为空！" */) /* "银行账户不能为空！" */);
            }
            if (StringUtils.isEmpty(balanceAdjustResult.getCurrency())) {//币种
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C4",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F3", "币种不能为空！") /* "币种不能为空！" */) /* "币种不能为空！" */);
            }
            if (balanceAdjustResult.getBanktzye() == null) {
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C6",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F5", "银行调整余额不能为空！") /* "银行调整余额不能为空！" */) /* "银行调整余额不能为空！" */);
            }

            balanceBatchCommonService.executeInOneServiceLock((int lockStatus)->{
                if(lockStatus == GETLOCK_FAIL){
                    //加锁失败添加报错信息 刷新进度
                   throw new Exception(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F6", "[%s+%s]已被其他用户操作，请稍后再试") /* "[%s+%s]已被其他用户操作，请稍后再试" */,ctmJson.getString("bankaccount_name"),ctmJson.getString("currency_name")) /* "[%s+%s]已被其他用户操作，请稍后再试" */);
                }
                extracted(balanceAdjustResult, filterArgs, ctmJson, result);
            }).apply(balanceAdjustResult.getBankaccount(), balanceAdjustResult.getCurrency());
        } else {
            throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BE",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F7", "余额调节表列表新增数据不能为空！") /* "余额调节表列表新增数据不能为空！" */) /* "余额调节表列表新增数据不能为空！" */);
        }
        return result;
    }

    private void extracted(BalanceAdjustResult balanceAdjustResult, String filterArgs, CtmJSONObject ctmJson, CtmJSONObject result) throws Exception {
        BalanceAdjustResult oldBalanceAdjustResult = queryExistsByCond(balanceAdjustResult);//查询是否存在
        String msg = "";
        balanceAdjustResult.setBalenceState(balanceAdjustResult.getJournaltzye().compareTo(balanceAdjustResult.getBanktzye()) == 0 ?
                BalanceStateEnum.AlreadyLeveled.getIndex() : BalanceStateEnum.NotAlreadyLeveled.getIndex());//是否调平
        if (BalanceStateEnum.NotAlreadyLeveled.getIndex().equals(balanceAdjustResult.getBalenceState())) {
            throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C8",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006FA", "余额调节未平，不允许保存") /* "余额调节未平，不允许保存" */) /* "余额调节未平，不允许保存" */);
        }
        //发起审批流如果配了的话
        ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
        param.setCategory(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        param.setBillTypeId(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        param.setOrgId(balanceAdjustResult.getAccentity());
        RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
        Object processDefinition = repositoryService.checkProcessDefinition(param);
        Map<String, Object> queryTransType = cmCommonService.queryTransTypeByForm_id(ICmpConstant.CM_CMP_BALANCEADJUSTRESULT);
        //单据类型下没有挂审批流的话就找交易类型下的
        if (!((ObjectNode) processDefinition).get("hasProcessDefinition").booleanValue()) {
            if (MapUtils.isNotEmpty(queryTransType)) {
                param.setCategory(queryTransType.get("id").toString());
                processDefinition = repositoryService.checkProcessDefinition(param);
            }
        }
        if(oldBalanceAdjustResult != null){//更新
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            if(!sf.format(balanceAdjustResult.getDzdate()).equals(sf.format(oldBalanceAdjustResult.getDzdate()))){
                EnterpriseBankAcctVO vo = baseRefRpcService.queryEnterpriseBankAccountById(balanceAdjustResult.getBankaccount());
                throw new Exception(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006EC", "银行账户【%s】已经存在截止日期【%s】的余额调节表已经生成，请按时间顺序生成，新增的余额调节表日期需大于【%s】") /* "银行账户【%s】已经存在截止日期【%s】的余额调节表已经生成，请按时间顺序生成，新增的余额调节表日期需大于【%s】" */,vo.getAcctName(),sf.format(oldBalanceAdjustResult.getDzdate()),sf.format(oldBalanceAdjustResult.getDzdate())));
            }
            if(null != oldBalanceAdjustResult.getAuditstatus() && (oldBalanceAdjustResult.getAuditstatus() == AuditStatus.Complete.getValue()|| oldBalanceAdjustResult.getAuditstatus() == BalanceAuditStatus.SUBMITED.getValue())) {
                EnterpriseBankAcctVO vo = baseRefRpcService.queryEnterpriseBankAccountById(balanceAdjustResult.getBankaccount());
                throw new Exception(String.format(InternationalUtils.getMessageWithDefault("\n" +
                        "UID:P_CM-BE_1F05178205400005",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006EE", "银行账户【%s】已存在截止日期【%s】的余额调节表，且已审批中或审核完成，不允许再次生成，请检查！") /* "银行账户【%s】已存在截止日期【%s】的余额调节表，且已审批中或审核完成，不允许再次生成，请检查！" */) /* "银行账户【%s】已存在截止日期【%s】的余额调节表，且已审批中或审核完成，不允许再次生成，请检查！" */, vo.getAcctName(), sf.format(balanceAdjustResult.getDzdate())));
            }
            oldBalanceAdjustResult.setJournalye(balanceAdjustResult.getJournalye());//日记账余额
            oldBalanceAdjustResult.setJournalyedetailinfo(balanceAdjustResult.getJournalyedetailinfo());//日记账余额详情
            oldBalanceAdjustResult.setJournalyhyf(balanceAdjustResult.getJournalyhyf());
            oldBalanceAdjustResult.setJournalyhys(balanceAdjustResult.getJournalyhys());
            oldBalanceAdjustResult.setJournaltzye(balanceAdjustResult.getJournaltzye());
            oldBalanceAdjustResult.setBankqyyf(balanceAdjustResult.getBankqyyf());
            oldBalanceAdjustResult.setBankqyys(balanceAdjustResult.getBankqyys());
            oldBalanceAdjustResult.setBankye(balanceAdjustResult.getBankye());
            oldBalanceAdjustResult.setBanktzye(balanceAdjustResult.getBanktzye());
            oldBalanceAdjustResult.setBalenceState(balanceAdjustResult.getJournaltzye().compareTo(balanceAdjustResult.getBanktzye()) == 0 ?
                    BalanceStateEnum.AlreadyLeveled.getIndex() : BalanceStateEnum.NotAlreadyLeveled.getIndex());
            if(oldBalanceAdjustResult.getAuditstatus() == null){
                oldBalanceAdjustResult.setAuditstatus(AuditStatus.Incomplete.getValue());
            }
            oldBalanceAdjustResult.setModifyDate(DateUtils.getNowDateShort2());
            oldBalanceAdjustResult.setModifyTime(new Date());
            oldBalanceAdjustResult.setModifier(AppContext.getCurrentUser().getName());
            oldBalanceAdjustResult.setModifierId(AppContext.getCurrentUser().getId());
            //202603 产品修改逻辑：更新余额调节表信息时，同步更新创建人
            oldBalanceAdjustResult.setCreator(AppContext.getCurrentUser().getName());//新增人名称
            oldBalanceAdjustResult.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
            oldBalanceAdjustResult.setUncheckflag(balanceAdjustResult.getUncheckflag());
            if (MapUtils.isNotEmpty(queryTransType)&&Objects.isNull(oldBalanceAdjustResult.getTradetype())) {
                oldBalanceAdjustResult.setTradetype(queryTransType.get("id").toString());
            }
            if (!((ObjectNode) processDefinition).get("hasProcessDefinition").booleanValue()) {
                oldBalanceAdjustResult.setIsWfControlled(false);
            } else {
                oldBalanceAdjustResult.setIsWfControlled(true);
            }
            EntityTool.setUpdateStatus(oldBalanceAdjustResult);
            MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, oldBalanceAdjustResult);
            msg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F8", "更新数据成功,已成功生成[%s]余额调节表") /* "更新数据成功,已成功生成[%s]余额调节表" */, sf.format(balanceAdjustResult.getDzdate()));
        } else {//新增操作
            String currency = balanceAdjustResult.getCurrency();
            if(StringUtil.isNotEmpty(currency)){
                balanceAdjustResult.setCurrency(currency);
            } else {
                throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C7",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F9", "银行币别信息查询为空！") /* "银行币别信息查询为空！" */) /* "银行币别信息查询为空！" */);
            }
            balanceAdjustResult.setTenant(AppContext.getTenantId());
            balanceAdjustResult.setCreateTime(new Date());
            balanceAdjustResult.setCreateDate(DateUtils.getNowDateShort2());
            balanceAdjustResult.setPubts(new Date());
            balanceAdjustResult.setCreator(AppContext.getCurrentUser().getName());//新增人名称
            balanceAdjustResult.setCreatorId(AppContext.getCurrentUser().getId());//新增人id
            balanceAdjustResult.setAuditstatus(AuditStatus.Incomplete.getValue());
            balanceAdjustResult.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
            balanceAdjustResult.setEntityStatus(EntityStatus.Insert);
            balanceAdjustResult.setId(ymsOidGenerator.nextId());
            BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                    ICmpConstant.BALANCEADJUSTRESULTOBJECTCODE,
                    IBillNumConstant.BALANCEADJUSTRESULT,
                    AppContext.getTenantId().toString(),
                    balanceAdjustResult.getAccentity(),
                    BalanceAdjustResult.ENTITY_NAME,
                    new BillCodeObj[]{new BillCodeObj(balanceAdjustResult)});
            String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
            balanceAdjustResult.setCode(codelist != null ? codelist[0] : null);
            if (MapUtils.isNotEmpty(queryTransType)&&Objects.isNull(balanceAdjustResult.getTradetype())) {
                balanceAdjustResult.setTradetype(queryTransType.get("id").toString());
            }
            if (!((ObjectNode) processDefinition).get("hasProcessDefinition").booleanValue()) {
                balanceAdjustResult.setIsWfControlled(false);
            } else {
                balanceAdjustResult.setIsWfControlled(true);
            }
            CmpMetaDaoHelper.insert(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResult);
            msg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CA",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006EF", "新增数据成功") /* "新增数据成功" */) /* "新增数据成功" */;
        }
        //添加余额表明细
        addDetail(balanceAdjustResult, filterArgs, ctmJson);
        if (!Objects.isNull(ctmJson.get("saveAndSubmit")) && ctmJson.getBoolean("saveAndSubmit")) {
            //调用提交规则
            BalanceAdjustResult submitData = queryExistsByCond(balanceAdjustResult);
            // 设置默认提交人
            submitData.put("BPM_EXT_SUBMIT_OPERATOR_",AppContext.getCurrentUser().getYhtUserId());
            if (submitData != null) {
                beforeSubmitCheck(balanceAdjustResult);
                //事物结束后异步执行
                CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @SneakyThrows
                    @Override
                    public void afterCommit() {
                        try {
                            RobotExecutors.runAs(AppContext.getYTenantId(), new Callable() {
                                @Override
                                public Object call() throws Exception {

                                    BillDataDto dataDto = new BillDataDto();
                                    List<BalanceAdjustResult> dataList = new ArrayList<>();
                                    if (null == submitData.get(ICmpConstant.IS_WFCONTROLLED) || !submitData.getBoolean(ICmpConstant.IS_WFCONTROLLED)) {
                                        submitData.setAuditstatus(BalanceAuditStatus.Complete.getValue());
                                    } else {
                                        submitData.setAuditstatus(BalanceAuditStatus.SUBMITED.getValue());
                                    }
                                    dataList.add(submitData);
                                    dataDto.setData(dataList);
                                    dataDto.setBillnum(IBillNumConstant.BALANCEADJUSTRESULT);
                                    dataDto.setAction(OperationTypeEnum.SUBMIT.getValue());
                                    BillBiz.batchDo(dataDto, false);
//                                    msg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418010F", "操作成功") /* "操作成功" */;
                                    return null;
                                }
                            }, ctmThreadPoolExecutor.getThreadPoolExecutor());
                        } catch (Exception e) {
                            // log.error("业务单据：" + dataSettled.getBusinessbillnum() + "提交失败：" + e.getMessage());
                            throw e;
                        }
                    }
                });
            }
        }
        BalanceAdjustResult lastData = queryExistsByCond(balanceAdjustResult);
        try {
            List<CooperationFileInfo> cooperationFileInfoList = cooperationFileQueryService.queryBusinessFiles(ICmpConstant.APPCODE, lastData.getId().toString(), InvocationInfoProxy.getTenantid());
            if (CollectionUtils.isNotEmpty(cooperationFileInfoList) && !Objects.isNull(ctmJson.get("fileIds"))) {
                List<String> fileIds = (List<String>) ctmJson.get("fileIds");
                if (!Objects.isNull(fileIds)) {
                    List<String> beforIds = new ArrayList<>();
                    for (CooperationFileInfo item : cooperationFileInfoList) {
                        if (!fileIds.contains(item.getFileId())) {
                            beforIds.add(item.getFileId());
                        }
                    }
                    if (CollectionUtils.isNotEmpty(beforIds)) {
                        //删除已有的附件返回id后前端会刷新保存前的附件
                        cooperationFileManageService.deleteBatchFiles(ICmpConstant.APPCODE, lastData.getId().toString(), beforIds);
                    }
                }
            }
        } catch (Exception e) {
            String message = e.getMessage();
            log.error("================调用文件处理报错 deleteFileByBusiness :" + message, e);
            throw new Exception(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F0", "调用文件处理报错 :") /* "调用文件处理报错 :" */ + message);

        }
        result.put("msg", msg);
        result.put("balanceAdjustResult", lastData);
    }

    @Override
    public void beforeSubmitCheck(BalanceAdjustResult balanceAdjustResult) throws Exception {
        EnterpriseBankAcctVO enterpriseBankAcctVO= enterpriseBankQueryService.findById(balanceAdjustResult.get(ICmpConstant.BANKACCOUNT));
        CurrencyTenantDTO currencyTenantDTO = currencyQueryService.findById(balanceAdjustResult.get(ICmpConstant.CURRENCY));
        BalanceAdjustResult unaudit = getEarlyUnauditData(balanceAdjustResult);
        if (unaudit != null) {
            throw new com.yonyou.yonbip.ctm.error.CtmException(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */,
                    enterpriseBankAcctVO.getAccount(), currencyTenantDTO.getName(), DateUtils.parseDateToStr(unaudit.getDzdate(),"yyyy-MM-dd")));
        }
    }

    /**
     * 查询当前会计主体下，对账方案下，某一账号是否有对账记录
     * @param balanceAdjustResult
     * @return
     * @throws Exception
     */
    @Override
    public BalanceAdjustResult queryExistsByCond(BalanceAdjustResult balanceAdjustResult) throws Exception {
        //单组织逻辑
        if(FIDubboUtils.isSingleOrg()){
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if(singleOrg!=null){
                balanceAdjustResult.setAccentity(singleOrg.get("id"));
            }
        }
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balanceAdjustResult.getAccentity()));
        conditionGroup.addCondition(QueryCondition.name("bankreconciliationscheme").eq(balanceAdjustResult.getBankreconciliationscheme()));
        conditionGroup.addCondition(QueryCondition.name("bankaccount").eq(balanceAdjustResult.getBankaccount()));
        conditionGroup.addCondition(QueryCondition.name("currency").eq(balanceAdjustResult.getCurrency()));
        conditionGroup.addCondition(QueryCondition.name("dzdate").egt(DateUtils.dateFormat(balanceAdjustResult.getDzdate(),"yyyy-MM-dd")));//对账截至日期
        schema.addOrderBy(new QueryOrderby("dzdate", "desc"));
        schema.addCondition(conditionGroup);
        List<BalanceAdjustResult> billListQuery = MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, schema, null);
        if(CollectionUtils.isNotEmpty(billListQuery)){
            return billListQuery.get(0);
        }
        return null;
    }

    /**
     * 插入余额调节表明细
     * @param balanceAdjustResult
     * @throws Exception
     */
    public void addDetail(BalanceAdjustResult balanceAdjustResult,String filterArgs,CtmJSONObject ctmJson) throws Exception{
        BalanceAdjustResult result = queryExistsByCond(balanceAdjustResult);

        if(result!=null){

            QuerySchema schema = new QuerySchema().addSelect("id");
            QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("balanceadjustresultid").eq(result.getId().toString()));
            schema.addCondition(conditionGroup);

            List<BalanceadjustJournal> oldJournals = MetaDaoHelper.queryObject(BalanceadjustJournal.ENTITY_NAME, schema,null);
            List<BalanceadjustBankreconciliation> oldBankreconciliations = MetaDaoHelper.queryObject(BalanceadjustBankreconciliation.ENTITY_NAME, schema,null);

            //清空数据
            if(oldJournals!=null&&oldJournals.size()>0){
                MetaDaoHelper.delete(BalanceadjustJournal.ENTITY_NAME,oldJournals);
            }

            if(oldBankreconciliations!=null&&oldBankreconciliations.size()>0){
                MetaDaoHelper.delete(BalanceadjustBankreconciliation.ENTITY_NAME,oldBankreconciliations);
            }

            //查询对账方案及银行账号
            BankReconciliationSetting bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,result.getBankreconciliationscheme());
            Integer reconciliationdatasource = bankReconciliationSetting.get("reconciliationdatasource"); //对账方案数据源，1.凭证，2.日记账
            Date enableDate = bankReconciliationSetting.getEnableDate(); //方案启用日期

            //添加数据
            List<Journal> journals = getJournal(enableDate,result,reconciliationdatasource,result.getBankaccount(),filterArgs,ctmJson);
            List<BankReconciliation> bankReconciliations = getBankReconciliation(enableDate,result,reconciliationdatasource,result.getBankaccount());

            List<BalanceadjustJournal> newBalanceadjustJournalJournal = new ArrayList<>();
            List<BalanceadjustBankreconciliation> newBalanceadjustBankreconciliation = new ArrayList<>();


            if(CollectionUtils.isNotEmpty(journals)){
                for (Map<String,Object> map:journals){
                    Journal journal = new Journal();
                    journal.init(map);
                    BalanceadjustJournal balanceadjustJournal = new BalanceadjustJournal();
                    balanceadjustJournal.setCurrency(journal.getCurrency());
                    balanceadjustJournal.setAccentity(journal.getAccentity());
                    //兼容凭证返回
                    if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                        balanceadjustJournal.setDzdate(journal.getDzdate());
                        balanceadjustJournal.setVouchdate(journal.getVouchdate());
                        balanceadjustJournal.setCheckdate(journal.getCheckdate());
                    }else if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                        balanceadjustJournal.setDzdate(map.get("dzdate")==null?null: DateUtils.strToDate(map.get("dzdate").toString()));
                        balanceadjustJournal.setVouchdate(map.get("vouchdate")==null?null:DateUtils.strToDate(map.get("vouchdate").toString()));
                        balanceadjustJournal.setCheckdate(map.get("checkdate")==null?null:DateUtils.strToDate(map.get("checkdate").toString()));
                    }

                    //SOP:CZFW-102600 凭证日记账摘要长度大于255兼容
                    if (journal.getDescription()!=null && journal.getDescription().length() > 255){
                        balanceadjustJournal.setDescription(journal.getDescription().substring(0,255));
                    }else {
                        balanceadjustJournal.setDescription(journal.getDescription());
                    }
                    balanceadjustJournal.setDebitoriSum(journal.getDebitoriSum()); //根据币种保留精度
                    balanceadjustJournal.setCreditoriSum(journal.getCreditoriSum());
                    balanceadjustJournal.setCheckflag(journal.getCheckflag());
                    balanceadjustJournal.setCheckman(journal.getCheckman());
                    balanceadjustJournal.setCheckno(journal.getCheckno());
                    balanceadjustJournal.setBalanceadjustresultid(result.getId());
                    balanceadjustJournal.setSrcitem(journal.getSrcitem());
                    balanceadjustJournal.setBillnum(journal.getBillnum());
                    balanceadjustJournal.setId(ymsOidGenerator.nextId());
                    newBalanceadjustJournalJournal.add(balanceadjustJournal);
                }
            }

            if(CollectionUtils.isNotEmpty(bankReconciliations)){

                for (Map<String,Object> map:bankReconciliations){
                    BankReconciliation bankReconciliation = new BankReconciliation();
                    bankReconciliation.init(map);
                    BalanceadjustBankreconciliation balanceadjustBankreconciliation = new BalanceadjustBankreconciliation();
                    balanceadjustBankreconciliation.setAccentity(bankReconciliation.getAccentity());
                    balanceadjustBankreconciliation.setCurrency(bankReconciliation.getCurrency());
                    balanceadjustBankreconciliation.setDzdate(bankReconciliation.getDzdate());
                    balanceadjustBankreconciliation.setTran_date(bankReconciliation.getTran_date());
                    balanceadjustBankreconciliation.setTran_time(bankReconciliation.getTran_time());
                    balanceadjustBankreconciliation.setDebitamount(bankReconciliation.getDebitamount());
                    balanceadjustBankreconciliation.setCreditamount(bankReconciliation.getCreditamount());
                    balanceadjustBankreconciliation.setCheckflag(bankReconciliation.getCheckflag());
                    balanceadjustBankreconciliation.setOther_checkflag(bankReconciliation.getOther_checkflag());
                    balanceadjustBankreconciliation.setCheckdate(bankReconciliation.getCheckdate());
                    balanceadjustBankreconciliation.setOther_checkdate(bankReconciliation.getOther_checkdate());
                    balanceadjustBankreconciliation.setCheckman(bankReconciliation.getCheckman());
                    balanceadjustBankreconciliation.setCheckno(bankReconciliation.getCheckno());
                    balanceadjustBankreconciliation.setOther_checkno(bankReconciliation.getOther_checkno());
                    balanceadjustBankreconciliation.setAutobill(bankReconciliation.getAutobill());
                    balanceadjustBankreconciliation.setBalanceadjustresultid(result.getId());
                    balanceadjustBankreconciliation.setBank_seq_no(bankReconciliation.getBank_seq_no());
                    balanceadjustBankreconciliation.setThirdserialno(bankReconciliation.getThirdserialno());
                    balanceadjustBankreconciliation.setTo_acct_no(bankReconciliation.getTo_acct_no());
                    balanceadjustBankreconciliation.setTo_acct_name(bankReconciliation.getTo_acct_name());
                    balanceadjustBankreconciliation.setUse_name(bankReconciliation.getUse_name());
                    balanceadjustBankreconciliation.setRemark(bankReconciliation.getRemark());
                    balanceadjustBankreconciliation.setOrgid(bankReconciliation.getOrgid());
                    balanceadjustBankreconciliation.setId(ymsOidGenerator.nextId());
                    newBalanceadjustBankreconciliation.add(balanceadjustBankreconciliation);
                }
            }

            CmpMetaDaoHelper.insert(BalanceadjustJournal.ENTITY_NAME,newBalanceadjustJournalJournal);
            CmpMetaDaoHelper.insert(BalanceadjustBankreconciliation.ENTITY_NAME,newBalanceadjustBankreconciliation);
        }
    }

    /**
     * 获取余额调节表日记账明细
     * @param balanceAdjustResult
     * @param reconciliationdatasource
     * @param bankaccount
     * @param filterArgs
     * @return
     * @throws Exception
     */
    private List<Journal> getJournal(Date enableDate,BalanceAdjustResult balanceAdjustResult,Integer reconciliationdatasource,String bankaccount,String filterArgs,CtmJSONObject ctmJson) throws Exception{
        List<Journal> journals = new ArrayList<>();
        if(balanceAdjustResult==null){
            return journals;
        }


        if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
            //账户共享，日记账余额要过滤授权使用组织
            PlanParam planParam = new PlanParam(null,null,balanceAdjustResult.getBankreconciliationscheme().toString());
            //银行日记账要使用返回账户使用组织的接口
            List<BankReconciliationSettingVO> infoList = cmpCheckService.findUseOrg(planParam);
            Set<String> useorgids = new HashSet<>();
            for (BankReconciliationSettingVO settingVO : infoList){
                if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()){
                    useorgids.add(settingVO.getUseOrg());
                }
            }
            if (useorgids.size() == 0){
                useorgids.add("0");
            }
            QuerySchema schema = new QuerySchema().addSelect("*");
            QueryConditionGroup mainGroup = QueryConditionGroup.and(
                    //银行日记账根据所属组织过滤
//                    QueryCondition.name("parentAccentity").eq(balanceAdjustResult.getAccentity()),
                    //日记账余额要过滤授权使用组织
                    QueryCondition.name("accentity").in(useorgids),
                    QueryCondition.name(BANK_ACCOUNT).eq(bankaccount),
                    QueryCondition.name(CURRENCY).eq(balanceAdjustResult.getCurrency()));
            QueryConditionGroup initGroup = QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(1),
                    QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(balanceAdjustResult.getBankreconciliationscheme()),QueryCondition.name("checkflag").eq(0));  //期初
            QueryConditionGroup journalGroup = QueryConditionGroup.and(QueryCondition.name(DZ_DATE).egt(enableDate),
                    QueryCondition.name(DZ_DATE).elt(balanceAdjustResult.getDzdate()),
                    QueryCondition.name(SETTLE_STATUS).eq("2"),
                    QueryCondition.name(AUDIT_STATUS).eq(AuditStatus.Complete.getValue()),
                    QueryCondition.name("checkflag").eq(0),
                    QueryCondition.name("billtype").not_eq(EventType.ExchangeBill.getValue()));
            QueryConditionGroup childrenGroup = QueryConditionGroup.or(journalGroup,initGroup);
            QueryConditionGroup group = QueryConditionGroup.and(mainGroup,childrenGroup);
            schema.addCondition(group);
            journals = MetaDaoHelper.query(Journal.ENTITY_NAME, schema);
        }else if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
            journals = getVoucheList(filterArgs,ctmJson);
        }
        return journals;
    }

    /**
     * 获取余额对账单明细
     * @param balanceAdjustResult
     * @param bankaccount
     * @return
     * @throws Exception
     */
    private List<BankReconciliation> getBankReconciliation(Date enableDate,BalanceAdjustResult balanceAdjustResult,Integer reconciliationdatasource,String bankaccount) throws Exception{
        List<BankReconciliation> bankReconciliations = new ArrayList<>();
        if(balanceAdjustResult==null){
            return bankReconciliations;
        }
        QuerySchema schema = new QuerySchema().addSelect("*");
        QueryConditionGroup mainGroup = QueryConditionGroup.and(
                //账户共享，去除所属组织概念
//                QueryCondition.name(IBussinessConstant.ACCENTITY).eq(balanceAdjustResult.getAccentity()),
                QueryCondition.name(BANK_ACCOUNT).eq(bankaccount),
                QueryCondition.name(CURRENCY).eq(balanceAdjustResult.getCurrency()));
        QueryConditionGroup initGroup = QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(1),
                QueryCondition.name(BANKRECONCILIATIONSCHEME).eq(balanceAdjustResult.getBankreconciliationscheme()));  //期初
        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
            initGroup.addCondition(QueryCondition.name("other_checkflag").eq(0));
        }else{
            initGroup.addCondition(QueryCondition.name("checkflag").eq(0));
        }
        //CZFW-379654 余额调节表生成时，详情信息需要增加期初过滤条件
        QueryConditionGroup bankGroup =  QueryConditionGroup.and(QueryConditionGroup.and(QueryCondition.name(INIT_FLAG).eq(0),
                        QueryCondition.name(DZ_DATE).egt(enableDate)),
                QueryConditionGroup.and(QueryCondition.name(DZ_DATE).elt(balanceAdjustResult.getDzdate())));
        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
            bankGroup.addCondition(QueryCondition.name("other_checkflag").eq(0));
        }else{
            bankGroup.addCondition(QueryCondition.name("checkflag").eq(0));
        }
        //无需处理是否统计到余额调节表 为否时过滤处理状态serialdealtype=5的数据
        boolean isNoProcess = BankreconciliationUtils.isNoProcess(balanceAdjustResult.getAccentity());
        if (!isNoProcess){
            QueryConditionGroup g1 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").not_eq(5));
            QueryConditionGroup g2 = QueryConditionGroup.and(QueryCondition.name("serialdealtype").is_null());
            bankGroup.appendCondition(QueryConditionGroup.or(g1,g2));
        }
        QueryConditionGroup chidrenGroup = QueryConditionGroup.or(bankGroup,initGroup);
        QueryConditionGroup group = QueryConditionGroup.and(mainGroup,chidrenGroup);
        //银行流水对账银行账户数据权限适配
        try {
            String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
            if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                group.appendCondition(QueryCondition.name("bankaccount").in(Arrays.asList(bankAccountPermissions)));
            }
        }catch (Exception e){
            log.error("获取数据权限时错误" + e);
        }
        schema.addCondition(group);
        bankReconciliations = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, schema);
        return bankReconciliations;
    }

    /**
     * 递归获取总账返回记录
     * @param filterArgs
     * @return
     */
    private List<Journal> getVoucheList(String filterArgs,CtmJSONObject ctmJson) throws Exception{
        //对账方案id
        String bankreconciliationscheme = ctmJson.getString("bankreconciliationscheme");
        //银行账户
        String bankaccount = ctmJson.getString("bankaccount");
        //币种
        String currency = ctmJson.getString("currency");
        //查询对账方案下使用组织的账簿
        PlanParam planParam = new PlanParam(null,null,bankreconciliationscheme.toString());
        List<BankReconciliationSettingVO> infoList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);
        CtmJSONObject argsJson = CtmJSONObject.parseObject(filterArgs);
        CtmJSONObject conditionJson = argsJson.getJSONObject("condition");
        CtmJSONObject pageJson = argsJson.getJSONObject("page");

        List<Journal> records = new ArrayList<>();
        List<String> checkedbookids = new ArrayList<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            //需要过滤对应币种和账户
            if (!settingVO.getBankAccount().equals(bankaccount) || !settingVO.getCurrency().equals(currency)){
                continue;
            }
            //CZFW-423757 问题修复；后台生成余额调节表凭证详情时，一个账簿只查询一次
            if (checkedbookids.contains(settingVO.getAccBook())){
                continue;
            }else {
                checkedbookids.add(settingVO.getAccBook());
            }
            //增加银行账户数据权限后的适配，构造 bankAccounts 数组元素
            List<CtmJSONObject> bankAccountsList = new ArrayList<>();
            CtmJSONObject bankAccountObj = new CtmJSONObject();
            bankAccountObj.put("bankAccount", settingVO.getBankAccount());
            bankAccountObj.put("currency", settingVO.getCurrency());
            bankAccountsList.add(bankAccountObj);
            // 将 bankAccounts 数组添加到 conditionJson 中
            conditionJson.put("bankAccounts", bankAccountsList);

            conditionJson.put("accbookId",settingVO.getAccBook());
            argsJson.put("condition",conditionJson);
            Pager page = reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
            if(page!=null){
                Integer pageCount = page.getPageCount();
                records.addAll(page.getRecordList());
                if(pageCount!=1){
                    for (int i=2;i<=pageCount;i++){
                        pageJson.put("pageIndex",i);
                        argsJson.put("page",pageJson);
                        page = reqVoucheList2(CtmJSONObject.toJSONString(argsJson));
                        if(page!=null){
                            records.addAll(page.getRecordList());
                        }
                    }
                }
            }
        }


        return records;

    }

    /**
     * 调用总账list2接口，获取凭证列表
     * @param filterArgs
     * @return
     */
    private Pager reqVoucheList2(String filterArgs){
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/list2";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, filterArgs, header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Integer code = result.getInteger("code");
        String message = result.getString("message");
        if(code==0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100207"),MessageUtils.getMessage("P_YS_FI_CM_0001252241") /* "查询总账失败，总账接口返回错误信息：" */+message);
        }
        Pager page = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(result.get("data")),Pager.class);
        if(page!=null) {
            return page;
        }
        return null;
    }
    //删除余额调节表
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = RuntimeException.class)
    public CtmJSONObject delete(Long id) throws Exception{

        BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, id, 3);
        if (balanceAdjustResult == null) {
            throw new Exception(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C9",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006ED", "数据不存在，请刷新页面后重试") /* "数据不存在，请刷新页面后重试" */) /* "数据不存在，请刷新页面后重试" */);
        }
        //删除主表
        MetaDaoHelper.delete(BalanceAdjustResult.ENTITY_NAME,id);

        //删除明细
        QuerySchema schema = new QuerySchema().addSelect("id");
        QueryConditionGroup conditionGroup = QueryConditionGroup.and(QueryCondition.name("balanceadjustresultid").eq(id.toString()));
        schema.addCondition(conditionGroup);
        List<BalanceadjustJournal> oldJournals = MetaDaoHelper.queryObject(BalanceadjustJournal.ENTITY_NAME, schema,null);
        List<BalanceadjustBankreconciliation> oldBankreconciliations = MetaDaoHelper.queryObject(BalanceadjustBankreconciliation.ENTITY_NAME, schema,null);
        if(oldJournals!=null&&oldJournals.size()>0){
            MetaDaoHelper.delete(BalanceadjustJournal.ENTITY_NAME,oldJournals);
        }
        if(oldBankreconciliations!=null&&oldBankreconciliations.size()>0){
            MetaDaoHelper.delete(BalanceadjustBankreconciliation.ENTITY_NAME,oldBankreconciliations);
        }

        String msg = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800C5",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006F4", "余额调节表删除成功") /* "余额调节表删除成功" */) /* "余额调节表删除成功" */;
        CtmJSONObject result = new CtmJSONObject();
        result.put("msg", msg);
        return result;
    }

    /**
     * 审核
     * @param balanceAdjustResultes
     * @return
     */
    @Override
    public CtmJSONObject balanceAudit(List<BalanceAdjustResult> balanceAdjustResultes) throws Exception {
        int size = balanceAdjustResultes.size();
        BalanceAdjustResultVO balanceAdjustResultVO = new BalanceAdjustResultVO();
        //时序审核
        checkSequenceAudit(balanceAdjustResultes,balanceAdjustResultVO);
        for (BizObject bizObject : balanceAdjustResultes){
            BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizObject.getId(), 1);
            if (null == balanceAdjustResult) {
                if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                    balanceAdjustResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC267A04380007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC267A04380007", "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！") /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！" */) /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！" */,
                            bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), bizObject.getDate(ICmpConstant.DZ_DATE)));
                    balanceAdjustResultVO.addFailCount();
                }
                continue;
            }
            try {
                if (null != balanceAdjustResult.getAuditstatus() && balanceAdjustResult.getAuditstatus() == AuditStatus.Incomplete.getValue()) {
                    if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                        balanceAdjustResult.setAuditstatus(AuditStatus.Complete.getValue());
                        balanceAdjustResult.setEntityStatus(EntityStatus.Update);
                        // 设置审核人、审核时间
                        balanceAdjustResult.setOperator(AppContext.getCurrentUser().getId().toString());
                        balanceAdjustResult.setOperatorName(AppContext.getCurrentUser().getName());
                        balanceAdjustResult.setOperatorTime(new Date());
                        MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResult);
                    }
                } else {
                    if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                        balanceAdjustResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                        balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002F", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002F", "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为已审批状态，不允许重复审批") /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为已审批状态，不允许重复审批" */) /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为已审批状态，不允许重复审批" */,
                                bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), balanceAdjustResult.getDzdate()));
                        balanceAdjustResultVO.addFailCount();
                    }
                }
            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================balanceAudit circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 600 ? message.substring(600) : message) : null;
                if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                    balanceAdjustResultVO.getFailed().put(balanceAdjustResult.getId().toString(), balanceAdjustResult.getId().toString());
                    balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1877BB0805600024", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1877BB0805600024", "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，审批失败，【%s】") /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，审批失败，【%s】" */) /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，审批失败，【%s】" */,
                            bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), balanceAdjustResult.getDzdate(), resultMsg));
                    balanceAdjustResultVO.addFailCount();
                }
            }
        }
        //用于记录批量审批失败条数
        int failSize = CollectionUtils.isEmpty(balanceAdjustResultVO.getMessages()) ? 0 : balanceAdjustResultVO.getMessages().size();
        String message = null;
        if (size == 1) {
            if (failSize == 0) {
                message = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002B", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002B",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002B", "余额调节表审批成功") /* "余额调节表审批成功" */) /* "余额调节表审批成功" */) /* "余额调节表审批成功" */;
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100208"),balanceAdjustResultVO.getMessages().get(0));
            }
        } else {
            message = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002D", InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002D",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002D", "共：%s张单据；%s张审批成功；%s张审批失败！") /* "共：%s张单据；%s张审批成功；%s张审批失败！" */) /* "共：%s张单据；%s张审批成功；%s张审批失败！" */) /* "共：%s张单据；%s张审批成功；%s张审批失败！" */ , size, size - failSize, failSize);
        }
        balanceAdjustResultVO.setMessage(message);
        balanceAdjustResultVO.setCount(size);
        balanceAdjustResultVO.setSucessCount(size - balanceAdjustResultVO.getFailCount());
        return balanceAdjustResultVO.getResult();
    }

    /**
     * 弃审
     * @param balanceAdjustResultes
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONObject balanceUnAudit(List<BalanceAdjustResult> balanceAdjustResultes) throws Exception {
        int size = balanceAdjustResultes.size();
        BalanceAdjustResultVO balanceAdjustResultVO = new BalanceAdjustResultVO();
        //时序弃审
        checkSequenceUnaudit(balanceAdjustResultes,balanceAdjustResultVO);
        for (BizObject bizObject : balanceAdjustResultes){
            BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, bizObject.getId(), 1);
            if (null == balanceAdjustResult) {
                if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                    balanceAdjustResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                    balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC267A04380007", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18AC267A04380007", "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！") /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！" */) /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，单据已删除，请刷新后重试！" */,
                            bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), bizObject.getDate(ICmpConstant.DZ_DATE)));
                    balanceAdjustResultVO.addFailCount();
                }
                continue;
            }

            try {
                if (null != balanceAdjustResult.getAuditstatus() && balanceAdjustResult.getAuditstatus() == AuditStatus.Complete.getValue()) {
                    if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                        balanceAdjustResult.setAuditstatus(AuditStatus.Incomplete.getValue());
                        balanceAdjustResult.setEntityStatus(EntityStatus.Update);
                        // 设置审核人、审核时间为空
                        balanceAdjustResult.setOperator(null);
                        balanceAdjustResult.setOperatorName(null);
                        balanceAdjustResult.setOperatorTime(null);
                        MetaDaoHelper.update(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResult);
                    }
                } else {
                    if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                        balanceAdjustResultVO.getFailed().put(bizObject.getId().toString(), bizObject.getId().toString());
                        balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002E", "银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为未审批状态，不允许弃审！") /* "银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为未审批状态，不允许弃审！" */) /* "银行账号【%s】，币种为【%s】，对账截止日期【%s】余额调节表为未审批状态，不允许弃审！" */,
                                bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), balanceAdjustResult.getDzdate()));
                        balanceAdjustResultVO.addFailCount();
                    }
                }
            } catch (Exception e) {
                String message = e.getMessage();
                log.error("================balanceAudit circulate :" + message, e);
                String resultMsg = ValueUtils.isNotEmptyObj(message) ? (message.length() > 600 ? message.substring(600) : message) : null;
                if (!balanceAdjustResultVO.getFailed().containsKey(bizObject.getId().toString())){
                    balanceAdjustResultVO.getFailed().put(balanceAdjustResult.getId().toString(), balanceAdjustResult.getId().toString());
                    balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1877BB0805600021", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1877BB0805600021", "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，弃审失败，【%s】") /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，弃审失败，【%s】" */) /* "余额调节表银行账号【%s】，币种为【%s】，对账截止日期【%s】，弃审失败，【%s】" */,
                            bizObject.get(ICmpConstant.BANKACCOUNT_ACCOUNT), bizObject.get(ICmpConstant.CURRENCY_NAME), balanceAdjustResult.getDzdate(), resultMsg));
                    balanceAdjustResultVO.addFailCount();
                }
            }
        }
        //用于记录批量弃审失败条数
        int failSize = CollectionUtils.isEmpty(balanceAdjustResultVO.getMessages()) ? 0 : balanceAdjustResultVO.getMessages().size();
        String message = null;
        if (size == 1) {
            if (failSize == 0) {
                message = InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002A", "余额调节表弃审成功") /* "余额调节表弃审成功" */) /* "余额调节表弃审成功" */;
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100209"),balanceAdjustResultVO.getMessages().get(0));
            }
        } else {
            message = String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187A58BA0560002C", "共：%s张单据；%s张弃审成功；%s张弃审失败！") /* "共：%s张单据；%s张弃审成功；%s张弃审失败！" */) /* "共：%s张单据；%s张弃审成功；%s张弃审失败！" */ , size, size - failSize, failSize);
        }
        balanceAdjustResultVO.setMessage(message);
        balanceAdjustResultVO.setCount(size);
        balanceAdjustResultVO.setSucessCount(size - balanceAdjustResultVO.getFailCount());
        return balanceAdjustResultVO.getResult();
    }

    /**
     * 校验时序审核流程
     * @param balanceAdjustResultes
     * @param balanceAdjustResultVO
     */
    private void checkSequenceAudit(List<BalanceAdjustResult> balanceAdjustResultes,BalanceAdjustResultVO balanceAdjustResultVO) throws Exception{
        //只有一条只用判断当前数据之前是否有未审核的数据
        if (balanceAdjustResultes.size() == 1){
            BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResultes.get(0).getId(), 1);
            if (balanceAdjustResult.getAuditstatus() != AuditStatus.Incomplete.getValue()){
                return;
            }
            BalanceAdjustResult unaudit = getEarlyUnauditData(balanceAdjustResult);
            if (unaudit != null){
                balanceAdjustResultVO.getFailed().put(balanceAdjustResult.getId().toString(), balanceAdjustResult.getId().toString());
                balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */,
                        balanceAdjustResultes.get(0).get(ICmpConstant.BANKACCOUNT_ACCOUNT), balanceAdjustResultes.get(0).get(ICmpConstant.CURRENCY_NAME), DateUtils.parseDateToStr(unaudit.getDzdate(),"yyyy-MM-dd")));
                balanceAdjustResultVO.addFailCount();
                return;
            }
        }

        //多条数据审批，先检查中间是否存在未勾选的数据；再去判断时序
        if (balanceAdjustResultes.size() > 1){
            Map<String,List<BalanceAdjustResult>> allData = new HashMap<>();
            //只筛选未审批数据。根据会计主体，对账方案，银行账户，币种分组
            for (BalanceAdjustResult result : balanceAdjustResultes){
                String key = result.getAccentity() + result.getBankreconciliationscheme() + result.getBankaccount() + result.getCurrency();
                BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, result.getId(), 1);
                if (balanceAdjustResult.getAuditstatus() == AuditStatus.Incomplete.getValue()){
                    if (allData.containsKey(key)){
                        allData.get(key).add(balanceAdjustResult);
                    }else {
                        List<BalanceAdjustResult> results = new ArrayList<>();
                        results.add(balanceAdjustResult);
                        allData.put(key,results);
                    }
                }
            }

            //按分组校验时序审核
            for (Map.Entry<String,List<BalanceAdjustResult>> entry : allData.entrySet()) {
                //根据对账截止日期排序
                List<BalanceAdjustResult> resultList = entry.getValue().stream().sorted(Comparator.comparing(BalanceAdjustResult::getDzdate)).collect(Collectors.toList());
                List<Date> dzdateList = new ArrayList<>();
                for (BalanceAdjustResult result : resultList){
                    dzdateList.add(result.getDzdate());
                }
                //前端传递过来的数据
                BalanceAdjustResult webdata = new BalanceAdjustResult();
                for(BalanceAdjustResult webReulst:balanceAdjustResultes){
                    if(webReulst.getId().toString().equals(resultList.get(0).getId().toString())){
                        webdata = webReulst;
                    }
                }
                //校验是否存在未勾选的数据
                BalanceAdjustResult unselect = getBetweenUnselectData(resultList.get(0),resultList.get(resultList.size() -1),dzdateList,AuditStatus.Incomplete.getValue(),"asc");
                if (unselect != null){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100210"),String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080014",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080014", "存在账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表未勾选，请勾选") /* "存在账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表未勾选，请勾选" */),
                            webdata.get(ICmpConstant.BANKACCOUNT_ACCOUNT), webdata.get(ICmpConstant.CURRENCY_NAME), unselect.getDzdate()));
                }

                //校验最早的数据之前是否存在未审批数据
                BalanceAdjustResult unaudit = getEarlyUnauditData(resultList.get(0));
                if(unaudit != null){
                    for (BalanceAdjustResult result : resultList){
                        balanceAdjustResultVO.getFailed().put(result.getId().toString(), result.getId().toString());
                        balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080013", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表尚未审批，请按照从早到晚的顺序进行审批！" */,
                                webdata.get(ICmpConstant.BANKACCOUNT_ACCOUNT), webdata.get(ICmpConstant.CURRENCY_NAME), DateUtils.parseDateToStr(unaudit.getDzdate(),"yyyy-MM-dd")));
                        balanceAdjustResultVO.addFailCount();
                    }
                }
            }
        }

    }

    /**
     * 时序弃审流程
     * @param balanceAdjustResultes
     * @param balanceAdjustResultVO
     * @throws Exception
     */
    private void checkSequenceUnaudit(List<BalanceAdjustResult> balanceAdjustResultes,BalanceAdjustResultVO balanceAdjustResultVO) throws Exception{
        //取消审批时校验同一方案、同一组织（对账组织）、银行账号、币种，该余额调节表截止日期之后是否存在“审批”的数据
        if (balanceAdjustResultes.size() == 1){
            BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, balanceAdjustResultes.get(0).getId(), 1);
            if (balanceAdjustResult.getAuditstatus() != AuditStatus.Complete.getValue()){
                return;
            }
            BalanceAdjustResult audit = getAfterAuditData(balanceAdjustResult);
            if (audit != null){
                balanceAdjustResultVO.getFailed().put(balanceAdjustResult.getId().toString(), balanceAdjustResult.getId().toString());
                balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */,
                        balanceAdjustResultes.get(0).get(ICmpConstant.BANKACCOUNT_ACCOUNT), balanceAdjustResultes.get(0).get(ICmpConstant.CURRENCY_NAME), audit.getDzdate()));
                balanceAdjustResultVO.addFailCount();
                return;
            }
        }

        //多条数据弃审，先检查中间是否存在未勾选的数据；再去判断时序
        if (balanceAdjustResultes.size() > 1){
            Map<String,List<BalanceAdjustResult>> allData = new HashMap<>();
            //只筛选未审批数据。根据会计主体，对账方案，银行账户，币种分组
            for (BalanceAdjustResult result : balanceAdjustResultes){
                String key = result.getAccentity() + result.getBankreconciliationscheme() + result.getBankaccount() + result.getCurrency();
                BalanceAdjustResult balanceAdjustResult = MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME, result.getId(), 1);
                if (balanceAdjustResult.getAuditstatus() == AuditStatus.Complete.getValue()){
                    if (allData.containsKey(key)){
                        allData.get(key).add(balanceAdjustResult);
                    }else {
                        List<BalanceAdjustResult> results = new ArrayList<>();
                        results.add(balanceAdjustResult);
                        allData.put(key,results);
                    }
                }
            }

            //按分组校验时序审核
            for (Map.Entry<String,List<BalanceAdjustResult>> entry : allData.entrySet()) {
                //根据对账截止日期排序
                List<BalanceAdjustResult> resultList = entry.getValue().stream().sorted(Comparator.comparing(BalanceAdjustResult::getDzdate)).collect(Collectors.toList());
                List<Date> dzdateList = new ArrayList<>();
                for (BalanceAdjustResult result : resultList){
                    dzdateList.add(result.getDzdate());
                }
                //前端传递过来的数据
                BalanceAdjustResult webdata = new BalanceAdjustResult();
                for(BalanceAdjustResult webReulst:balanceAdjustResultes){
                    if(webReulst.getId().toString().equals(resultList.get(0).getId().toString())){
                        webdata = webReulst;
                    }
                }
                //校验是否存在未勾选的数据
                BalanceAdjustResult unselect = getBetweenUnselectData(resultList.get(0),resultList.get(resultList.size() -1),dzdateList,AuditStatus.Complete.getValue(),"desc");
                if (unselect != null){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100210"),String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080014",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080014", "存在账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表未勾选，请勾选") /* "存在账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表未勾选，请勾选" */),
                            webdata.get(ICmpConstant.BANKACCOUNT_ACCOUNT), webdata.get(ICmpConstant.CURRENCY_NAME), unselect.getDzdate()));
                }

                //取消审批时校验同一方案、同一组织（对账组织）、银行账号、币种，该余额调节表截止日期之后是否存在“审批”的数据
                BalanceAdjustResult audit = getAfterAuditData(resultList.get(resultList.size() -1));
                if(audit != null){
                    for (BalanceAdjustResult result : resultList){
                        balanceAdjustResultVO.getFailed().put(result.getId().toString(), result.getId().toString());
                        balanceAdjustResultVO.getMessages().add(String.format(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C027D0405080012", "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！") /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */) /* "账号【%s】、币种【%s】存在截止日期为【%s】的余额调节表已审批，请按照从晚到早的顺序取消审批！" */,
                                webdata.get(ICmpConstant.BANKACCOUNT_ACCOUNT), webdata.get(ICmpConstant.CURRENCY_NAME), audit.getDzdate()));
                        balanceAdjustResultVO.addFailCount();
                    }
                }
            }
        }

    }

    /**
     * 查询早于当前截止时间最早的未审批数据
     * @param balanceAdjustResult
     * @return
     */
    @Override
    public BalanceAdjustResult getEarlyUnauditData(BalanceAdjustResult balanceAdjustResult) throws Exception {
        BalanceAdjustResult result = new BalanceAdjustResult();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("accentity").eq(balanceAdjustResult.getAccentity()),
                QueryCondition.name("bankreconciliationscheme").eq(balanceAdjustResult.getBankreconciliationscheme()),
                QueryCondition.name("bankaccount").eq(balanceAdjustResult.getBankaccount()),
                QueryCondition.name("currency").eq(balanceAdjustResult.getCurrency()),
                QueryCondition.name("auditstatus").not_eq(AuditStatus.Complete.getValue()), //未审批过的
                QueryCondition.name("dzdate").lt(balanceAdjustResult.getDzdate())
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate","asc"));
        Map<String,Object> map = MetaDaoHelper.queryOne(BalanceAdjustResult.ENTITY_NAME,querySchema);
        if (map == null){
            return null;
        }
        result.init(map);
        return result;
    }

    /**
     * 获取当前余额调节表之后的已审核数据
     * @param balanceAdjustResult
     * @return
     * @throws Exception
     */
    @Override
    public BalanceAdjustResult  getAfterAuditData(BalanceAdjustResult balanceAdjustResult) throws Exception {
        BalanceAdjustResult result = new BalanceAdjustResult();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("accentity").eq(balanceAdjustResult.getAccentity()),
                QueryCondition.name("bankreconciliationscheme").eq(balanceAdjustResult.getBankreconciliationscheme()),
                QueryCondition.name("bankaccount").eq(balanceAdjustResult.getBankaccount()),
                QueryCondition.name("currency").eq(balanceAdjustResult.getCurrency()),
                QueryCondition.name("auditstatus").not_eq(AuditStatus.Incomplete.getValue()), //已审批的，审批中的
                QueryCondition.name("dzdate").gt(balanceAdjustResult.getDzdate())
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate","desc"));
        Map<String,Object> map = MetaDaoHelper.queryOne(BalanceAdjustResult.ENTITY_NAME,querySchema);
        if (map == null){
            return null;
        }
        result.init(map);
        return result;
    }

    @Override
    public BalanceAdjustResult getBalanceAdjustResultById(Long id) throws Exception {
        return MetaDaoHelper.findById(BalanceAdjustResult.ENTITY_NAME,id);
    }

    /**
     * 获取中间已审批/未审批的数据
     * @param first 最早数据
     * @param last 最晚数据
     * @param dzdateList 日期集合
     * @param auditStatus 校验的数据审批状态
     * @param orderStr 排序规则
     * @return
     * @throws Exception
     */
    private BalanceAdjustResult getBetweenUnselectData(BalanceAdjustResult first,BalanceAdjustResult last,List<Date> dzdateList,Short auditStatus,String orderStr) throws Exception {
        BalanceAdjustResult result = new BalanceAdjustResult();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("accentity").eq(first.getAccentity()),
                QueryCondition.name("bankreconciliationscheme").eq(first.getBankreconciliationscheme()),
                QueryCondition.name("bankaccount").eq(first.getBankaccount()),
                QueryCondition.name("currency").eq(first.getCurrency()),
                QueryCondition.name("auditstatus").eq(auditStatus),
                QueryCondition.name("dzdate").egt(first.getDzdate()),
                QueryCondition.name("dzdate").elt(last.getDzdate()),
                QueryCondition.name("dzdate").not_in(dzdateList)
        );
        querySchema.addCondition(group);
        querySchema.addOrderBy(new QueryOrderby("dzdate",orderStr));
        //校验是否存在未勾选的数据
        Map<String,Object> map = MetaDaoHelper.queryOne(BalanceAdjustResult.ENTITY_NAME,querySchema);
        if (map == null){
            return null;
        }
        result.init(map);
        return result;

    }
}
