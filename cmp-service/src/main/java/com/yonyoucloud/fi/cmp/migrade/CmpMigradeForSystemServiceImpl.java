package com.yonyoucloud.fi.cmp.migrade;

import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.tenant.sdk.TenantCenter;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.util.HttpTookitYts;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.file.CooperationFileUtilService;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.vo.migrade.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CmpMigradeForSystemServiceImpl implements CmpMigradeForSystemService{

    private static final String RepairPlan_AuditAndUnSettle = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400460", "当前单据审核状态已审核，结算状态未结算，建议实施联系客户执行线下支付或者网银支付处理。") /* "当前单据审核状态已审核，结算状态未结算，建议实施联系客户执行线下支付或者网银支付处理。" */;
    private static final String RepairPlan_AuditAndSettleIng = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400461", "当前单据在结算状态为结算中，需要实施联系客户执行线下支付或者网银支付处理。") /* "当前单据在结算状态为结算中，需要实施联系客户执行线下支付或者网银支付处理。" */;
    private static final String RepairPlan_VoucherStatusReceived = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400463", "当前单据为往期已结算且凭证状态为已发送待生成，需要实施人员检查“重新生成凭证”节点数据，处理异常数据。") /* "当前单据为往期已结算且凭证状态为已发送待生成，需要实施人员检查“重新生成凭证”节点数据，处理异常数据。" */;
    private static final String RepairPlan_SourceFromDrftBill = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400464", "当前单据为商业汇票推式生成单据，需要实施处理为已结算，或者上游商业汇票撤回单据。") /* "当前单据为商业汇票推式生成单据，需要实施处理为已结算，或者上游商业汇票撤回单据。" */;
    private static final String RepairPlan_SourceFromArapBill = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400466", "当前单据来源于应收应付未审核完成单据，需要应收应付升迁处理推送资金结算，本次升迁删除日记账数据。") /* "当前单据来源于应收应付未审核完成单据，需要应收应付升迁处理推送资金结算，本次升迁删除日记账数据。" */;
    private static final String RepairPlan_SourceFromPayapplicationBillApproveEdit = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400467", "付款申请单存在审批过程中的单据，需要审批完成后才能升级，或者撤回审批返回成开立态再升级。") /* "付款申请单存在审批过程中的单据，需要审批完成后才能升级，或者撤回审批返回成开立态再升级。" */;
    private static final String RepairPlan_IsWfControlledApproveEdit = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400468", "当前单据审批状态为审批过程中，需要审批完成后才能升级。") /* "当前单据审批状态为审批过程中，需要审批完成后才能升级。" */;
    private static final String RepairPlan_FundPayBillIsExist = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400469", "本升级租户当前已存在“资金付款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！") /* "本升级租户当前已存在“资金付款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！" */;
    private static final String RepairPlan_FundCollBillIsExist = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046A", "本升级租户当前已存在“资金收款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！") /* "本升级租户当前已存在“资金收款单”数据，在修改现金生成凭证时机的参数时，请谨慎处理！" */;

    private static final String INVOKEID = "invokeId";//升级请求的调用id，id 相同表示同一次请求的反复调用：人工重试
    private static final String TENANTID = "tenantId";//升级的租户id
    private static final String UPGRADETYPE = "upgradeType";//升级的类型：0-期初升级、1-规格升级
    private static final String SOURCEDATACENTER = "sourceDataCenter";//原数据中心：c2.yonyoucloud.com
    private static final String TARGETDATACENTER = "targetDataCenter";//目标数据中心：c4.yonyoucloud.com
    private static final String PRINCIPAL = "principal";//发起人（即计划负责人）
    private static final String PRINCIPALEMAIL = "principalEmail";//发起人邮箱（即计划负责人）
    private static final String UPGRADETENANTAUDITUSERID = "upgradeTenantAuditUserId";//升级租户的审计用户友互通 id
    private static final String CALLBACKURI = "callBackUri";//回调检查服务，返回检查结果 xxxx/upgradeTenant/callBack/preCheck?callbackTenantId=xxxx

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;
    @Autowired
    private CmpNewFiPreCheckService cmpNewFiPreCheckService;
    @Autowired
    private CooperationFileUtilService cooperationFileUtilService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;// 生成oid
    @Autowired
    private CmpNewFiMigradeService cmpNewFiMigradeService;
    @Autowired
    private CmpNewFiMigradeUtilService cmpNewFiMigradeUtilService;

    @Override
    public CtmJSONObject cmpMigradeForSystemCheck(CmpMigradeForSystemRequest params) throws Exception {
        //同步返回数据
        CtmJSONObject result = new CtmJSONObject();
        //参数解析
        //本次预检动作所属的调用id，id 相同表示同一次请求的反复调用：人工重试
        String invokeId = params.getInvokeId();
        //升级的租户id
        String tenantId = params.getTenantId();
        //加锁处理
        YmsLock ymsLock = JedisLockUtils.lockWithOutTrace(invokeId+tenantId+ICmpConstant.YONBIP_FI_CTMCMP);
        if(null == ymsLock){
            result.put(ICmpConstant.CODE, 100);
            result.put(ICmpConstant.MSG,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046B", "现金预检进行中请勿重复返送信息") /* "现金预检进行中请勿重复返送信息" */);
            return result;
        }
        //升级的类型：0-期初升级、1-规格升级
        int upgradeType = params.getUpgradeType();
        //回调地址回调检查服务，返回检查结果
        String callBackUri = params.getCallBackUri();
        //异步查询数据
        RobotExecutors.runAs(tenantId, new Callable() {
            @Override
            public Object call() throws Exception {
                try {
                    String tenantResult = TenantCenter.getTenantById (tenantId);
                    String tenantName = ((LinkedHashMap)CtmJSONObject.parseObject(tenantResult).get("tenant")).get("tenantName").toString();
                    Map<String, String> tenantMap = new HashMap<>();
                    tenantMap.put(tenantId, tenantName);
                    CmpPreCheckReqVO preCheckReqVO = new CmpPreCheckReqVO();
                    preCheckReqVO.setTenantMap(tenantMap);
                    List<CmpMigraForSystemCheckResult> checkResultList = new ArrayList<>();
                    //依次调用每一个检查项
                    buildCheckResultAuditAndUnSettleData(preCheckReqVO,checkResultList);
                    buildVoucherStatusReceivedData(preCheckReqVO,checkResultList);
                    buildAuditAndSettleIngData(preCheckReqVO,checkResultList);
                    buildSourceFromDrftBill(preCheckReqVO,checkResultList);
                    buildSourceFromArapBill(preCheckReqVO,checkResultList);
                    buildPayApplicationBill(preCheckReqVO,checkResultList);
                    buildIsWfAndApprovalData(preCheckReqVO,checkResultList);
                    buildFundPayBillIsExist(preCheckReqVO,checkResultList);
                    buildFundCollBillIsExist(preCheckReqVO,checkResultList);

                    //构建总返回结果
                    CmpMigradeForSystemRequest bodyResult = new CmpMigradeForSystemRequest();
                    bodyResult.setInvokeId(invokeId);
                    bodyResult.setTenantId(tenantId);
                    bodyResult.setCheckResult(checkResultList);
                    //发送回调结果
                    callBackForSystem(bodyResult,callBackUri);
                } catch (Exception e) {
                    log.error("查询预检数据失败：",e);
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102141"),e.getMessage());
                }finally {
                    JedisLockUtils.unlockWithOutTrace(ymsLock);
                }
                return null;
            }
        }, executorServicePool.getThreadPoolExecutor());
        result.put(ICmpConstant.CODE, 100);
        result.put(ICmpConstant.MSG,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400455", "现金预检进行中，请后续检查回调信息") /* "现金预检进行中，请后续检查回调信息" */);
        return result;
    }


    public CmpMigraForSystemCheckResult buildCheckResultAuditAndUnSettleData(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getAuditAndUnSettleData(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckTypeName());
        checkData.setStrictValidation(false);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.AuditAndUnSettle.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_AuditAndUnSettle);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("check");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildVoucherStatusReceivedData(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getVoucherStatusReceivedData(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.VoucherStatusReceived.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.VoucherStatusReceived.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_VoucherStatusReceived);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildAuditAndSettleIngData(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getAuditAndSettleIngData(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.AuditAndSettleIng.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_AuditAndSettleIng);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildSourceFromDrftBill(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getSourceFromDrftBill(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.SourceFromDrftBill.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.SourceFromDrftBill.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_SourceFromDrftBill);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildSourceFromArapBill(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getSourceFromArapBill(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.SourceFromArapBill.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.SourceFromArapBill.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_SourceFromArapBill);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
//         reportUrl
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildPayApplicationBill(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getPayApplicationBill(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.PayapplicationBillApproveEdit.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.PayapplicationBillApproveEdit.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_SourceFromPayapplicationBillApproveEdit);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildIsWfAndApprovalData(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getIsWfAndApprovalData(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.IsWfControlledApproveEdit.getCheckTypeName());
        checkData.setStrictValidation(true);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.IsWfControlledApproveEdit.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_IsWfControlledApproveEdit);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("fail");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 50));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildFundPayBillIsExist(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getFundPayBillIsExist(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.FundPayBillIsExist.getCheckTypeName());
        checkData.setStrictValidation(false);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.FundPayBillIsExist.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_FundPayBillIsExist);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("check");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 10));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    public CmpMigraForSystemCheckResult buildFundCollBillIsExist(CmpPreCheckReqVO preCheckReqVO,List<CmpMigraForSystemCheckResult> checkResultList) throws Exception {
        //按顺序调用各个检查项 并构建结果
        List<CmpPreCheckDetailVO> cmpPreCheckDetailVO = cmpNewFiPreCheckService.getFundCollBillIsExist(preCheckReqVO);
        //构建单次返回结果
        CmpMigraForSystemCheckResult checkData = new CmpMigraForSystemCheckResult();
        checkData.setDomain(ICmpConstant.YONBIP_FI_CTMCMP);
        checkData.setDomainName(EventSource.Cmpchase.getName());
        checkData.setCheckItem(MigradeCheckTypeEnum.FundCollBillIsExist.getCheckTypeName());
        checkData.setStrictValidation(false);
        checkData.setStatus("pass");
        checkData.setMessage(MigradeCheckTypeEnum.FundCollBillIsExist.getCheckTypeName());
        checkData.setSuggestion(RepairPlan_FundCollBillIsExist);
        checkData.setErrorCount(cmpPreCheckDetailVO.size());
        if(CollectionUtils.isNotEmpty(cmpPreCheckDetailVO)){
            checkData.setStatus("check");
//         reportUrl
            checkData.setReportUrl(bilidExcelUpload(cmpPreCheckDetailVO));
            //最多只返回50条数据
            List<CmpPreCheckDetailVO> dataList = cmpPreCheckDetailVO.subList(0, Math.min(cmpPreCheckDetailVO.size(), 10));
            checkData.setData(dataList);
        }else{
            checkData.setData(new ArrayList<>());
        }
        checkResultList.add(checkData);
        return checkData;
    }

    /**
     * 数据生成csv格式 并上传协同云服务器 返回文件id
     * @param cmpPreCheckDetailVO
     * @return
     */
    private String bilidExcelUpload(List<CmpPreCheckDetailVO> cmpPreCheckDetailVO) throws Exception {
        String fileId = "";
        StringBuffer stringBuffer = new StringBuffer();
        ByteArrayInputStream inputStream = null;
        CSVPrinter printer = new CSVPrinter(stringBuffer, CSVFormat.DEFAULT.withHeader(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045C", "单据编码") /* "单据编码" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045F", "单据类型") /* "单据类型" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400457", "单据日期") /* "单据日期" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400458", "会计主体") /* "会计主体" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400459", "来源应用") /* "来源应用" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045E", "来源单据号") /* "来源单据号" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045B", "审核状态") /* "审核状态" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045D", "账簿ID") /* "账簿ID" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540045A", "账簿") /* "账簿" */));
        try {
            for(CmpPreCheckDetailVO vo : cmpPreCheckDetailVO){
                List<String> row = new ArrayList<>();
                row.add(vo.getBillCode());
                row.add(vo.getBillType());
                row.add(vo.getBillDate());
                row.add(vo.getAccEntityName());
                row.add(vo.getSrcAppName());
                row.add(vo.getSrcBillNo());
                row.add(vo.getAuditStatus());
                row.add(vo.getAccountId());
                row.add(vo.getAccountName());
                printer.printRecord(row);
            }
            printer.close();

            // 在数据写入完成后再创建输入流
            inputStream = new ByteArrayInputStream(stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
            String fileName ="error-data-CMP-"+ymsOidGenerator.nextId() + ".csv";
            fileId = cooperationFileUtilService.uploadInputStream(inputStream,fileName);
        }catch(Exception e){
            throw new CtmException(e.getMessage());
        }finally {
            printer.close();
            if(inputStream != null){
                inputStream.close();
            }
        }
        return fileId;
    }


    private void callBackForSystem(CmpMigradeForSystemRequest asynResult,String callBackUri){
        Map<String, String> requestHeader = new HashMap<>();
        String responseStr = HttpTookitYts.doPostWithJson(callBackUri, CtmJSONObject.toJSONString(asynResult), requestHeader);
        CtmJSONObject resultJson = CtmJSONObject.parseObject(responseStr);
        String code = resultJson.getString("code");
        if(!code.equals("200")){
            String message = resultJson.getString("message");
            log.error("callBackForSystem回调报错，发送平台失败："+resultJson.toString());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102142"),message!=null?message:com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B1", "回调方法报错，发送平台失败") /* "回调方法报错，发送平台失败" */);
        }
    }


    @Override
    public CtmJSONObject updateconfig(CtmJSONObject params) throws Exception {
        //同步返回数据
        CtmJSONObject result = new CtmJSONObject();
        //参数解析
        //本次预检动作所属的调用id，id 相同表示同一次请求的反复调用：人工重试
        String invokeId = params.getString(INVOKEID);
        //升级的租户id
        String tenantId = params.getString(TENANTID);
        //加锁处理
        YmsLock ymsLock = JedisLockUtils.lockWithOutTrace(invokeId+tenantId+"updateconfig"+ICmpConstant.YONBIP_FI_CTMCMP);
        if(null == ymsLock){
            result.put(ICmpConstant.CODE, 100);
            result.put(ICmpConstant.MSG,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400454", "现金配置数据升迁进行中,请勿重复返送信息") /* "现金配置数据升迁进行中,请勿重复返送信息" */);
            return result;
        }
        //回调地址回调检查服务，返回检查结果
        String callBackUri = params.getString(CALLBACKURI);
        //构建总返回结果
        CmpMigradeForSystemRequest bodyResult = new CmpMigradeForSystemRequest();
        //开启异步数据升迁
        RobotExecutors.runAs(tenantId, new Callable() {
            @Override
            public Object call() throws Exception {
                try {
                    //交易类型升迁
                    cmpNewFiMigradeService.migradeUpdateTradetypeExcute(tenantId);
                    //特征升级
                    cmpNewFiMigradeService.migradeUpdateCharacterDefExcute(null);

                    bodyResult.setInvokeId(invokeId);
                    bodyResult.setTenantId(tenantId);
                    bodyResult.setStatus("pass");
                } catch (Exception e) {
                    log.error("查询预检数据失败：",e);
                    bodyResult.setInvokeId(invokeId);
                    bodyResult.setTenantId(tenantId);
                    bodyResult.setStatus("fail");
                    bodyResult.setMessage(e.getMessage());
                    bodyResult.setDetail(e.getMessage());
                }finally {
                    //发送回调结果
                    callBackForSystem(bodyResult,callBackUri);
                    JedisLockUtils.unlockWithOutTrace(ymsLock);
                }
                return null;
            }
        }, executorServicePool.getThreadPoolExecutor());
        result.put(ICmpConstant.CODE, 100);
        result.put(ICmpConstant.MSG,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400456", "现金配置数据升迁进行中,请后续检查回调信息") /* "现金配置数据升迁进行中,请后续检查回调信息" */);
        return result;
    }

    @Override
    public CtmJSONObject update(CtmJSONObject params) throws Exception {
        //同步返回数据
        CtmJSONObject result = new CtmJSONObject();
        //参数解析
        //本次预检动作所属的调用id，id 相同表示同一次请求的反复调用：人工重试
        String invokeId = params.getString(INVOKEID);
        //升级的租户id
        String tenantId = params.getString(TENANTID);
        //回调地址回调检查服务，返回检查结果
        String callBackUri = params.getString(CALLBACKURI);
        //构建总返回结果
        CmpMigradeForSystemRequest bodyResult = new CmpMigradeForSystemRequest();
        ExecutorService taskExecutor = null;
        taskExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"scheduledUpgradeInitData-threadpool");
        try{
            taskExecutor.submit(() -> {
                RobotExecutors.runAs(tenantId, ()->{
                    try {
                        CtmLockTool.executeInOneServiceLock("UpgradeInitData", 5 * 60L, TimeUnit.SECONDS, (int lockstatus) -> { if (lockstatus == LockStatus.GETLOCK_FAIL) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102143"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB5405900037", "账户期初历史数据升级任务正在执行中，请勿重复执行") /* "账户期初历史数据升级任务正在执行中，请勿重复执行" */);
                        }});
                        //升级前校验 不符合校验项 报错停止
                        String checkMessage =  beforeUpdateCheck();
                        if(checkMessage!=null){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102143"),checkMessage );
                        }
                        //付款升级
                        cmpNewFiMigradeService.migradePayToFunPayMentExcuteForSystem();
                        //收款升级/
                        cmpNewFiMigradeService.migradeReToFundCollectionExcuteForSystem();
                        bodyResult.setInvokeId(invokeId);
                        bodyResult.setTenantId(tenantId);
                        bodyResult.setStatus("pass");
                    } catch (Exception e) {
                        log.error("财资服务数据升级失败：",e);
                        bodyResult.setInvokeId(invokeId);
                        bodyResult.setTenantId(tenantId);
                        bodyResult.setStatus("fail");
                        bodyResult.setMessage(e.getMessage());
                        bodyResult.setDetail(e.getMessage());
                    }finally {
                        callBackForSystem(bodyResult,callBackUri);
                    }
                });
            });
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }finally{
            if (taskExecutor!=null){
                taskExecutor.shutdown();
            }
        }
        buildUpdateResponse(result,null);
        return result;
    }



    private String beforeUpdateCheck() throws Exception{
        Map<String , Boolean> resultMap = new HashMap<>();
        cmpNewFiMigradeUtilService.checkBeforeUpgradeTransType(null,resultMap);
        if(!resultMap.get("CheckPayTransType")){
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400462", "付款工作台的交易类型，没有完全同步到资金付款单，不允许升级。") /* "付款工作台的交易类型，没有完全同步到资金付款单，不允许升级。" */;
        }else if(!resultMap.get("CheckRecTransType")){
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400465", "收款工作台的交易类型，没有完全同步到资金收款单，不允许升级。") /* "收款工作台的交易类型，没有完全同步到资金收款单，不允许升级。" */;
        }
        return null;
    }

    private void buildUpdateResponse(CtmJSONObject result,String failMessage){
        if(failMessage!=null){
            result.put(ICmpConstant.CODE, 100);
            result.put(ICmpConstant.MSG,failMessage);
        }else{
            result.put(ICmpConstant.CODE, 100);
            result.put(ICmpConstant.MSG,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540046C", "财资数据升级中，请等待回调信息") /* "财资数据升级中，请等待回调信息" */);
        }
    }

}
